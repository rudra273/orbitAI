package com.example.orbitai.core.model

data class ModelDownloadFile(
    val relativePath: String,
    val url: String,
    val sizeBytes: Long? = null,
)

data class ModelDownloadSpec(
    val files: List<ModelDownloadFile>,
    val requiresAuth: Boolean = false,
) {
    val totalSizeBytes: Long?
        get() = if (files.isNotEmpty() && files.all { it.sizeBytes != null }) {
            files.sumOf { it.sizeBytes!! }
        } else null
}

private fun hfResolveUrl(repo: String, path: String, revision: String): String {
    return "https://huggingface.co/$repo/resolve/$revision/$path?download=true"
}

private fun singleFileSpec(
    repo: String,
    remotePath: String,
    localPath: String,
    requiresAuth: Boolean,
    sizeBytes: Long,
    revision: String,
): ModelDownloadSpec {
    return ModelDownloadSpec(
        files = listOf(
            ModelDownloadFile(
                relativePath = localPath,
                url = hfResolveUrl(repo, remotePath, revision),
                sizeBytes = sizeBytes,
            )
        ),
        requiresAuth = requiresAuth,
    )
}

private fun folderSpec(
    repo: String,
    remoteRoot: String,
    files: List<Pair<String, Long>>,
    requiresAuth: Boolean = false,
    revision: String,
): ModelDownloadSpec {
    return ModelDownloadSpec(
        files = files.map { (relativePath, sizeBytes) ->
            val remotePath = if (remoteRoot.isBlank()) relativePath else "$remoteRoot/$relativePath"
            ModelDownloadFile(
                relativePath = relativePath,
                url = hfResolveUrl(repo, remotePath, revision),
                sizeBytes = sizeBytes,
            )
        },
        requiresAuth = requiresAuth,
    )
}

val MODEL_DOWNLOAD_SPECS = mapOf(
    "gemma3-1b-litertlm" to singleFileSpec(
        repo = "litert-community/Gemma3-1B-IT",
        remotePath = "gemma3-1b-it-int4.litertlm",
        localPath = "gemma3-1b-it-int4.litertlm",
        requiresAuth = true,
        sizeBytes = 584_417_280L,
        revision = "a6306a4e292016480083b73b8dc6f3f939ae04c3",
    ),
    "gemma3-1b" to singleFileSpec(
        repo = "litert-community/Gemma3-1B-IT",
        remotePath = "gemma3-1b-it-int4.task",
        localPath = "gemma3-1b-it-int4.task",
        requiresAuth = true,
        sizeBytes = 554_661_243L,
        revision = "a6306a4e292016480083b73b8dc6f3f939ae04c3",
    ),
    "gemma3-4b" to singleFileSpec(
        repo = "google/gemma-3n-E4B-it-litert-lm",
        remotePath = "gemma-3n-E4B-it-int4.litertlm",
        localPath = "gemma-3n-E4B-it-int4.litertlm",
        requiresAuth = true,
        sizeBytes = 4_919_541_760L,
        revision = "297ed75955702dec3503e00c2c2ecbbf475300bc",
    ),
    "gemma3-2b" to singleFileSpec(
        repo = "google/gemma-3n-E2B-it-litert-lm",
        remotePath = "gemma-3n-E2B-it-int4.litertlm",
        localPath = "gemma-3n-E2B-it-int4.litertlm",
        requiresAuth = true,
        sizeBytes = 3_655_827_456L,
        revision = "c03b6f60b8da6c5400b6838a2cf26420f80c0a01",
    ),
    "gemma4-e2b" to singleFileSpec(
        repo = "litert-community/gemma-4-E2B-it-litert-lm",
        remotePath = "gemma-4-E2B-it.litertlm",
        localPath = "gemma-4-E2B-it-int4.litertlm",
        requiresAuth = false,
        sizeBytes = 2_588_147_712L,
        revision = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1",
    ),
    "gemma4-e4b" to singleFileSpec(
        repo = "litert-community/gemma-4-E4B-it-litert-lm",
        remotePath = "gemma-4-E4B-it.litertlm",
        localPath = "gemma-4-E4B-it-int4.litertlm",
        requiresAuth = false,
        sizeBytes = 3_659_530_240L,
        revision = "2eee7ac325f20eb8c9ac1d0e972f7c84663062da",
    ),
    "gemma2-2b" to singleFileSpec(
        repo = "litert-community/Gemma2-2B-IT",
        remotePath = "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
        localPath = "gemma2-2b-it-cpu-int8.task",
        requiresAuth = true,
        sizeBytes = 2_713_274_466L,
        revision = "0f2722125e4cd85da620e6a0ebcb26b87a230049",
    ),
    "onnx-gemma3-4b-it" to folderSpec(
        repo = "onnxruntime/Gemma-3-ONNX",
        remoteRoot = "gemma-3-4b-it/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
        files = listOf(
            "chat_template.jinja" to 1_532L,
            "gemma-3-embedding.onnx" to 142_373L,
            "gemma-3-embedding.onnx.data" to 2_685_009_920L,
            "gemma-3-text.onnx" to 433_790L,
            "gemma-3-text.onnx.data" to 2_694_922_240L,
            "gemma-3-vision.onnx" to 397_532L,
            "gemma-3-vision.onnx.data" to 645_089_984L,
            "genai_config.json" to 2_077L,
            "processor_config.json" to 1_045L,
            "special_tokens_map.json" to 662L,
            "tokenizer.json" to 33_384_568L,
            "tokenizer_config.json" to 1_155_387L,
        ),
        revision = "57c665cff71315f7c63d346549cfeea69d90048e",
    ),
    "onnx-gemma3-270m-it" to folderSpec(
        repo = "smartvest-llc/gemma-3-270m-it-genai-int4-android",
        remoteRoot = "",
        files = listOf(
            "added_tokens.json" to 35L,
            "chat_template.jinja" to 1_532L,
            "genai_config.json" to 1_506L,
            "model.onnx" to 229_108L,
            "model.onnx.data" to 905_969_664L,
            "special_tokens_map.json" to 662L,
            "tokenizer.json" to 33_384_568L,
            "tokenizer.model" to 4_689_074L,
            "tokenizer_config.json" to 1_155_373L,
        ),
        revision = "7e9779dea004bc5d5767ddd419d02d4cabcae1a5",
    ),
    "onnx-phi3-mini-4k" to folderSpec(
        repo = "microsoft/Phi-3-mini-4k-instruct-onnx",
        remoteRoot = "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
        files = listOf(
            "added_tokens.json" to 306L,
            "config.json" to 919L,
            "configuration_phi3.py" to 10_411L,
            "genai_config.json" to 1_576L,
            "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx" to 231_335L,
            "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data" to 2_722_861_056L,
            "special_tokens_map.json" to 599L,
            "tokenizer.json" to 1_937_869L,
            "tokenizer.model" to 499_723L,
            "tokenizer_config.json" to 3_441L,
        ),
        revision = "5f5f794c1c23c9d5ee142af85df02a6cc52d6945",
    ),
    "onnx-phi4-mini" to folderSpec(
        repo = "microsoft/Phi-4-mini-instruct-onnx",
        remoteRoot = "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
        files = listOf(
            "added_tokens.json" to 249L,
            "config.json" to 2_504L,
            "configuration_phi3.py" to 10_875L,
            "genai_config.json" to 1_520L,
            "merges.txt" to 2_418_348L,
            "model.onnx" to 52_118_230L,
            "model.onnx.data" to 4_856_573_952L,
            "special_tokens_map.json" to 587L,
            "tokenizer.json" to 15_524_095L,
            "tokenizer_config.json" to 2_960L,
            "vocab.json" to 3_910_310L,
        ),
        revision = "fc04c8f93df696602fd9f300a30d1bf2e3081347",
    ),
    "onnx-phi4" to folderSpec(
        repo = "microsoft/phi-4-onnx",
        remoteRoot = "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
        files = listOf(
            "config.json" to 802L,
            "configuration_phi3.py" to 10_875L,
            "genai_config.json" to 1_520L,
            "merges.txt" to 916_646L,
            "model.onnx" to 265_958L,
            "model.onnx.data" to 10_906_062_848L,
            "special_tokens_map.json" to 575L,
            "tokenizer.json" to 7_153_083L,
            "tokenizer_config.json" to 17_714L,
            "vocab.json" to 1_612_637L,
        ),
        revision = "c4e245b57793e8481e0fa9252b3a45dee44a921b",
    ),
    "onnx-llama3.2-3b" to folderSpec(
        repo = "onnx-community/Llama-3.2-3B-Instruct-GENAI-ONNX",
        remoteRoot = "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
        files = listOf(
            "config.json" to 877L,
            "genai_config.json" to 1_540L,
            "model.onnx" to 185_824L,
            "model.onnx.data" to 3_651_678_208L,
            "special_tokens_map.json" to 296L,
            "tokenizer.json" to 9_085_657L,
            "tokenizer_config.json" to 54_528L,
        ),
        revision = "5db5cdb5b0c8c440264ca0f16f5ec3351e753add",
    ),
)
