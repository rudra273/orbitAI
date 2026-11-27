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
| qwen35-0.8b | 963.2 MB | 1 | [litert-community/Qwen3.5-0.8B@03d910d3](https://huggingface.co/litert-community/Qwen3.5-0.8B/tree/03d910d3643b5ef29babed32327bdfad39fd1aef) |
| qwen35-0.8b-vl | 1.30 GB | 1 | [litert-community/Qwen3.5-0.8B@03d910d3](https://huggingface.co/litert-community/Qwen3.5-0.8B/tree/03d910d3643b5ef29babed32327bdfad39fd1aef) |
| onnx-llama3.2-3b | 3.66 GB | 7 | [onnx-community/Llama-3.2-3B-Instruct-GENAI-ONNX@5db5cdb5](https://huggingface.co/onnx-community/Llama-3.2-3B-Instruct-GENAI-ONNX/tree/5db5cdb5b0c8c440264ca0f16f5ec3351e753add) |

Universal Sentence Encoder: 6.1 MB (6,120,274 bytes), verified from the existing Google Storage download response.

Gemma 2 previously referenced a missing file. Its URL now points to the provider's `Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task`, retaining the existing local filename. Added Gemma 3 1B LiteRT as a compact alternative for chat and Orbit. Gated repositories still require the user to accept the provider license and supply a Hugging Face token.

The reported Gemma 4 E2B failure (`Vision Encoder model must have exactly one signature but got 3`) occurred with LiteRT-LM 0.10.0. LiteRT-LM 0.17.1 supports selecting among multiple vision signatures. Its Kotlin 2.4.0 dependency requires a compiler update before this runtime fix can be built and tested.

## Download integrity (2026-10-03)

SHA-256 digests for 8 of 13 models (Gemma 4, all ONNX models) are stored in `core/model/ModelChecksums.kt`. They come from the Hugging Face `paths-info` LFS metadata of the same pinned revisions, and every API size matched the pinned sizes above. Downloads are verified against them before the file is moved into place.

The gated repositories (`litert-community/Gemma3-1B-IT`, `google/gemma-3n-E4B-it-litert-lm`, `google/gemma-3n-E2B-it-litert-lm`, `litert-community/Gemma2-2B-IT`) need an authenticated request even for metadata, so they are verified by exact size only. Add their digests by running the same `paths-info` query with a Hugging Face token that has accepted the licenses.

## Qwen 3.5 (2026-10-04)

Added `qwen35-0.8b` (text, int8) and `qwen35-0.8b-vl` (with vision encoder). Apache-2.0, not gated; SHA-256 digests from the same pinned revision are in `ModelChecksums.kt`. The model card requires LiteRT-LM 0.15 or newer; Orbit uses 0.17.1. Load and chat on a device have not been tested yet.

## Embedding model: multilingual-e5-small (2026-10-04)

Ungated, MIT license, 100 languages including Hindi. Orbit downloads two files from [Xenova/multilingual-e5-small@761b726d](https://huggingface.co/Xenova/multilingual-e5-small/tree/761b726dd34fb83930e26aab4e9ac3899aa1fa78), a mirror of [intfloat/multilingual-e5-small](https://huggingface.co/intfloat/multilingual-e5-small) with ONNX exports:

| File | Size | SHA-256 |
| --- | --- | --- |
| onnx/model_int8.onnx | 118,054,593 B | 4d24e2bc01a447951524466ef533e52944bf48509e6552810bcee1a2711cb02c |
| tokenizer.json | 17,082,730 B | 0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39 |

Checked on a computer with ONNX Runtime and the Hugging Face tokenizer: it matches English and Hindi questions to the right passage across both languages. Orbit's own tokenizer (`UnigramTokenizer`) reproduces the reference token ids for 23 test texts (English, Hindi, Tamil, Gujarati, Japanese, symbols). It does not reproduce zero-width spaces. The check runs when `ORBIT_E5_TOKENIZER` points at `tokenizer.json`. Not yet run on a phone.

## LiteRT-LM 0.20.0 catalog update (2026-11-28)

Runtime moved from LiteRT-LM 0.17.1 to 0.20.0. Its classes are Java 25 bytecode, so JVM unit tests run on a Java 25 toolchain and CI uses JDK 25. Sizes and SHA-256 digests come from the Hugging Face LFS metadata of each pinned revision and are in `ModelChecksums.kt`.

| Model ID | Change | File | Download size | Provider revision |
| --- | --- | --- | --- | --- |
| gemma4-e2b | repinned, file byte-identical | gemma-4-E2B-it.litertlm | 2.59 GB | [litert-community/gemma-4-E2B-it-litert-lm@2e12bfd8](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/2e12bfd869b2962984f97a1f2a26807316d534cc) |
| gemma4-e4b | repinned, file byte-identical | gemma-4-E4B-it.litertlm | 3.66 GB | [litert-community/gemma-4-E4B-it-litert-lm@f254b5ba](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/tree/f254b5ba6e36f7d89cc7892e268e9877bc20f0a0) |
| qwen35-0.8b | new file (needs LiteRT-LM 0.20+) | Qwen3.5-0.8B_mixed48_h1024.litertlm | 486.5 MB | [litert-community/Qwen3.5-0.8B@7c656dc9](https://huggingface.co/litert-community/Qwen3.5-0.8B/tree/7c656dc9e84b135df6b702e54fbe14a05c67abb8) |
| qwen35-0.8b-vl | rebuilt file, same name | Qwen3.5-0.8B-VL_int8.litertlm | 1.30 GB | [litert-community/Qwen3.5-0.8B@7c656dc9](https://huggingface.co/litert-community/Qwen3.5-0.8B/tree/7c656dc9e84b135df6b702e54fbe14a05c67abb8) |
| qwen35-2b | added | Qwen3.5-2B_int8.litertlm | 2.12 GB | [litert-community/Qwen3.5-2B@ab919e9d](https://huggingface.co/litert-community/Qwen3.5-2B/tree/ab919e9d348ec29deb9b5c3c9c559dd50a3a147d) |
| qwen35-2b-vl | added | Qwen3.5-2B-VL_int8.litertlm | 3.15 GB | [litert-community/Qwen3.5-2B@ab919e9d](https://huggingface.co/litert-community/Qwen3.5-2B/tree/ab919e9d348ec29deb9b5c3c9c559dd50a3a147d) |
| qwen35-4b | added | Qwen3.5-4B_mixed_int4.litertlm | 2.75 GB | [litert-community/Qwen3.5-4B@d1d2c9e9](https://huggingface.co/litert-community/Qwen3.5-4B/tree/d1d2c9e977626705a749a0f36d58d345b4e09215) |

The previous `qwen35-0.8b` file (`Qwen3.5-0.8B_int8.litertlm`) is listed in `REPLACED_MODEL_FILES` and deleted once the new file has downloaded. Existing downloads of `qwen35-0.8b-vl` keep the older file, since a present file counts as downloaded.

Embedding model added: EmbeddingGemma 2 Text 270M, `embeddinggemma-2-text-270m.litertlm`, 164,626,432 B, SHA-256 `2d079ee2f6f066b1f368e8d7c819f55214eaef1d0513b312321901f30ab286fb`, from [litert-community/embeddinggemma-2-text-270m-litert-lm@9be6e8b9](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/tree/9be6e8b90982095dc05c2bd162e4b954ee4dbac7). Apache-2.0, not gated. Run with LiteRT-LM's `EmbeddingEngine` on the CPU, 512-token input, normalised vectors at the model's default size; queries use `task: search result | query: …` and stored text `title: none | text: …`. None of these models have been tested on a phone yet.
