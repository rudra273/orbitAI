# f009 Enable R8 code and resource shrinking for release builds
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P1 · **Size:** M · **Area:** Release

**Title:** Turn on `isMinifyEnabled` / `isShrinkResources` with correct keep rules for the native AI runtimes

**Description:**
Release builds set `isMinifyEnabled = false` and `proguard-rules.pro` is empty. The APK carries MediaPipe GenAI, LiteRT-LM, ONNX Runtime + GenAI, the Google GenAI SDK, PDFBox and the full `material-icons-extended`, all unshrunk. Users already download multi-GB models, so a lean app install still matters for Play conversion.

These runtimes use JNI and reflection, so keep rules have to be found by testing, not guessed.

**Acceptance Criteria:**
- [ ] `isMinifyEnabled = true` and `isShrinkResources = true` for `release`
- [ ] Keep rules added for LiteRT-LM (`com.google.ai.edge.litertlm.**` tool annotations), ONNX Runtime GenAI JNI classes, MediaPipe tasks, Google GenAI SDK models and PDFBox resources, each with a comment naming the crash it prevents
- [ ] Release build smoke-tested: one LiteRT-LM model, one ONNX model, one `.task` model, Gemini, PDF ingestion, the bubble with tool calls
- [ ] APK/AAB size before and after recorded in the PR
- [ ] `mapping.txt` uploaded as a CI artifact on tagged releases (for deobfuscating crash reports, see f010)
