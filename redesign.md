# OrbitAI: UI Redesign Context & Functional Blueprint

This document provides a comprehensive overview of **OrbitAI** to serve as a reference for your UI redesign and for generating high-quality new screens.

---

## 🚀 1. The Core Concept: What is OrbitAI?

**OrbitAI** is a private, **on-device AI Productivity Assistant** for Android. 

### The Problem It Solves:
*   **Privacy First**: Most AI assistants send data to the cloud. OrbitAI runs locally on the phone (using MediaPipe and LiteRtLm), ensuring user data never leaves the device.
*   **Personalized Knowledge**: It uses **RAG (Retrieval-Augmented Generation)** through "Spaces," allowing the AI to "read" your PDFs and notes to give answers based on your private data.
*   **Always-Ready Productivity**: It features a "Floating Bubble" (Orbit Bubble) that lives over other apps for instant AI access.

### Aesthetic Vibe (Current):
The current design uses a **"Space/Glassmorphism"** theme: deep navy backgrounds (`SpaceDeep`), vibrant violet accents (`VioletCore`), and semi-transparent "glassy" surfaces with subtle glows and borders.

---

## 🛠️ 2. Features & User Capabilities (Layout Independent)

*In the new UI, these features can be placed anywhere. This is what the user **can do** with OrbitAI:*

### A. Conversational AI (The "Chat")
*   **Multi-Threaded Conversations**: Start, name, and delete separate chat threads.
*   **Multimodal Input**: Talk to the AI via text or **Voice (Speech-to-Text)**.
*   **Document/Image Context**: "Drop" files (PDFs, TXT, MD, etc.) or images into a chat so the AI can analyze them.
*   **Engine Switching**: Switch between different AI "Brains" (Model Engines) mid-conversation (e.g., a fast small model vs. a powerful large one).
*   **Real-time Control**: Stop the AI while it's still typing if the answer isn't what you wanted.

### B. Knowledge Management (The "Spaces")
*   **Custom Knowledge Bases**: Create "Spaces" (e.g., "Medical Research," "Travel Plans," "Work Snippets").
*   **RAG (Retrieval)**: Add documents to a space. The AI "indexes" them so it can recall specific facts from them later.
*   **Space Management**: Add/Remove documents and see their indexing status (Processing vs. Ready).
*   **Selective Context**: During a chat, a user can "toggle on" specific Spaces. The AI then uses the documents in those active spaces to answer questions.

### C. Personalization & Modes (The "Personalities")
*   **Custom Personas**: Create "Modes" with custom **System Prompts**. A "Python Expert" mode, a "Creative Writer" mode, or a "Sarcastic Friend" mode.
*   **Deep Tuning**: For each mode, users can adjust technical AI settings:
    *   *Temperature*: (Creative/Random vs. Focused/Deterministic).
    *   *Token Limits*: (Short vs. Long responses).
    *   *Top-K / Top-P*: (Vocabulary diversity).
*   **One-Tap Switching**: Quickly flip between these personas during a chat.

### D. Power Tools & Automation (The "Action" Layer)
*   **Smart Drafting**: The AI can generate ready-to-send drafts for:
    *   **Emails**: Automatically populates recipients and subjects based on chat context.
    *   **WhatsApp**: Can open WhatsApp with a pre-filled message for a specific contact.
*   **Automated Reminders**: The AI can "understand" a request for a reminder and:
    *   Schedule it directly in the system calendar.
    *   Set up a local notification for a specific time.
*   **Proactive Memory**: The AI doesn't just wait to be told to remember; it actively "listens" for user facts (e.g., "I'm allergic to nuts") and saves them to the long-term memory system automatically.

### E. Memory System (The "Long-term Recall")
*   **Autonomous Memory**: The AI can automatically "remember" facts about the user (e.g., "I am a vegan," "I live in Berlin").
*   **Memory Management**: Users can view everything the AI "knows" about them, edit those memories, or delete them.

### F. The Orbit Bubble (Omnipresent Assistant)
*   **Persistent Access**: A floating icon stays on top of other apps for instant triggers.
*   **Visual Styles**:
    *   *Round*: A classic floating bubble.
    *   *Slide*: A subtle tab on the edge of the screen.
*   **Deep Customization**: Users can tune the bubble's size, its transparency when idle, and the height of its response window.
*   **Behavioral Modes**:
    *   *Overlay Mode*: Results pop up in a small window over the current app.
    *   *App Mode*: Results jump back into the main OrbitAI chat interface.

---

## 🧭 3. The "Wiring" (Functional Components)

*These are the functional elements and actions that must exist in your new layout:*

### Global Navigation & Hubs
*   **Chat Hub**: Recent conversations + "New Chat" FAB.
*   **Spaces Hub**: Knowledge bases displayed in a vertical pager.
*   **Modes Hub**: Grid of AI personalities + "New Mode" toggle.
*   **Settings Hub**: Central access to Models, Memory, Tools, and Bubble settings.

### User Actions & Secondary Controls
*   **Model Management**: Download models (HF/Custom URL), delete models, and configure API keys (Gemini).
*   **Knowledge Management**: Add/Remove documents from Spaces and monitor indexing status.
*   **Memory Management**: Clear individual facts or toggle the entire memory system.
*   **Tool Configuration**: Toggle "Automation" for reminders (Background vs. UI handoff).
*   **Theme Switch**: Global Light/Dark mode toggle.

### Chat Interface Components
*   **Input Layer**: Text field, Voice trigger, and Document/Image attachment button.
*   **Context Layer**: Selectors for the active "Brain" (Model), "Personality" (Mode), and "Knowledge" (Spaces).
*   **Execution Layer**: Stop-generation button and real-time status banners.

---
