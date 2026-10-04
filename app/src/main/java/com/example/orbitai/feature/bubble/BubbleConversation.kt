package com.example.orbitai.feature.bubble

/** Recent question/answer pairs of one bubble session, so a follow-up keeps its context. Memory only. */
class BubbleConversation(private val maxExchanges: Int = 8) {

    data class Exchange(val question: String, val answer: String)

    private val items = ArrayDeque<Exchange>()

    val exchanges: List<Exchange> get() = items.toList()

    val isEmpty: Boolean get() = items.isEmpty()

    fun record(question: String, answer: String) {
        if (question.isBlank() || answer.isBlank()) return
        items.addLast(Exchange(question.trim(), answer.trim()))
        while (items.size > maxExchanges) items.removeFirst()
    }

    fun clear() = items.clear()

    /** For models without conversation support: earlier turns written into the single instruction. */
    fun withHistory(question: String): String {
        if (items.isEmpty()) return question
        val earlier = items.joinToString("\n") { "User: ${it.question}\nAssistant: ${it.answer}" }
        return "Earlier in this conversation:\n$earlier\n\nNow answer the user's new message.\nUser: $question"
    }
}
