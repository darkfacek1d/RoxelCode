package com.roxelcode.app.editor

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class LanguageDef(
    val id: String,
    val name: String,
    val extensions: List<String>,
    val keywords: List<String>,
    val keywordColor: Color = Color(0xFFC586C0),
    val stringColor: Color = Color(0xFFCE9178),
    val numberColor: Color = Color(0xFFB5CEA8),
    val commentColor: Color = Color(0xFF6A9955),
    val functionColor: Color = Color(0xFFDCDCAA),
    val typeColor: Color = Color(0xFF4EC9B0),
    val lineComment: String = "//",
    val blockCommentStart: String? = null,
    val blockCommentEnd: String? = null,
    val isCustom: Boolean = false
)

object BuiltInLanguages {

    val PYTHON = LanguageDef(
        id = "python", name = "Python",
        extensions = listOf("py", "pyw"),
        keywords = listOf(
            "def", "class", "if", "elif", "else", "for", "while", "return",
            "import", "from", "as", "try", "except", "finally", "with",
            "pass", "break", "continue", "lambda", "yield", "global",
            "nonlocal", "assert", "del", "raise", "in", "is", "not", "and",
            "or", "None", "True", "False", "async", "await", "self"
        ),
        lineComment = "#",
        blockCommentStart = "\"\"\"", blockCommentEnd = "\"\"\""
    )

    val JAVASCRIPT = LanguageDef(
        id = "javascript", name = "JavaScript",
        extensions = listOf("js", "mjs", "cjs", "jsx"),
        keywords = listOf(
            "function", "const", "let", "var", "if", "else", "for", "while",
            "return", "class", "extends", "new", "this", "super", "import",
            "export", "default", "from", "try", "catch", "finally", "throw",
            "switch", "case", "break", "continue", "typeof", "instanceof",
            "null", "undefined", "true", "false", "async", "await", "yield",
            "delete", "in", "of", "do"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val TYPESCRIPT = JAVASCRIPT.copy(
        id = "typescript", name = "TypeScript",
        extensions = listOf("ts", "tsx"),
        keywords = JAVASCRIPT.keywords + listOf(
            "interface", "type", "enum", "implements", "public", "private",
            "protected", "readonly", "abstract", "namespace", "declare",
            "any", "string", "number", "boolean", "void", "never", "unknown"
        ),
        typeColor = Color(0xFF4EC9B0)
    )

    val HTML = LanguageDef(
        id = "html", name = "HTML",
        extensions = listOf("html", "htm", "xhtml"),
        keywords = listOf(
            "html", "head", "body", "div", "span", "p", "a", "img", "h1",
            "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "table", "tr",
            "td", "th", "form", "input", "button", "script", "style",
            "link", "meta", "title", "header", "footer", "nav", "section",
            "article", "aside", "main", "br", "hr", "strong", "em"
        ),
        keywordColor = Color(0xFF569CD6),
        stringColor = Color(0xFFCE9178),
        lineComment = "//",
        blockCommentStart = "<!--", blockCommentEnd = "-->"
    )

    val CSS = LanguageDef(
        id = "css", name = "CSS",
        extensions = listOf("css"),
        keywords = listOf(
            "color", "background", "background-color", "font-size",
            "font-family", "font-weight", "margin", "padding", "border",
            "display", "position", "top", "left", "right", "bottom",
            "width", "height", "flex", "grid", "gap", "opacity",
            "transform", "transition", "animation", "box-shadow",
            "border-radius", "text-align", "line-height", "overflow"
        ),
        keywordColor = Color(0xFF9CDCFE),
        stringColor = Color(0xFFCE9178),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val JSON = LanguageDef(
        id = "json", name = "JSON",
        extensions = listOf("json"),
        keywords = listOf("true", "false", "null"),
        keywordColor = Color(0xFF569CD6),
        stringColor = Color(0xFFCE9178),
        lineComment = "//"
    )

    val XML = LanguageDef(
        id = "xml", name = "XML",
        extensions = listOf("xml", "svg", "plist"),
        keywords = emptyList(),
        keywordColor = Color(0xFF569CD6),
        stringColor = Color(0xFFCE9178),
        lineComment = "//",
        blockCommentStart = "<!--", blockCommentEnd = "-->"
    )

    val KOTLIN = LanguageDef(
        id = "kotlin", name = "Kotlin",
        extensions = listOf("kt", "kts"),
        keywords = listOf(
            "fun", "val", "var", "class", "object", "interface", "if", "else",
            "when", "for", "while", "do", "return", "break", "continue",
            "import", "package", "private", "public", "protected", "internal",
            "override", "open", "abstract", "sealed", "data", "enum", "companion",
            "this", "super", "null", "true", "false", "is", "as", "in", "out",
            "by", "lateinit", "suspend", "async", "await", "try", "catch",
            "finally", "throw", "init", "constructor", "inline", "noinline",
            "crossinline", "reified", "where", "typealias", "const"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val JAVA = LanguageDef(
        id = "java", name = "Java",
        extensions = listOf("java"),
        keywords = listOf(
            "public", "private", "protected", "class", "interface", "enum",
            "extends", "implements", "import", "package", "if", "else",
            "for", "while", "do", "switch", "case", "default", "break",
            "continue", "return", "try", "catch", "finally", "throw",
            "throws", "new", "this", "super", "static", "final", "abstract",
            "synchronized", "volatile", "transient", "native", "null",
            "true", "false", "void", "int", "long", "double", "float",
            "boolean", "char", "byte", "short", "String"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val C = LanguageDef(
        id = "c", name = "C",
        extensions = listOf("c", "h"),
        keywords = listOf(
            "int", "long", "short", "char", "float", "double", "void",
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "goto", "sizeof", "typedef",
            "struct", "union", "enum", "static", "extern", "const",
            "volatile", "register", "auto", "signed", "unsigned", "NULL"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val CPP = C.copy(
        id = "cpp", name = "C++",
        extensions = listOf("cpp", "cc", "cxx", "hpp", "hxx"),
        keywords = C.keywords + listOf(
            "class", "public", "private", "protected", "virtual", "override",
            "template", "typename", "namespace", "using", "new", "delete",
            "try", "catch", "throw", "true", "false", "nullptr", "auto",
            "constexpr", "noexcept", "friend", "operator", "this"
        )
    )

    val CSHARP = LanguageDef(
        id = "csharp", name = "C#",
        extensions = listOf("cs"),
        keywords = listOf(
            "using", "namespace", "class", "interface", "struct", "enum",
            "public", "private", "protected", "internal", "static", "readonly",
            "const", "virtual", "override", "abstract", "sealed", "new",
            "if", "else", "for", "foreach", "while", "do", "switch", "case",
            "default", "break", "continue", "return", "try", "catch",
            "finally", "throw", "this", "base", "null", "true", "false",
            "var", "int", "long", "double", "float", "bool", "string", "void"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val GO = LanguageDef(
        id = "go", name = "Go",
        extensions = listOf("go"),
        keywords = listOf(
            "package", "import", "func", "var", "const", "type", "struct",
            "interface", "map", "chan", "go", "defer", "if", "else", "for",
            "range", "switch", "case", "default", "break", "continue",
            "return", "goto", "fallthrough", "select", "nil", "true", "false"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val RUST = LanguageDef(
        id = "rust", name = "Rust",
        extensions = listOf("rs"),
        keywords = listOf(
            "fn", "let", "mut", "const", "static", "struct", "enum", "trait",
            "impl", "pub", "mod", "use", "crate", "self", "super", "if",
            "else", "match", "for", "while", "loop", "break", "continue",
            "return", "as", "in", "ref", "move", "async", "await", "dyn",
            "true", "false", "Some", "None", "Ok", "Err"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val PHP = LanguageDef(
        id = "php", name = "PHP",
        extensions = listOf("php", "phtml"),
        keywords = listOf(
            "function", "class", "interface", "trait", "extends", "implements",
            "public", "private", "protected", "static", "const", "var",
            "if", "else", "elseif", "for", "foreach", "while", "do",
            "switch", "case", "default", "break", "continue", "return",
            "try", "catch", "finally", "throw", "new", "echo", "print",
            "use", "namespace", "require", "include", "null", "true", "false"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val RUBY = LanguageDef(
        id = "ruby", name = "Ruby",
        extensions = listOf("rb"),
        keywords = listOf(
            "def", "class", "module", "if", "elsif", "else", "unless",
            "while", "until", "for", "in", "do", "end", "begin", "rescue",
            "ensure", "return", "yield", "break", "next", "redo", "retry",
            "self", "super", "nil", "true", "false", "and", "or", "not",
            "require", "require_relative", "include", "extend", "attr_accessor"
        ),
        lineComment = "#",
        blockCommentStart = "=begin", blockCommentEnd = "=end"
    )

    val SWIFT = LanguageDef(
        id = "swift", name = "Swift",
        extensions = listOf("swift"),
        keywords = listOf(
            "func", "var", "let", "class", "struct", "enum", "protocol",
            "extension", "if", "else", "guard", "for", "while", "repeat",
            "switch", "case", "default", "break", "continue", "return",
            "try", "catch", "throw", "throws", "import", "public", "private",
            "internal", "fileprivate", "open", "static", "final", "override",
            "init", "deinit", "self", "super", "nil", "true", "false", "as", "is", "in"
        ),
        lineComment = "//",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val SHELL = LanguageDef(
        id = "shell", name = "Shell",
        extensions = listOf("sh", "bash", "zsh"),
        keywords = listOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do",
            "done", "case", "esac", "function", "return", "exit", "echo",
            "export", "local", "readonly", "source", "cd", "ls", "pwd",
            "cp", "mv", "rm", "mkdir", "touch", "cat", "grep", "sed", "awk"
        ),
        lineComment = "#"
    )

    val SQL = LanguageDef(
        id = "sql", name = "SQL",
        extensions = listOf("sql"),
        keywords = listOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE",
            "SET", "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "INDEX",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "GROUP", "BY",
            "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "AS", "AND", "OR",
            "NOT", "NULL", "IS", "IN", "LIKE", "BETWEEN", "DISTINCT", "COUNT",
            "SUM", "AVG", "MIN", "MAX"
        ),
        keywordColor = Color(0xFF569CD6),
        lineComment = "--",
        blockCommentStart = "/*", blockCommentEnd = "*/"
    )

    val MARKDOWN = LanguageDef(
        id = "markdown", name = "Markdown",
        extensions = listOf("md", "markdown"),
        keywords = emptyList(),
        keywordColor = Color(0xFF569CD6),
        lineComment = "<!--",
        blockCommentStart = "<!--", blockCommentEnd = "-->"
    )

    val YAML = LanguageDef(
        id = "yaml", name = "YAML",
        extensions = listOf("yaml", "yml"),
        keywords = listOf("true", "false", "null", "yes", "no", "on", "off"),
        keywordColor = Color(0xFF569CD6),
        lineComment = "#"
    )

    val ALL = listOf(
        PYTHON, JAVASCRIPT, TYPESCRIPT, HTML, CSS, JSON, XML, KOTLIN,
        JAVA, C, CPP, CSHARP, GO, RUST, PHP, RUBY, SWIFT, SHELL, SQL,
        MARKDOWN, YAML
    )

    fun findByExtension(ext: String): LanguageDef? {
        return ALL.firstOrNull { it.extensions.contains(ext.lowercase()) }
    }

    fun findById(id: String): LanguageDef? = ALL.firstOrNull { it.id == id }
}

object CustomLanguageStorage {
    private const val PREFS = "roxelcode_languages"
    private const val KEY = "custom_languages"

    fun load(context: Context): List<LanguageDef> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LanguageDef(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    extensions = o.getJSONArray("extensions").let { a ->
                        (0 until a.length()).map { a.getString(it) }
                    },
                    keywords = o.getJSONArray("keywords").let { a ->
                        (0 until a.length()).map { a.getString(it) }
                    },
                    keywordColor = Color(o.optLong("keywordColor", 0xFFC586C0)),
                    stringColor = Color(o.optLong("stringColor", 0xFFCE9178)),
                    numberColor = Color(o.optLong("numberColor", 0xFFB5CEA8)),
                    commentColor = Color(o.optLong("commentColor", 0xFF6A9955)),
                    functionColor = Color(o.optLong("functionColor", 0xFFDCDCAA)),
                    typeColor = Color(o.optLong("typeColor", 0xFF4EC9B0)),
                    lineComment = o.optString("lineComment", "//"),
                    isCustom = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, langs: List<LanguageDef>) {
        val arr = JSONArray()
        langs.forEach { l ->
            val o = JSONObject()
            o.put("id", l.id)
            o.put("name", l.name)
            o.put("extensions", JSONArray(l.extensions))
            o.put("keywords", JSONArray(l.keywords))
            o.put("keywordColor", l.keywordColor.value.toLong())
            o.put("stringColor", l.stringColor.value.toLong())
            o.put("numberColor", l.numberColor.value.toLong())
            o.put("commentColor", l.commentColor.value.toLong())
            o.put("functionColor", l.functionColor.value.toLong())
            o.put("typeColor", l.typeColor.value.toLong())
            o.put("lineComment", l.lineComment)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
