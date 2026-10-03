# f040 Unified tool registry with native tool calling
Labels: enhancement, question
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** XL · **Area:** Automation / Architecture
**Depends on:** f008, f016

**Title:** One tool system for chat and bubble: native LiteRT-LM tool calls, with the regex router as fallback

**Description:**
There are two parallel tool systems today:
1. **Chat:** regex routing (`AutomationCommandParser`, `DeviceCommandParser`) → a per-tool draft prompt → a per-tool text parser (`EmailDraftParser`, `WhatsAppDraftParser`, `ReminderDraftParser`) → `AutomationExecutor`. Every new tool needs 3–4 new classes, and the model can't decide to use a tool unprompted ("text Mom I'll be late" only works if a regex matches).
2. **Bubble:** native LiteRT-LM tool calling via `OrbitBubbleToolSchema` (`take_screenshot`, `copy_to_clipboard`) with `automaticToolCalling = false`.

**Decision needed (owner):** confirm that native tool calling becomes the primary path on LiteRT-LM (and Gemini function calling), with regex commands kept as a deterministic fast path and for engines without tool support (ONNX, MediaPipe).

**Proposed design:**
- `OrbitTool` interface: `name`, `description`, typed parameters, `requiresConfirmation`, `requiredPermissions`, `execute(args): ToolResult`
- `ToolRegistry` builds the LiteRT-LM `ToolSet`, the Gemini function declarations and the "/" command palette (f028) from the same definitions
- Every side-effecting tool goes through a **confirmation card** in chat (edit fields → Confirm), as today's reminder and call reviews already do
- Port existing tools: email draft, WhatsApp, reminder, call, remember, copy, screenshot

**Acceptance Criteria:**
- [ ] Existing unit suites stay green (`ToolRouterTest`, draft parser tests); new tests for registry → schema generation
- [ ] "Text Mom I'm running late" on a LiteRT-LM model produces a WhatsApp/SMS confirmation card without a slash command
- [ ] No tool with side effects executes without explicit user confirmation
- [ ] Adding a new tool = one class + registration (documented in CLAUDE.md)
