package com.example.orbitai.feature.memory

/**
 * Finds a personal fact worth remembering in a user message ("I live in Pune" → "Lives in Pune")
 * so chat can offer to save it. Only a few high-precision phrasings are recognised; anything
 * vague, negated, hypothetical or asked as a question produces nothing. Nothing is saved here:
 * the user confirms each suggestion.
 */
object MemorySuggester {

    private const val MAX_VALUE_WORDS = 4
    private const val VALUE = """([^.,!?;:\n]+?)(?=\s+(?:and|but|so|because|since|which|who|where|as|if)\b|[.,!?;:\n]|$)"""

    /** First words that mean the capture isn't a concrete name, place or employer. */
    private val vagueStarts = setOf(
        "a", "an", "the", "this", "that", "my", "your", "our", "his", "her", "their", "some", "any", "no", "not",
        "fear", "hope", "denial", "peace", "pain", "love", "here", "there", "home", "work", "school", "office",
        "now", "it", "you", "me", "him", "them", "myself", "yourself", "free", "bed", "debt", "trouble",
        "progress", "doubt", "confused", "sure", "sorry", "fine", "okay", "ok", "going", "trying", "just",
        "back", "later", "tomorrow", "today", "soon",
    )

    private const val PROFESSIONS =
        "engineer|developer|programmer|designer|teacher|professor|student|doctor|nurse|dentist|lawyer|" +
            "accountant|manager|consultant|analyst|scientist|researcher|architect|artist|writer|journalist|" +
            "photographer|chef|pilot|pharmacist|electrician|plumber|mechanic|farmer|entrepreneur|founder|" +
            "freelancer|musician|actor|editor|marketer|salesperson|banker|surgeon|therapist|psychologist|" +
            "intern|tester|administrator|technician|coach|trainer|officer|driver"

    private const val LANGUAGES = "english|hindi|odia|bengali|tamil|telugu|kannada|malayalam|marathi|gujarati|punjabi|urdu|" +
        "spanish|french|german|italian|portuguese|japanese|chinese|korean|arabic|russian"

    /** Hypotheticals, stories and facts about other people. */
    private val notAFact = Regex(
        """(?i)\b(?:if|imagine|pretend|suppose|would|wish|dream|maybe|story|roleplay|my (?:friend|brother|sister|mom|mother|dad|father|wife|husband|son|daughter|boss|colleague|cousin)'?s?)\b""",
    )

    private val rules: List<Pair<Regex, (MatchResult) -> String?>> = listOf(
        Regex("""(?i)\bmy name is $VALUE""") to { m -> value(m, maxWords = 3)?.let { "Name is ${capitalize(it)}" } },
        Regex("""(?i)^(?:please )?call me $VALUE""") to { m -> value(m, maxWords = 2)?.let { "Prefers to be called ${capitalize(it)}" } },
        Regex("""(?i)\bi (?:live|stay) in $VALUE""") to { m -> value(m)?.let { "Lives in ${capitalize(it)}" } },
        Regex("""(?i)\bi(?:'m| am) (?:currently )?(?:based|living|staying) in $VALUE""") to { m -> value(m)?.let { "Lives in ${capitalize(it)}" } },
        Regex("""(?i)\bi(?:'m| am) (?:originally )?from $VALUE""") to { m -> value(m)?.let { "Is from ${capitalize(it)}" } },
        Regex("""(?i)\bi work (?:at|for) $VALUE""") to { m -> value(m)?.let { "Works at ${capitalize(it)}" } },
        Regex("""(?i)\bi work as (an? )?$VALUE""") to { m -> value(m, group = 2)?.let { "Works as ${m.groupValues[1].lowercase()}$it" } },
        Regex("""(?i)\bi(?:'m| am) (an? (?:[\p{L}-]+ ){0,2}(?:$PROFESSIONS)s?)\b""") to { m -> "Is ${m.groupValues[1].lowercase()}" },
        Regex("""(?i)\bmy (?:job|profession|occupation) is (?:being )?(an? )?$VALUE""") to { m ->
            value(m, group = 2)?.let { "Works as ${m.groupValues[1].lowercase()}$it" }
        },
        Regex("""(?i)\bmy birthday is (?:on )?$VALUE""") to { m -> value(m)?.let { "Birthday is $it" } },
        Regex("""(?i)\bi(?:'m| am) (\d{1,2}) years old\b""") to { m -> "Is ${m.groupValues[1]} years old" },
        Regex("""(?i)\bmy fav(?:ou)?rite ([\p{L}]+(?: [\p{L}]+)?) is $VALUE""") to { m ->
            value(m, group = 2)?.let { "Favorite ${m.groupValues[1].lowercase()} is $it" }
        },
        Regex("""(?i)\bi(?:'m| am) (vegetarian|vegan|pescatarian|lactose intolerant|gluten[- ]free|diabetic|left-handed)\b""") to { m ->
            "Is ${m.groupValues[1].lowercase()}"
        },
        Regex("""(?i)\bi(?:'m| am) allergic to $VALUE""") to { m -> value(m)?.let { "Is allergic to $it" } },
        Regex("""(?i)\b(?:always )?(?:reply|answer|respond|talk to me|speak to me) (?:to me )?in ($LANGUAGES)\b""") to { m ->
            "Prefers replies in ${capitalize(m.groupValues[1].lowercase())}"
        },
    )

    /** Returns one suggested fact for [message], or null. */
    fun suggest(message: String): String? {
        for (sentence in message.split(Regex("""(?<=[.!?\n])\s+"""))) {
            val text = sentence.trim().replace(Regex("""(?i)^(?:hi|hello|hey|ok|okay|so|well)[,!.]?\s+"""), "")
            if (text.isEmpty() || isQuestion(text) || notAFact.containsMatchIn(text)) continue
            for ((regex, build) in rules) {
                regex.find(text)?.let(build)?.let { return it }
            }
        }
        return null
    }

    /** True when [fact] says the same as one of [existing] (ignoring case, punctuation and filler). */
    fun isDuplicate(fact: String, existing: List<String>): Boolean {
        val words = significantWords(fact)
        if (words.isEmpty()) return true
        return existing.any { other ->
            val otherWords = significantWords(other)
            otherWords.isNotEmpty() && words.intersect(otherWords).size.toDouble() / words.union(otherWords).size >= 0.8
        }
    }

    private val filler = setOf("the", "user", "users", "is", "a", "an", "in", "at", "as", "to", "of", "s")

    private fun significantWords(text: String): Set<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() && it !in filler }.toSet()

    private fun isQuestion(text: String): Boolean =
        text.endsWith("?") ||
            Regex("""(?i)^(?:what|where|when|why|how|who|which|do|does|did|is|are|am|can|could|should|would|will|write|tell|explain)\b""").containsMatchIn(text)

    private fun value(m: MatchResult, group: Int = 1, maxWords: Int = MAX_VALUE_WORDS): String? {
        val raw = m.groupValues[group].trim().trimEnd('\'', '"')
        val words = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > maxWords) return null
        if (words.first().lowercase() in vagueStarts) return null
        return raw
    }

    private fun capitalize(text: String): String =
        text.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
}
