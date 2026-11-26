package com.example.orbitai.feature.chat.markdown

enum class TokenKind { KEYWORD, STRING, COMMENT, NUMBER }

data class CodeToken(val start: Int, val end: Int, val kind: TokenKind)

/** Lightweight lexical highlighting for common languages; unknown languages get strings/comments/numbers only. */
object CodeHighlighter {

    private val keywords: Map<String, Set<String>> = run {
        val cLike = setOf("if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",
            "class", "struct", "enum", "public", "private", "protected", "static", "const", "new", "try", "catch",
            "finally", "throw", "true", "false", "null", "void", "int", "long", "float", "double", "char", "bool",
            "boolean", "import", "package", "this", "super", "interface", "extends", "implements", "default")
        val kotlin = cLike + setOf("fun", "val", "var", "when", "is", "in", "object", "data", "sealed", "override",
            "suspend", "inline", "companion", "init", "lateinit", "internal", "open", "abstract", "typealias", "as")
        val js = cLike + setOf("function", "let", "var", "const", "async", "await", "export", "from", "of",
            "typeof", "instanceof", "undefined", "yield", "type")
        val python = setOf("def", "class", "if", "elif", "else", "for", "while", "in", "not", "and", "or", "is",
            "return", "import", "from", "as", "with", "try", "except", "finally", "raise", "lambda", "yield",
            "pass", "break", "continue", "None", "True", "False", "self", "async", "await", "global", "nonlocal")
        val shell = setOf("if", "then", "else", "elif", "fi", "for", "in", "do", "done", "while", "case", "esac",
            "function", "return", "export", "local", "echo", "cd", "sudo")
        mapOf(
            "kotlin" to kotlin, "kt" to kotlin, "java" to cLike, "c" to cLike, "cpp" to cLike, "c++" to cLike,
            "csharp" to cLike, "cs" to cLike, "go" to cLike + setOf("func", "package", "go", "defer", "chan", "map", "range", "type"),
            "swift" to cLike + setOf("func", "let", "var", "guard", "self", "nil"),
            "rust" to cLike + setOf("fn", "let", "mut", "impl", "trait", "match", "pub", "use", "mod", "self", "Self"),
            "javascript" to js, "js" to js, "typescript" to js, "ts" to js, "jsx" to js, "tsx" to js,
            "python" to python, "py" to python,
            "bash" to shell, "sh" to shell, "shell" to shell, "zsh" to shell,
        )
    }

    private val hashComments = setOf("python", "py", "bash", "sh", "shell", "zsh", "yaml", "yml", "ruby", "rb", "r")

    fun tokens(code: String, language: String): List<CodeToken> {
        val lang = language.lowercase().trim()
        val words = keywords[lang].orEmpty()
        val lineComment = if (lang in hashComments) "#" else "//"
        val out = mutableListOf<CodeToken>()
        var i = 0
        while (i < code.length) {
            val c = code[i]
            when {
                code.startsWith(lineComment, i) -> {
                    val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                    out += CodeToken(i, end, TokenKind.COMMENT); i = end
                }
                lineComment == "//" && code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 }
                    out += CodeToken(i, end, TokenKind.COMMENT); i = end
                }
                c == '"' || c == '\'' || c == '`' -> {
                    var j = i + 1
                    while (j < code.length && code[j] != c && code[j] != '\n') {
                        if (code[j] == '\\') j++
                        j++
                    }
                    val end = (j + 1).coerceAtMost(code.length)
                    out += CodeToken(i, end, TokenKind.STRING); i = end
                }
                c.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit() && code[i - 1] != '_') -> {
                    var j = i
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                    out += CodeToken(i, j, TokenKind.NUMBER); i = j
                }
                c.isLetter() || c == '_' -> {
                    var j = i
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    if (code.substring(i, j) in words) out += CodeToken(i, j, TokenKind.KEYWORD)
                    i = j
                }
                else -> i++
            }
        }
        return out
    }
}
