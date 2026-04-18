package com.example.orbitai.core.model

data class ModelDownloadFile(
    val relativePath: String,
    val url: String,
    val sizeBytes: Long? = null,
)

data class ModelDownloadSpec(
    val files: List<ModelDownloadFile>,
    val requiresAuth: Boolean = false,
)

private fun hfResolveUrl(repo: String, path: String): String {
    return "https://huggingface.co/$repo/resolve/main/$path?download=true"
}

private fun singleFileSpec(
    repo: String,
    remotePath: String,
    localPath: String,
    requiresAuth: Boolean,
): ModelDownloadSpec {
    return ModelDownloadSpec(
        files = listOf(
            ModelDownloadFile(
                relativePath = localPath,
                url = hfResolveUrl(repo, remotePath),
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
): ModelDownloadSpec {
    return ModelDownloadSpec(
        files = files.map { (relativePath, sizeBytes) ->
            val remotePath = if (remoteRoot.isBlank()) relativePath else "$remoteRoot/$relativePath"
            ModelDownloadFile(
                relativePath = relativePath,
                url = hfResolveUrl(repo, remotePath),
                sizeBytes = sizeBytes,
            )
        },
        requiresAuth = requiresAuth,
    )
}

val MODEL_DOWNLOAD_SPECS = mapOf(
    "gemma3-1b" to singleFileSpec(
        repo = "litert-community/Gemma3-1B-IT",
        remotePath = "gemma3-1b-it-int4.task",
        localPath = "gemma3-1b-it-int4.task",
        requiresAuth = true,
    ),
    "gemma3-4b" to singleFileSpec(
        repo = "google/gemma-3n-E4B-it-litert-lm",
        remotePath = "gemma-3n-E4B-it-int4.litertlm",
        localPath = "gemma-3n-E4B-it-int4.litertlm",
        requiresAuth = true,
    ),
    "gemma3-2b" to singleFileSpec(
        repo = "google/gemma-3n-E2B-it-litert-lm",
        remotePath = "gemma-3n-E2B-it-int4.litertlm",
        localPath = "gemma-3n-E2B-it-int4.litertlm",
        requiresAuth = true,
    ),
    "gemma4-e2b" to singleFileSpec(
        repo = "litert-community/gemma-4-E2B-it-litert-lm",
        remotePath = "gemma-4-E2B-it.litertlm",
        localPath = "gemma-4-E2B-it-int4.litertlm",
        requiresAuth = true,
    ),
    "gemma4-e4b" to singleFileSpec(
        repo = "litert-community/gemma-4-E4B-it-litert-lm",
        remotePath = "gemma-4-E4B-it.litertlm",
        localPath = "gemma-4-E4B-it-int4.litertlm",
        requiresAuth = true,
    ),
    "gemma2-2b" to singleFileSpec(
        repo = "litert-community/Gemma2-2B-IT",
        remotePath = "gemma2-2b-it-cpu-int8.task",
        localPath = "gemma2-2b-it-cpu-int8.task",
        requiresAuth = true,
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
    ),
    "onnx-gemma3-270m-it" to folderSpec(
        repo = "smartvest-llc/gemma-3-270m-it-genai-int4-android",
        remoteRoot = "",
        files = listOf(
            "added_tokens.json" to 35L,
            "chat_template.jinja" to 1_530L,
            "genai_config.json" to 1_510L,
            "model.onnx" to 229_000L,
            "model.onnx.data" to 906_000_000L,
            "special_tokens_map.json" to 662L,
            "tokenizer.json" to 33_400_000L,
            "tokenizer.model" to 4_690_000L,
            "tokenizer_config.json" to 1_160_000L,
        ),
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
    ),
)
