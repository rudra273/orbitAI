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
- [x] Works with no extra runtime permission
- [x] Captured image appears in the 3-image attachment strip and persists with the message
- [x] Hidden when the selected model has no vision support

**Status:** 🟡 Done, device check pending (2026-10-04). Attach button on vision models opens Camera / Photos / Files. Camera uses TakePicture into an app-private FileProvider file (no CAMERA permission declared, so none is needed); Photos uses the system photo picker. Images are downscaled to 1024 px before inference. Non-vision models keep the plain file picker.
