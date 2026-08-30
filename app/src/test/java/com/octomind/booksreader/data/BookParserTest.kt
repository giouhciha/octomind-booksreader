package com.octomind.booksreader.data

import com.octomind.booksreader.domain.BookFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BookParserTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `txt parser normalizes whitespace and creates a title`() {
        val file = temporaryFolder.newFile("lectura.txt")
        file.writeText("  Primera   línea.\r\n\r\n\r\nSegunda\tlínea.  ")

        val parsed = TxtBookParser().parse(file, "Mi lectura.txt")

        assertEquals(BookFormat.TXT, parsed.format)
        assertEquals("Mi lectura", parsed.title)
        assertEquals("Primera línea.\n\nSegunda línea.", parsed.text)
    }

    @Test
    fun `epub parser follows spine and preserves chapter locations`() {
        val file = temporaryFolder.newFile("sample.epub")
        createSyntheticEpub(file)

        val parsed = EpubBookParser().parse(file, "sample.epub")

        assertEquals(BookFormat.EPUB, parsed.format)
        assertEquals("Libro sintético", parsed.title)
        assertEquals("Autora de prueba", parsed.author)
        assertEquals(2, parsed.chapters.size)
        assertEquals(0, parsed.chapters.first().startCharacterOffset)
        assertTrue(parsed.chapters[1].startCharacterOffset > 0)
        assertTrue(parsed.text.indexOf("Primer contenido") < parsed.text.indexOf("Segundo contenido"))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), parsed.coverImage)
    }

    @Test
    fun `pdf normalization joins visual lines and preserves semantic breaks`() {
        val normalized =
            normalizePdfPageText(
                """
CAPÍTULO 1

Una oración repartida en varias
líneas conserva su ritmo natu-
ral.

42
• Primer punto de una lista
                """.trimIndent(),
            )

        assertEquals(
            "CAPÍTULO 1\n\nUna oración repartida en varias líneas conserva su ritmo natural.\n\n" +
                "• Primer punto de una lista",
            normalized,
        )
    }

    @Test
    fun `pdf normalization keeps contents entries and wrapped titles separate`() {
        val normalized =
            normalizePdfPageText(
                """
índice
Capıt́ ulo 1: Repensar la ciencia y la práctica clínicas
Capıt́ ulo 12: De los problemas a la prosperidad: mantenimiento y
expansión de las ganancias
Epıĺ ogo
Referencias
                """.trimIndent(),
            )

        assertEquals(
            "Índice\n\n" +
                "Capítulo 1: Repensar la ciencia y la práctica clínicas\n\n" +
                "Capítulo 12: De los problemas a la prosperidad: mantenimiento y expansión de las ganancias\n\n" +
                "Epílogo\n\nReferencias",
            normalized,
        )
    }

    private fun createSyntheticEpub(file: File) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.add(
                "META-INF/container.xml",
                """
<?xml version="1.0"?>
<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OPS/content.opf"/></rootfiles>
</container>
                """.trimIndent(),
            )
            zip.add(
                "OPS/content.opf",
                """
<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Libro sintético</dc:title>
    <dc:creator>Autora de prueba</dc:creator>
  </metadata>
  <manifest>
    <item id="c1" href="one.xhtml" media-type="application/xhtml+xml"/>
    <item id="c2" href="two.xhtml" media-type="application/xhtml+xml"/>
    <item id="cover" href="cover.png" media-type="image/png" properties="cover-image"/>
  </manifest>
  <spine><itemref idref="c1"/><itemref idref="c2"/></spine>
</package>
                """.trimIndent(),
            )
            zip.add("OPS/one.xhtml", "<html><body><h1>Primero</h1><p>Primer contenido legible.</p></body></html>")
            zip.add("OPS/two.xhtml", "<html><body><h1>Segundo</h1><p>Segundo contenido legible.</p></body></html>")
            zip.addBytes("OPS/cover.png", byteArrayOf(1, 2, 3, 4))
        }
    }

    private fun ZipOutputStream.add(
        path: String,
        content: String,
    ) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.addBytes(
        path: String,
        content: ByteArray,
    ) {
        putNextEntry(ZipEntry(path))
        write(content)
        closeEntry()
    }
}
