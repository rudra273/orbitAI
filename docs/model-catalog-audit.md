# Model catalog audit

Checked 2026-10-02 against provider repository metadata. Download links are pinned to verified revisions so upstream changes cannot silently replace the chosen files. Existing local filenames remain unchanged to preserve installed models. Sizes use decimal MB/GB and include all required files for ONNX models. Runtime inference must still be tested separately for each model/device.

| Model ID | Download size | Files | Provider revision |
| --- | --- | --- | --- |
| gemma3-1b-litertlm | 584.4 MB | 1 | [litert-community/Gemma3-1B-IT@a6306a4e](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/a6306a4e292016480083b73b8dc6f3f939ae04c3) |
| gemma3-1b | 554.7 MB | 1 | [litert-community/Gemma3-1B-IT@a6306a4e](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/a6306a4e292016480083b73b8dc6f3f939ae04c3) |
| gemma3-4b | 4.92 GB | 1 | [google/gemma-3n-E4B-it-litert-lm@297ed759](https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/tree/297ed75955702dec3503e00c2c2ecbbf475300bc) |
| gemma3-2b | 3.66 GB | 1 | [google/gemma-3n-E2B-it-litert-lm@c03b6f60](https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/tree/c03b6f60b8da6c5400b6838a2cf26420f80c0a01) |
| gemma4-e2b | 2.59 GB | 1 | [litert-community/gemma-4-E2B-it-litert-lm@b3ca0d2f](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1) |
| gemma4-e4b | 3.66 GB | 1 | [litert-community/gemma-4-E4B-it-litert-lm@2eee7ac3](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/tree/2eee7ac325f20eb8c9ac1d0e972f7c84663062da) |
| gemma2-2b | 2.71 GB | 1 | [litert-community/Gemma2-2B-IT@0f272212](https://huggingface.co/litert-community/Gemma2-2B-IT/tree/0f2722125e4cd85da620e6a0ebcb26b87a230049) |
| onnx-gemma3-4b-it | 6.06 GB | 12 | [onnxruntime/Gemma-3-ONNX@57c665cf](https://huggingface.co/onnxruntime/Gemma-3-ONNX/tree/57c665cff71315f7c63d346549cfeea69d90048e) |
| onnx-gemma3-270m-it | 945.4 MB | 9 | [smartvest-llc/gemma-3-270m-it-genai-int4-android@7e9779de](https://huggingface.co/smartvest-llc/gemma-3-270m-it-genai-int4-android/tree/7e9779dea004bc5d5767ddd419d02d4cabcae1a5) |
| onnx-phi3-mini-4k | 2.73 GB | 10 | [microsoft/Phi-3-mini-4k-instruct-onnx@5f5f794c](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/tree/5f5f794c1c23c9d5ee142af85df02a6cc52d6945) |
| onnx-phi4-mini | 4.93 GB | 11 | [microsoft/Phi-4-mini-instruct-onnx@fc04c8f9](https://huggingface.co/microsoft/Phi-4-mini-instruct-onnx/tree/fc04c8f93df696602fd9f300a30d1bf2e3081347) |
| onnx-phi4 | 10.92 GB | 10 | [microsoft/phi-4-onnx@c4e245b5](https://huggingface.co/microsoft/phi-4-onnx/tree/c4e245b57793e8481e0fa9252b3a45dee44a921b) |
| onnx-llama3.2-3b | 3.66 GB | 7 | [onnx-community/Llama-3.2-3B-Instruct-GENAI-ONNX@5db5cdb5](https://huggingface.co/onnx-community/Llama-3.2-3B-Instruct-GENAI-ONNX/tree/5db5cdb5b0c8c440264ca0f16f5ec3351e753add) |

Universal Sentence Encoder: 6.1 MB (6,120,274 bytes), verified from the existing Google Storage download response.

Gemma 2 previously referenced a missing file. Its URL now points to the provider's `Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task`, retaining the existing local filename. Added Gemma 3 1B LiteRT as a compact alternative for chat and Orbit. Gated repositories still require the user to accept the provider license and supply a Hugging Face token.

The reported Gemma 4 E2B failure (`Vision Encoder model must have exactly one signature but got 3`) occurred with LiteRT-LM 0.10.0. LiteRT-LM 0.17.1 supports selecting among multiple vision signatures. Its Kotlin 2.4.0 dependency requires a compiler update before this runtime fix can be built and tested.
