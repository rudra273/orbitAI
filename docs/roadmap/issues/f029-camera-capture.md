# f029 Take a photo directly from chat for vision models
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P2 · **Size:** S · **Area:** Chat / Vision

**Title:** Add a camera option next to the attachment picker

**Description:**
Vision is supported (Gemma 3n E2B, Gemma 4 E2B/E4B, Gemma 3 4B ONNX, Gemini), but images come only through `ActivityResultContracts.OpenDocument()`. "What is this plant?" / "Translate this sign" needs a quick camera path.

**Proposed behavior:**
- Attachment menu: **Camera**, **Photos** (system photo picker, `PickVisualMedia`), **Files**
- Camera uses `ActivityResultContracts.TakePicture()` into an app-private `FileProvider` URI (no CAMERA permission needed when delegating to the camera app)
- Downscale to the model's expected resolution before inference to save memory

**Acceptance Criteria:**
- [ ] Works with no extra runtime permission
- [ ] Captured image appears in the 3-image attachment strip and persists with the message
- [ ] Hidden when the selected model has no vision support
