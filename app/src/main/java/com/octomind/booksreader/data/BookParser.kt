package com.octomind.booksreader.data

import com.octomind.booksreader.domain.BookChapter
import com.octomind.booksreader.domain.BookFormat
import com.octomind.booksreader.domain.ParsedBook
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

interface BookParser {
    fun parse(file: File, displayName: String): ParsedBook
}

class TxtBookParser : BookParser {
    override fun parse(file: File, displayName: String): ParsedBook {
        require(file.length() <= MAX_TEXT_BYTES) { "El archivo TXT supera el límite de 25 MB" }
        val rawText = file.readText(StandardCharsets.UTF_8).removePrefix("\uFEFF")
        val text = normalizeBookText(rawText)
        require(text.isNotBlank()) { "El archivo no contiene texto legible" }
        return ParsedBook(
            title = displayName.substringBeforeLast('.').ifBlank { "Libro sin título" },
            author = null,
            format = BookFormat.TXT,
            text = text,
            chapters = listOf(BookChapter("Inicio", 0)),
        )
    }

    private companion object {
        const val MAX_TEXT_BYTES = 25L * 1024 * 1024
    }
}

class EpubBookParser : BookParser {
    override fun parse(file: File, displayName: String): ParsedBook {
        require(file.length() <= MAX_EPUB_BYTES) { "El archivo EPUB supera el límite de 100 MB" }
        ZipFile(file).use { zip ->
            validateArchive(zip)
            val containerXml = zip.readEntry("META-INF/container.xml")
            val packagePath = parseContainerPath(containerXml)
            val packageXml = zip.readEntry(packagePath)
            val packageDocument = parseXml(packageXml)
            val packageDirectory = packagePath.substringBeforeLast('/', "")

            val title = packageDocument.elementsByLocalName("title")
                .firstOrNull()?.textContent?.trim().orEmpty()
                .ifBlank { displayName.substringBeforeLast('.') }
            val author = packageDocument.elementsByLocalName("creator")
                .firstOrNull()?.textContent?.trim()?.takeIf(String::isNotBlank)

            val manifest = packageDocument.elementsByLocalName("item").associate { element ->
                element.getAttribute("id") to resolvePath(packageDirectory, element.getAttribute("href"))
            }
            val manifestItems = packageDocument.elementsByLocalName("item")
            val epub2CoverId = packageDocument.elementsByLocalName("meta")
                .firstOrNull { it.getAttribute("name").equals("cover", ignoreCase = true) }
                ?.getAttribute("content")
            val coverElement = manifestItems.firstOrNull {
                it.getAttribute("properties").split(Regex("\\s+")).contains("cover-image")
            } ?: manifestItems.firstOrNull { it.getAttribute("id") == epub2CoverId }
                ?: manifestItems.firstOrNull {
                    it.getAttribute("id").contains("cover", ignoreCase = true) &&
                        it.getAttribute("media-type").startsWith("image/")
                }
            val coverImage = coverElement
                ?.takeIf { it.getAttribute("media-type") in SUPPORTED_COVER_MIME_TYPES }
                ?.let { resolvePath(packageDirectory, it.getAttribute("href")) }
                ?.let { coverPath -> zip.readEntry(coverPath) }
                ?.takeIf { it.isNotEmpty() }
            val spine = packageDocument.elementsByLocalName("itemref")
                .mapNotNull { manifest[it.getAttribute("idref")] }
                .filter { it.endsWith(".xhtml", true) || it.endsWith(".html", true) || it.endsWith(".htm", true) }
                .ifEmpty {
                    manifest.values.filter {
                        it.endsWith(".xhtml", true) || it.endsWith(".html", true) || it.endsWith(".htm", true)
                    }
                }

            require(spine.isNotEmpty()) { "El EPUB no contiene capítulos legibles" }

            val combined = StringBuilder()
            val chapters = mutableListOf<BookChapter>()
            spine.distinct().forEachIndexed { index, path ->
                val xhtml = zip.readEntry(path).toString(StandardCharsets.UTF_8)
                val chapterText = normalizeBookText(xhtmlToText(xhtml))
                if (chapterText.isNotBlank()) {
                    if (combined.isNotEmpty()) combined.append("\n\n")
                    val chapterTitle = extractHtmlTitle(xhtml)
                        ?: "Capítulo ${index + 1}"
                    chapters += BookChapter(chapterTitle, combined.length)
                    combined.append(chapterText)
                }
            }

            val text = normalizeBookText(combined.toString())
            require(text.isNotBlank()) { "El EPUB no contiene texto legible" }
            return ParsedBook(
                title = title,
                author = author,
                format = BookFormat.EPUB,
                text = text,
                chapters = chapters.ifEmpty { listOf(BookChapter("Inicio", 0)) },
                coverImage = coverImage,
            )
        }
    }

    private fun validateArchive(zip: ZipFile) {
        val entries = zip.entries().toList()
        require(entries.size <= MAX_ENTRIES) { "El EPUB contiene demasiados archivos" }
        val expandedBytes = entries.sumOf { entry -> entry.size.coerceAtLeast(0L) }
        require(expandedBytes <= MAX_EXPANDED_BYTES) { "El EPUB expandido supera el límite permitido" }
    }

    private fun parseContainerPath(xml: ByteArray): String {
        val document = parseXml(xml)
        return document.elementsByLocalName("rootfile")
            .firstOrNull()?.getAttribute("full-path")
            ?.takeIf(String::isNotBlank)
            ?: error("El EPUB no declara su archivo de contenido")
    }

    private fun parseXml(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }.newDocumentBuilder().parse(bytes.inputStream())

    private fun org.w3c.dom.Document.elementsByLocalName(name: String): List<Element> {
        val namespaced = getElementsByTagNameNS("*", name)
        if (namespaced.length > 0) return (0 until namespaced.length).map { namespaced.item(it) as Element }
        val plain = getElementsByTagName(name)
        return (0 until plain.length).map { plain.item(it) as Element }
    }

    private fun ZipFile.readEntry(path: String): ByteArray {
        val safePath = path.replace('\\', '/').removePrefix("/")
        require(!safePath.split('/').contains("..")) { "Ruta no permitida dentro del EPUB" }
        val entry = getEntry(safePath) ?: error("Falta el archivo $safePath dentro del EPUB")
        require(entry.size in 0..MAX_SINGLE_ENTRY_BYTES) { "Una sección del EPUB supera el límite permitido" }
        return getInputStream(entry).use { it.readBytes() }
    }

    private fun resolvePath(base: String, href: String): String {
        val decoded = href.substringBefore('#').replace('\\', '/')
        val segments = (if (base.isBlank()) decoded else "$base/$decoded").split('/')
        val resolved = ArrayDeque<String>()
        for (segment in segments) {
            when (segment) {
                "", "." -> Unit
                ".." -> {
                    require(resolved.isNotEmpty()) { "Ruta no permitida dentro del EPUB" }
                    resolved.removeLast()
                }
                else -> resolved.addLast(segment)
            }
        }
        return resolved.joinToString("/")
    }

    private fun extractHtmlTitle(html: String): String? {
        val heading = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)
        return heading?.let(::stripTags)?.takeIf(String::isNotBlank)
    }

    private fun xhtmlToText(html: String): String {
        val withoutHidden = html
            .replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</?(p|div|h[1-6]|li|blockquote|section|article)[^>]*>", RegexOption.IGNORE_CASE), "\n")
        return decodeEntities(stripTags(withoutHidden))
    }

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), " ").trim()

    private fun decodeEntities(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
        .replace(Regex("&#(\\d+);")) { match -> match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: "" }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { match -> match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: "" }

    private companion object {
        const val MAX_EPUB_BYTES = 100L * 1024 * 1024
        const val MAX_EXPANDED_BYTES = 150L * 1024 * 1024
        const val MAX_SINGLE_ENTRY_BYTES = 10L * 1024 * 1024
        const val MAX_ENTRIES = 5_000
        val SUPPORTED_COVER_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}

internal fun normalizeBookText(value: String): String = value
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .lines()
    .joinToString("\n") { line -> line.trim().replace(Regex("[\\t ]+"), " ") }
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
