package com.example.orbitai.core.model

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ResumableFileDownloaderTest {

    @get:Rule val tmpDir = TemporaryFolder()
    private val server = MockWebServer()
    private val content = ByteArray(300_000) { (it % 251).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
    private lateinit var dest: File
    private val tmp get() = File(dest.absolutePath + ".tmp")

    private fun downloader(token: String? = null) = ResumableFileDownloader(OkHttpClient()) { token }
    private fun body(bytes: ByteArray) = Buffer().write(bytes)

    @Before fun setUp() { server.start(); dest = File(tmpDir.root, "model.bin") }
    @After fun tearDown() { server.shutdown() }

    private fun download(sha256: String? = sha, size: Long? = content.size.toLong(), auth: Boolean = false, token: String? = null) =
        runBlocking { downloader(token).download(server.url("/f").toString(), dest, size, sha256, auth) }

    @Test fun freshDownloadVerifiesAndRenames() {
        server.enqueue(MockResponse().setBody(body(content)))
        assertEquals(FileDownloadResult.Completed, download())
        assertArrayEquals(content, dest.readBytes())
        assertFalse(tmp.exists())
        assertNull(server.takeRequest().getHeader("Range"))
    }

    @Test fun partialFileResumesWithRangeRequest() {
        tmp.writeBytes(content.copyOfRange(0, 100_000))
        server.enqueue(MockResponse().setResponseCode(206).setBody(body(content.copyOfRange(100_000, content.size))))
        assertEquals(FileDownloadResult.Completed, download())
        assertEquals("bytes=100000-", server.takeRequest().getHeader("Range"))
        assertArrayEquals(content, dest.readBytes())
    }

    @Test fun serverIgnoringRangeRestartsFromZero() {
        tmp.writeBytes(ByteArray(100_000) { 7 })
        server.enqueue(MockResponse().setResponseCode(200).setBody(body(content)))
        assertEquals(FileDownloadResult.Completed, download())
        assertArrayEquals(content, dest.readBytes())
    }

    @Test fun droppedConnectionKeepsPartialFileForResume() {
        server.enqueue(MockResponse().setBody(body(content)).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        val result = download()
        assertTrue(result is FileDownloadResult.Failed && result.message.contains("resume"))
        assertFalse(dest.exists())
        assertTrue(tmp.exists())
    }

    @Test fun checksumMismatchRejectsFile() {
        val corrupted = content.copyOf().also { it[10] = (it[10] + 1).toByte() }
        server.enqueue(MockResponse().setBody(body(corrupted)))
        val result = download()
        assertTrue(result is FileDownloadResult.Failed && result.message.contains("checksum"))
        assertFalse(dest.exists())
        assertFalse(tmp.exists())
    }

    @Test fun truncatedFileRejectedBySize() {
        server.enqueue(MockResponse().setBody(body(content.copyOfRange(0, 1000))))
        val result = download(sha256 = null)
        assertTrue(result is FileDownloadResult.Failed && result.message.contains("incomplete"))
        assertFalse(dest.exists())
    }

    @Test fun rangeNotSatisfiableDiscardsStalePartial() {
        tmp.writeBytes(content.copyOfRange(0, 10))
        server.enqueue(MockResponse().setResponseCode(416))
        assertTrue(download() is FileDownloadResult.Failed)
        assertFalse(tmp.exists())
    }

    @Test fun completePartialFileIsVerifiedWithoutNetwork() {
        tmp.writeBytes(content)
        assertEquals(FileDownloadResult.Completed, download())
        assertEquals(0, server.requestCount)
    }

    @Test fun explicitCancelDiscardsPartial() {
        server.enqueue(MockResponse().setBody(body(content)))
        val result = runBlocking {
            downloader().download(server.url("/f").toString(), dest, content.size.toLong(), sha, false, isCancelled = { true })
        }
        assertEquals(FileDownloadResult.Failed("Cancelled"), result)
        assertFalse(tmp.exists())
    }

    @Test fun authHeaderSentOnlyWhenRequired() {
        server.enqueue(MockResponse().setBody(body(content)))
        download(auth = true, token = "hf_test")
        assertEquals("Bearer hf_test", server.takeRequest().getHeader("Authorization"))
        dest.delete()
        server.enqueue(MockResponse().setBody(body(content)))
        download(auth = false, token = "hf_test")
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun httpErrorsMapToActionableMessages() {
        server.enqueue(MockResponse().setResponseCode(403))
        assertEquals(FileDownloadResult.Failed("Access denied. Accept the model license on HuggingFace first."), download())
    }
}
