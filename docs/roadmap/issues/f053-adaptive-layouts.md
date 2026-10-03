# f053 Adaptive layouts for tablets, foldables and landscape
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P2 · **Size:** M · **Area:** UI

**Title:** List-detail chat layout on large screens

**Description:**
All screens are phone-portrait layouts. On tablets and unfolded foldables (which often have the RAM for 4B models), the chat stretches edge to edge and the chat list sits on a separate screen.

**Proposed behavior:**
- Window size classes: on expanded width, chat list + chat detail side by side; Spaces list + Space detail the same way
- Navigation rail instead of bottom bar on medium/expanded
- Max readable width for message bubbles (~720 dp)
- Survive fold/unfold and rotation without losing input text or interrupting a streaming reply

**Acceptance Criteria:**
- [ ] Tested on a tablet emulator, a foldable emulator, and in landscape on a phone
- [ ] No generation restart on configuration change
