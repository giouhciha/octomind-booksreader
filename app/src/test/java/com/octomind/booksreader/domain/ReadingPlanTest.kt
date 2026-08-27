package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPlanTest {
    @Test
    fun `build keeps stable character ranges and paragraphs`() {
        val text = "Uno dos tres cuatro. Cinco seis.\n\nSegundo párrafo aquí."

        val plan = ReadingPlanBuilder.build(text, wordsPerBlock = 3)

        assertEquals(2, plan.paragraphs.size)
        assertEquals("Uno dos tres", plan.blocks[0].text)
        assertEquals("cuatro.", plan.blocks[1].text)
        assertEquals("Segundo párrafo aquí.", plan.blocks.last().text)
        plan.blocks.forEach { block ->
            assertEquals(block.text, text.substring(block.startCharacterOffset, block.endCharacterOffset))
        }
    }

    @Test
    fun `sentence ending creates a longer pause`() {
        val plan = ReadingPlanBuilder.build("Primera frase. Después continúa, con calma", wordsPerBlock = 2)
        val sentence = plan.blocks.first()
        val comma = plan.blocks.first { it.text.endsWith(',') }

        assertTrue(sentence.pauseAfterMillis > comma.pauseAfterMillis)
    }

    @Test
    fun `comma closes a meaning group before the word limit`() {
        val plan = ReadingPlanBuilder.build(
            "Respira profundo, después continúa con calma.",
            wordsPerBlock = 6,
        )

        assertEquals("Respira profundo,", plan.blocks.first().text)
        assertEquals(2, plan.blocks.first().wordCount)
        assertTrue(plan.blocks.first().pauseAfterMillis > 0L)
    }

    @Test
    fun `closing quotation mark keeps the sentence pause`() {
        val plan = ReadingPlanBuilder.build(
            "Ella respondió: «Ahora estoy lista.» Luego avanzó.",
            wordsPerBlock = 8,
        )

        val quotedSentence = plan.blocks.first { it.text.endsWith("lista.»") }
        val colon = plan.blocks.first { it.text.endsWith(':') }
        assertTrue(quotedSentence.pauseAfterMillis > colon.pauseAfterMillis)
    }

    @Test
    fun `dialogue dashes create natural groups`() {
        val plan = ReadingPlanBuilder.build(
            "—No estoy segura —dijo Elena—, pero continuaré.",
            wordsPerBlock = 8,
        )

        assertEquals(
            listOf("—No estoy segura", "—dijo Elena—,", "pero continuaré."),
            plan.blocks.map(ReadingBlock::text),
        )
        assertTrue(plan.blocks[0].pauseAfterMillis > 0L)
        assertEquals(340L, plan.blocks[1].pauseAfterMillis)
    }

    @Test
    fun `narrator aside keeps a short pause before brackets`() {
        val plan = ReadingPlanBuilder.build(
            "[—dijo Elena—] y continuó.",
            wordsPerBlock = 8,
        )

        val aside = plan.blocks.first()
        assertEquals("[—dijo Elena—]", aside.text)
        assertEquals(340L, aside.pauseAfterMillis)
        assertEquals(540L, PacingCalculator.durationMillis(aside, wordsPerMinute = 600))
    }

    @Test
    fun `closing dialogue dash takes priority over a comma`() {
        val plan = ReadingPlanBuilder.build(
            "—No estoy segura —dijo Elena—, pero continuaré.",
            wordsPerBlock = 8,
        )

        val narratorAside = plan.blocks.first { it.text == "—dijo Elena—," }
        assertEquals(340L, narratorAside.pauseAfterMillis)
    }

    @Test
    fun `long attached aside from epub pauses at its closing dash`() {
        val fragment = "—esa última gota que me hizo correr fuera de la solitaria granja " +
            "Akeley y a través de las salvajes colinas abovedadas de Vermont en un " +
            "automóvil secuestrado de noche—"

        val plan = ReadingPlanBuilder.build(fragment, wordsPerBlock = 4)
        val closingBlock = plan.blocks.last()

        assertEquals("secuestrado de noche—", closingBlock.text)
        assertEquals(3, closingBlock.wordCount)
        assertEquals(660L, closingBlock.pauseAfterMillis)
    }

    @Test
    fun `invisible epub mark after a closing dash does not hide its pause`() {
        val plan = ReadingPlanBuilder.build(
            "Una aclaración termina aquí—\u200B y el texto continúa.",
            wordsPerBlock = 8,
        )

        val closingBlock = plan.blocks.first()
        assertEquals("Una aclaración termina aquí—\u200B", closingBlock.text)
        assertEquals(340L, closingBlock.pauseAfterMillis)
    }

    @Test
    fun `horizontal bar variant also creates a pause`() {
        val plan = ReadingPlanBuilder.build(
            "Una aclaración termina aquí― y el texto continúa.",
            wordsPerBlock = 8,
        )

        assertEquals(340L, plan.blocks.first().pauseAfterMillis)
    }

    @Test
    fun `spaced en dashes keep an explanation in its own group`() {
        val plan = ReadingPlanBuilder.build(
            "La lectura – una práctica consciente – mejora la comprensión.",
            wordsPerBlock = 8,
        )

        assertEquals(
            listOf("La lectura", "– una práctica consciente –", "mejora la comprensión."),
            plan.blocks.map(ReadingBlock::text),
        )
        assertEquals(listOf(2, 3, 3), plan.blocks.map(ReadingBlock::wordCount))
        assertTrue(plan.blocks[0].pauseAfterMillis > 0L)
        assertTrue(plan.blocks[1].pauseAfterMillis > 0L)
    }

    @Test
    fun `spaced ascii hyphens can delimit an explanation`() {
        val plan = ReadingPlanBuilder.build(
            "La práctica - cuando es constante - produce resultados.",
            wordsPerBlock = 8,
        )

        assertEquals(
            listOf("La práctica", "- cuando es constante -", "produce resultados."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `standalone leading dash stays with dialogue`() {
        val plan = ReadingPlanBuilder.build(
            "— Hola, ¿cómo estás?",
            wordsPerBlock = 8,
        )

        assertEquals(listOf("— Hola,", "¿cómo estás?"), plan.blocks.map(ReadingBlock::text))
        assertEquals(1, plan.blocks.first().wordCount)
    }

    @Test
    fun `hyphenated words do not create false pauses`() {
        val plan = ReadingPlanBuilder.build(
            "Un enfoque teórico-práctico mejora la lectura veloz",
            wordsPerBlock = 8,
        )

        assertEquals(1, plan.blocks.size)
        assertEquals("Un enfoque teórico-práctico mejora la lectura veloz", plan.blocks.single().text)
    }

    @Test
    fun `duration responds to speed without dropping punctuation pause`() {
        val block = ReadingPlanBuilder.build("Leer con atención.", wordsPerBlock = 3).blocks.single()

        val slow = PacingCalculator.durationMillis(block, wordsPerMinute = 180)
        val fast = PacingCalculator.durationMillis(block, wordsPerMinute = 360)

        assertTrue(slow > fast)
        assertTrue(fast >= block.pauseAfterMillis)
    }

    @Test
    fun `restores the block containing a saved character offset`() {
        val text = "uno dos tres cuatro cinco seis"
        val plan = ReadingPlanBuilder.build(text, wordsPerBlock = 2)
        val target = text.indexOf("cuatro")

        val index = plan.blockIndexFor(target)

        assertEquals("tres cuatro", plan.blocks[index].text)
    }

    @Test
    fun `punctuation mode keeps complete units of meaning`() {
        val plan = ReadingPlanBuilder.build(
            text = "El lector avanza con calma, comprende la idea completa. Después continúa: sin perderse",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(
            listOf(
                "El lector avanza con calma,",
                "comprende la idea completa.",
                "Después continúa:",
                "sin perderse",
            ),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `punctuation mode does not split an unpunctuated paragraph`() {
        val plan = ReadingPlanBuilder.build(
            text = "Una unidad completa conserva todas sus palabras",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(1, plan.blocks.size)
        assertEquals("Una unidad completa conserva todas sus palabras", plan.blocks.single().text)
    }

    @Test
    fun `punctuation mode keeps compact initialisms with their reference`() {
        val plan = ReadingPlanBuilder.build(
            text = "R.F.D. #2, Townshend, Windham Co., Vermont.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(
            listOf("R.F.D. #2,", "Townshend,", "Windham Co.,", "Vermont."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `punctuation mode keeps spaced author initials together`() {
        val plan = ReadingPlanBuilder.build(
            text = "Henry W. Akeley\nH. W. A.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(1, plan.blocks.size)
        assertEquals("Henry W. Akeley\nH. W. A.", plan.blocks.single().text)
    }

    @Test
    fun `punctuation mode keeps initials treatments and street abbreviations natural`() {
        val plan = ReadingPlanBuilder.build(
            text = "Albert N. Wilmarth, Esq.,\n118 Saltonstall St.,\nArkham, Mass.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(
            listOf("Albert N. Wilmarth,", "Esq.,", "118 Saltonstall St.,", "Arkham,", "Mass."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `postscript abbreviation stays with its message`() {
        val plan = ReadingPlanBuilder.build(
            text = "P.D. Estoy haciendo algunas impresiones adicionales, que enviaré pronto.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(
            listOf("P.D. Estoy haciendo algunas impresiones adicionales,", "que enviaré pronto."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `date without terminal punctuation remains one fragment`() {
        val plan = ReadingPlanBuilder.build(
            text = "5 de mayo de 1928",
            wordsPerBlock = 1,
            readingMode = ReadingMode.PUNCTUATION,
        )

        assertEquals(listOf("5 de mayo de 1928"), plan.blocks.map(ReadingBlock::text))
    }

    @Test
    fun `sentence mode ignores soft punctuation`() {
        val plan = ReadingPlanBuilder.build(
            text = "Respira profundo, conserva la idea; después continúa: sin perder el ritmo.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.SENTENCE,
        )

        assertEquals(
            listOf("Respira profundo, conserva la idea; después continúa: sin perder el ritmo."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `sentence mode keeps dialogue dashes inside the complete sentence`() {
        val plan = ReadingPlanBuilder.build(
            text = "—No estoy segura —dijo Elena—, pero continuaré. Después respiró.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.SENTENCE,
        )

        assertEquals(
            listOf("—No estoy segura —dijo Elena—, pero continuaré.", "Después respiró."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `sentence mode preserves abbreviations until the actual period`() {
        val plan = ReadingPlanBuilder.build(
            text = "P.D. El autor H. P. Lovecraft escribió desde Mass. Luego continuó.",
            wordsPerBlock = 1,
            readingMode = ReadingMode.SENTENCE,
        )

        assertEquals(
            listOf("P.D. El autor H. P. Lovecraft escribió desde Mass.", "Luego continuó."),
            plan.blocks.map(ReadingBlock::text),
        )
    }

    @Test
    fun `sentence mode ends at question exclamation and paragraph boundary`() {
        val plan = ReadingPlanBuilder.build(
            text = "¿Estás listo? Sí, avancemos! Una línea sin punto\n\nOtro párrafo",
            wordsPerBlock = 1,
            readingMode = ReadingMode.SENTENCE,
        )

        assertEquals(
            listOf("¿Estás listo?", "Sí, avancemos!", "Una línea sin punto", "Otro párrafo"),
            plan.blocks.map(ReadingBlock::text),
        )
    }
}
