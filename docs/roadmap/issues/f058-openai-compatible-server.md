# f058 Connect to your own server (OpenAI-compatible API on your network)
Labels: enhancement, question
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Engine / Cloud

**Title:** A third provider: self-hosted models via an OpenAI-compatible endpoint the user enters

**Description:**
Phones top out around 4B-parameter models. Many privacy-minded users already run larger models at home through local servers that expose an OpenAI-compatible chat-completions API. Letting Orbit talk to **a user-entered** endpoint gives them big-model quality while keeping data in their own network.

**Decision needed (owner):** confirm scope (chat only vs tools/vision) and whether non-HTTPS LAN addresses are allowed, which needs a scoped network-security-config exception because `usesCleartextTraffic="false"`.

**Proposed behavior:**
- `ModelProvider.OPENAI_COMPATIBLE`; Settings → Models → "Add server": base URL, optional API key, model name (fetched from the server's model list if available)
- New `OpenAiCompatibleEngine` streaming SSE responses; structured messages (shares the f015 structured `InferenceInput`)
- Treated like Gemini for consent: the first send per chat shows "This will be sent to <host>"
- No default URL ships in the app (project rule: never fabricate endpoints)

**Acceptance Criteria:**
- [ ] Works against at least one popular local server (named in the PR)
- [ ] Connection test button with clear errors (unreachable, 401, model missing)
- [ ] API key stored encrypted (f056)
- [ ] Cleartext allowed only if the owner approves, and only for private IP ranges the user explicitly confirms
