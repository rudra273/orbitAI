package com.example.orbitai.core.model

/**
 * SHA-256 of downloaded model files: modelId -> local relative path -> hex digest.
 * Taken from the Hugging Face LFS metadata of each pinned revision in [MODEL_DOWNLOAD_SPECS].
 * Gated repositories (Gemma 2/3/3n) are missing because their metadata needs an authenticated
 * request; those downloads are verified by exact size only.
 */
val MODEL_FILE_SHA256: Map<String, Map<String, String>> = mapOf(
    "qwen35-0.8b" to mapOf(
        "Qwen3.5-0.8B_mixed48_h1024.litertlm" to "61c65dbcaa86461184df1b60a82941c5d3e65fb535411d1b2192ccf222e98b35",
    ),
    "qwen35-0.8b-vl" to mapOf(
        "Qwen3.5-0.8B-VL_int8.litertlm" to "7413f51eafb82e7cf0b707fa506eddfc76c10433b26fa8f0dc2f051970d2feb5",
    ),
    "qwen35-2b" to mapOf(
        "Qwen3.5-2B_int8.litertlm" to "c2438cb9c050220e667366b4beeca4daac56e693b226420674f53fc40f007c49",
    ),
    "qwen35-2b-vl" to mapOf(
        "Qwen3.5-2B-VL_int8.litertlm" to "90ece8de37537b757ef21075a7d17c14bc11ac29793dada058a5be7c3fe03ccc",
    ),
    "qwen35-4b" to mapOf(
        "Qwen3.5-4B_mixed_int4.litertlm" to "878e5d1926ecc4930e24c62de43a7038edeeb0c57446231086b139892984a5fd",
    ),
    "gemma4-e2b" to mapOf(
        "gemma-4-E2B-it-int4.litertlm" to "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
    ),
    "gemma4-e4b" to mapOf(
        "gemma-4-E4B-it-int4.litertlm" to "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
    ),
    "onnx-gemma3-4b-it" to mapOf(
        "gemma-3-embedding.onnx" to "f3842735d19fa00f1c32304b3558b59f2dc59a703051612ab63af91e457e6533",
        "gemma-3-embedding.onnx.data" to "b33e3f6a89e7345d8735498377d4ceb0d9f09eaa83e911883139d04e0d8b1013",
        "gemma-3-text.onnx" to "5c17c14b6ddb9105c7eb3347b0ddb75a569b1c5da030054029b1eb5b0005abfd",
        "gemma-3-text.onnx.data" to "6141ac2a1f23de0d42d7e7ab851e312a96ee4defd4662085a34e2948f898430a",
        "gemma-3-vision.onnx" to "5a4f4cea8ab4ac1e1c26f28b4714844fcb73e66fe62432eb43b496c37a9dd54e",
        "gemma-3-vision.onnx.data" to "9e57accb40a4fd317ca18d413b3c6da406088854e04d6e2360ef5e3ddbd304a9",
        "genai_config.json" to "3ecf2309c4448ba9f1995e012cf07f21bbf3f42f1cb097a857970b26d994c563",
        "processor_config.json" to "e0efb6c6edbd60ba8c609d190429a1091d3bfd2ac4f765393b7c873ff16b0d6e",
        "special_tokens_map.json" to "2f7b0adf4fb469770bb1490e3e35df87b1dc578246c5e7e6fc76ecf33213a397",
        "tokenizer.json" to "4667f2089529e8e7657cfb6d1c19910ae71ff5f28aa7ab2ff2763330affad795",
        "tokenizer_config.json" to "983c80895e7b188911f5ccfbb4f780a4e73fb3131ed2080a3e847ef8623cfdca",
    ),
    "onnx-gemma3-270m-it" to mapOf(
        "model.onnx" to "cd4727011cc35fcc6db4bd009bc2d5ee62b3f063636ec928a036945deff89ef4",
        "model.onnx.data" to "1d0744ff1358f9bf14f65e041739d34264607a8af7eeb3eb71b370a1f53b6de5",
        "tokenizer.json" to "4667f2089529e8e7657cfb6d1c19910ae71ff5f28aa7ab2ff2763330affad795",
        "tokenizer.model" to "1299c11d7cf632ef3b4e11937501358ada021bbdf7c47638d13c0ee982f2e79c",
    ),
    "onnx-phi3-mini-4k" to mapOf(
        "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx" to "385cd1b908a0d2f8634e86d30236f6dbb7ae660eb3943fd1ef5bdc3847326480",
        "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data" to "5db30ce699aee1123cf9045742488db5928006fa618a42cb3c0840322a85ad0f",
        "tokenizer.model" to "9e556afd44213b6bd1be2b850ebbbd98f5481437a8021afaf58ee7fb1818d347",
    ),
    "onnx-phi4-mini" to mapOf(
        "added_tokens.json" to "d4f2aceb0f20b71dd1f4bcc7e052e4412946bf281840b8f83d39f259571af486",
        "config.json" to "ac65d86061d3d0d704ee2511fd0eb8713ef19eb6eedba17c3080a4165d5b933b",
        "genai_config.json" to "0fcfa1e663f2bc867f8dc62fae65dd0924f0a4d68b43d1234df742dd19171470",
        "model.onnx" to "701aa5d185b6a782bc27104a990dd5b634fa507840b7c42f7ee6f1fb812d0b83",
        "model.onnx.data" to "cb0267fa60befa1a4ade8c98b6d32a3d67f51abbd307c7f793f132e8d9092131",
        "special_tokens_map.json" to "aff38493227d813e29fcf8406e8e90062f1f031aa47d589325e9c31d89ac7cc3",
        "tokenizer.json" to "382cc235b56c725945e149cc25f191da667c836655efd0857b004320e90e91ea",
        "tokenizer_config.json" to "c565326a315fbe62cda093a59d298828c8f3f823122661325f41f3ba577a7dec",
        "vocab.json" to "6cb65a857824fa6615bb1782d95d882617a8bbce1da0317118586b36f39e98bd",
    ),
    "onnx-phi4" to mapOf(
        "config.json" to "07eedad2c48798b6e3728e4a1b75e0e092019a375ba725a40e40c78d13664045",
        "genai_config.json" to "cb5ffa9107d84fe58e1fbacc3819ea88422f00eaab7c28b8ac75fc3089c4b932",
        "model.onnx" to "f57c1187064f9fb47cadf6ade3b8a38c6256a92b2b19ffd675f63ae71de93465",
        "model.onnx.data" to "9a91462a3fe396cd28b99351baa3b19f6373056766d0f28ec024c6612d747f30",
        "special_tokens_map.json" to "80b78737e654c58bfdeca896e45765563f37252efe982251ede7aee157b8549b",
        "tokenizer.json" to "c612e57b2763204798271c0cdc14a373a565e6a371f1bfbd44e385a8391c9884",
        "tokenizer_config.json" to "0eb6d6c0b18f0730c9714dd429004c66aac14b59cd05ee88b9f825a24c12f575",
        "vocab.json" to "741943243bd0035a8082ef0ccc09ef62e7ce7ec2f2f053aeedd808dba1c2ffc5",
    ),
    "onnx-llama3.2-3b" to mapOf(
        "model.onnx" to "b159a71e593d75290c6827b2d67430b469947bbf9b807fd4e291ac80089caa65",
        "model.onnx.data" to "0a6801080cf0bd335d1f35909b574e70f6cdc0faadb3735289dee96fcbd19ec6",
    ),
)
