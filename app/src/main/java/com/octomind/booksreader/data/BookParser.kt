package com.octomind.booksreader.data

import android.graphics.Bitmap
import com.octomind.booksreader.domain.BookChapter
import com.octomind.booksreader.domain.BookFormat
import com.octomind.booksreader.domain.BookPageAnchor
import com.octomind.booksreader.domain.ParsedBook
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

interface BookParser {
    fun parse(
        file: File,
        displayName: String,
    ): ParsedBook
}

class TxtBookParser : BookParser {
    override fun parse(
        file: File,
        displayName: String,
    ): ParsedBook {
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
    override fun parse(
        file: File,
        displayName: String,
    ): ParsedBook {
        require(file.length() <= MAX_EPUB_BYTES) { "El archivo EPUB supera el límite de 100 MB" }
        ZipFile(file).use { zip ->
            validateArchive(zip)
            val containerXml = zip.readEntry("META-INF/container.xml")
            val packagePath = parseContainerPath(containerXml)
            val packageXml = zip.readEntry(packagePath)
            val packageDocument = parseXml(packageXml)
            val packageDirectory = packagePath.substringBeforeLast('/', "")

            val title =
                packageDocument
                    .elementsByLocalName("title")
                    .firstOrNull()
                    ?.textContent
                    ?.trim()
                    .orEmpty()
                    .ifBlank { displayName.substringBeforeLast('.') }
            val author =
                packageDocument
                    .elementsByLocalName("creator")
                    .firstOrNull()
                    ?.textContent
                    ?.trim()
                    ?.takeIf(String::isNotBlank)

            val manifest =
                packageDocument.elementsByLocalName("item").associate { element ->
                    element.getAttribute("id") to resolvePath(packageDirectory, element.getAttribute("href"))
                }
            val manifestItems = packageDocument.elementsByLocalName("item")
            val epub2CoverId =
                packageDocument
                    .elementsByLocalName("meta")
                    .firstOrNull { it.getAttribute("name").equals("cover", ignoreCase = true) }
                    ?.getAttribute("content")
            val coverElement =
                manifestItems.firstOrNull {
                    it.getAttribute("properties").split(Regex("\\s+")).contains("cover-image")
                } ?: manifestItems.firstOrNull { it.getAttribute("id") == epub2CoverId }
                    ?: manifestItems.firstOrNull {
                        it.getAttribute("id").contains("cover", ignoreCase = true) &&
                            it.getAttribute("media-type").startsWith("image/")
                    }
            val coverImage =
                coverElement
                    ?.takeIf { it.getAttribute("media-type") in SUPPORTED_COVER_MIME_TYPES }
                    ?.let { resolvePath(packageDirectory, it.getAttribute("href")) }
                    ?.let { coverPath -> zip.readEntry(coverPath) }
                    ?.takeIf { it.isNotEmpty() }
            val spine =
                packageDocument
                    .elementsByLocalName("itemref")
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
                    val chapterTitle =
                        extractHtmlTitle(xhtml)
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
        return document
            .elementsByLocalName("rootfile")
            .firstOrNull()
            ?.getAttribute("full-path")
            ?.takeIf(String::isNotBlank)
            ?: error("El EPUB no declara su archivo de contenido")
    }

    private fun parseXml(bytes: ByteArray) =
        DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }.newDocumentBuilder()
            .parse(bytes.inputStream())

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

    private fun resolvePath(
        base: String,
        href: String,
    ): String {
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
        val heading =
            Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)
                ?.groupValues
                ?.get(1)
        return heading?.let(::stripTags)?.takeIf(String::isNotBlank)
    }

    private fun xhtmlToText(html: String): String {
        val withoutHidden =
            html
                .replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</?(p|div|h[1-6]|li|blockquote|section|article)[^>]*>", RegexOption.IGNORE_CASE), "\n")
        return decodeEntities(stripTags(withoutHidden))
    }

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), " ").trim()

    private fun decodeEntities(value: String): String =
        value
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&apos;", "'", ignoreCase = true)
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1]
                    .toIntOrNull()
                    ?.toChar()
                    ?.toString() ?: ""
            }.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                match.groupValues[1]
                    .toIntOrNull(16)
                    ?.toChar()
                    ?.toString() ?: ""
            }

    private companion object {
        const val MAX_EPUB_BYTES = 100L * 1024 * 1024
        const val MAX_EXPANDED_BYTES = 150L * 1024 * 1024
        const val MAX_SINGLE_ENTRY_BYTES = 10L * 1024 * 1024
        const val MAX_ENTRIES = 5_000
        val SUPPORTED_COVER_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}

class PdfBookParser : BookParser {
    override fun parse(
        file: File,
        displayName: String,
    ): ParsedBook {
        require(file.length() <= MAX_PDF_BYTES) { "El archivo PDF supera el límite de 200 MB" }
        require(
            file.inputStream().use { input ->
                val signature = ByteArray(PDF_SIGNATURE.size)
                input.read(signature) == signature.size && signature.contentEquals(PDF_SIGNATURE)
            },
        ) { "El archivo no tiene una firma PDF válida" }

        val loadedDocument =
            try {
                PDDocument.load(file)
            } catch (_: InvalidPasswordException) {
                throw IllegalArgumentException("Los PDF protegidos con contraseña todavía no son compatibles")
            }
        loadedDocument.use { document ->
            require(!document.isEncrypted) { "Los PDF protegidos con contraseña todavía no son compatibles" }
            require(document.numberOfPages in 1..MAX_PDF_PAGES) {
                "El PDF supera el límite de $MAX_PDF_PAGES páginas"
            }
            val textStripper =
                PDFTextStripper().apply {
                    sortByPosition = true
                    lineSeparator = "\n"
                    paragraphStart = "\n\n"
                    paragraphEnd = "\n\n"
                }
            val combinedText = StringBuilder()
            val pageAnchors = mutableListOf<BookPageAnchor>()
            val chapters = mutableListOf<BookChapter>()

            repeat(document.numberOfPages) { pageIndex ->
                textStripper.startPage = pageIndex + 1
                textStripper.endPage = pageIndex + 1
                val rawPageText = textStripper.getText(document)
                val pageText = normalizePdfPageText(rawPageText)
                if (pageText.isNotBlank()) {
                    if (combinedText.isNotEmpty()) combinedText.append("\n\n")
                    val pageStart = combinedText.length
                    pageAnchors += BookPageAnchor(pageIndex, pageStart)
                    detectPdfChapterTitle(pageText)?.let { title ->
                        if (chapters.none { it.title.equals(title, ignoreCase = true) }) {
                            chapters += BookChapter(title, pageStart)
                        }
                    }
                    combinedText.append(pageText)
                }
            }

            val text = normalizeBookText(combinedText.toString())
            require(text.isNotBlank()) {
                "Este PDF parece contener páginas escaneadas sin texto seleccionable; requiere OCR"
            }
            val information = document.documentInformation
            val title =
                information
                    ?.title
                    ?.trim()
                    .orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.').ifBlank { "Libro sin título" }
            val author = information?.author?.trim()?.takeIf(String::isNotBlank)
            return ParsedBook(
                title = title,
                author = author,
                format = BookFormat.PDF,
                text = text,
                chapters = chapters.ifEmpty { listOf(BookChapter("Inicio", 0)) },
                coverImage = renderPdfCover(document),
                pageAnchors = pageAnchors,
            )
        }
    }

    private fun renderPdfCover(document: PDDocument): ByteArray? =
        runCatching {
            val mediaBox = document.getPage(0).mediaBox
            val longestEdge =
                maxOf(mediaBox.width, mediaBox.height).takeIf { it.isFinite() && it > 0f }
                    ?: return@runCatching null
            val renderScale = (COVER_LONGEST_EDGE_PIXELS / longestEdge).coerceIn(MINIMUM_COVER_SCALE, MAXIMUM_COVER_SCALE)
            val bitmap = PDFRenderer(document).renderImage(0, renderScale)
            try {
                ByteArrayOutputStream().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_JPEG_QUALITY, output)) {
                        return@runCatching null
                    }
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        }.getOrNull()

    private companion object {
        const val MAX_PDF_BYTES = 200L * 1024 * 1024
        const val MAX_PDF_PAGES = 5_000
        const val COVER_LONGEST_EDGE_PIXELS = 1_280f
        const val MINIMUM_COVER_SCALE = 0.1f
        const val MAXIMUM_COVER_SCALE = 2f
        const val COVER_JPEG_QUALITY = 88
        val PDF_SIGNATURE = "%PDF-".toByteArray(StandardCharsets.US_ASCII)
    }
}

internal fun normalizePdfPageText(value: String): String {
    val dehyphenated =
        value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\u00AD", "")
            .replace(Regex("(?<=\\p{L})-\\n(?=\\p{Ll})"), "")
            .filter { character -> character == '\n' || character == '\t' || !character.isISOControl() }
    val paragraphs = mutableListOf<String>()
    val current = StringBuilder()
    var contentsMode = false

    fun flushParagraph() {
        current
            .toString()
            .trim()
            .takeIf(String::isNotBlank)
            ?.let(paragraphs::add)
        current.clear()
    }

    dehyphenated.lines().forEach { rawLine ->
        val line = rawLine.trim().replace(Regex("[\\t ]+"), " ")
        when {
            line.isBlank() -> if (!contentsMode) flushParagraph()
            line.matches(Regex("^\\d{1,4}$")) -> Unit
            isPdfContentsHeading(line) -> {
                flushParagraph()
                paragraphs += normalizePdfContentsHeading(line)
                contentsMode = true
            }
            contentsMode && isPdfContentsEntry(line) -> {
                flushParagraph()
                current.append(normalizePdfStructuralLine(line))
            }
            contentsMode -> {
                if (current.isNotEmpty()) current.append(' ')
                current.append(line)
            }
            isPdfStandaloneLine(line) -> {
                flushParagraph()
                paragraphs += normalizePdfStructuralLine(line)
            }
            else -> {
                if (current.isNotEmpty()) current.append(' ')
                current.append(line)
            }
        }
    }
    flushParagraph()
    return paragraphs.joinToString("\n\n").trim()
}

private fun isPdfStandaloneLine(line: String): Boolean {
    val wordCount = Regex("\\S+").findAll(line).count()
    val structuralLine = line.pdfStructureKey()
    val isShortHeading =
        wordCount <= 12 &&
            line.length <= 90 &&
            (
                structuralLine.startsWith("capitulo ") ||
                    structuralLine == "prologo" ||
                    structuralLine == "epilogo" ||
                    structuralLine == "referencias" ||
                    line.matches(Regex("^[\\p{Lu}\\d][\\p{Lu}\\d '’.,:;()/-]+$"))
            )
    val isListItem = line.matches(Regex("^(?:[•▪◦]|\\d+[.)]|[a-zA-Z][.)])\\s+.+"))
    return isShortHeading || isListItem
}

private fun isPdfContentsHeading(line: String): Boolean =
    line.pdfStructureKey() in
        setOf(
            "indice",
            "contenido",
            "contenidos",
            "tabla de contenido",
            "tabla de contenidos",
            "contents",
        )

private fun normalizePdfContentsHeading(line: String): String =
    when (line.pdfStructureKey()) {
        "indice" -> "Índice"
        "contenido", "contenidos", "tabla de contenido", "tabla de contenidos" -> "Contenido"
        else -> line
    }

private fun isPdfContentsEntry(line: String): Boolean {
    val structuralLine = line.pdfStructureKey()
    return structuralLine.matches(Regex("^(?:capitulo|parte|seccion)\\s+[\\divxlcdm]+.*")) ||
        structuralLine.startsWith("prologo") ||
        structuralLine.startsWith("introduccion") ||
        structuralLine.startsWith("prefacio") ||
        structuralLine.startsWith("agradecimientos") ||
        structuralLine.startsWith("epilogo") ||
        structuralLine.startsWith("referencias") ||
        structuralLine.startsWith("apendice") ||
        structuralLine.startsWith("anexo") ||
        structuralLine.matches(Regex("^\\d+(?:\\.\\d+)*\\s+\\D.*"))
}

private fun normalizePdfStructuralLine(line: String): String {
    val cleaned = line.replace(Regex("\\s*\\.{2,}\\s*"), " · ").trim()
    val structuralLine = cleaned.pdfStructureKey()
    val title = cleaned.substringAfter(':', "").trim()
    val chapterNumber =
        Regex("^capitulo\\s+([\\divxlcdm]+)")
            .find(structuralLine)
            ?.groupValues
            ?.get(1)
    return when {
        chapterNumber != null && title.isNotEmpty() -> "Capítulo ${chapterNumber.uppercase()}: $title"
        structuralLine == "prologo" -> "Prólogo"
        structuralLine == "epilogo" -> "Epílogo"
        structuralLine == "introduccion" -> "Introducción"
        structuralLine == "referencias" -> "Referencias"
        else -> cleaned
    }
}

private fun String.pdfStructureKey(): String =
    Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace('ı', 'i')
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("capit\\s*ulo"), "capitulo")
        .replace(Regex("epil\\s*ogo"), "epilogo")
        .replace(Regex("prol\\s*ogo"), "prologo")
        .replace(Regex("ind\\s*ice"), "indice")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun detectPdfChapterTitle(pageText: String): String? {
    val openingParagraphs = pageText.split(Regex("\\n{2,}")).take(4)
    if (openingParagraphs.firstOrNull()?.let(::isPdfContentsHeading) == true) return null
    return openingParagraphs.firstOrNull { paragraph ->
        val structuralParagraph = paragraph.pdfStructureKey()
        paragraph.length <= 120 &&
            (
                structuralParagraph.startsWith("capitulo ") ||
                    structuralParagraph == "prologo" ||
                    structuralParagraph == "epilogo" ||
                    structuralParagraph == "referencias"
            )
    }
}

internal fun normalizeBookText(value: String): String =
    value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .joinToString("\n") { line -> line.trim().replace(Regex("[\\t ]+"), " ") }
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
