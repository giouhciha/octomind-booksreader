package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensionTest {
    @Test
    fun `local provider creates grounded questions with stable ownership`() {
        var nextId = 0
        val text =
            "Elena llegó a la estación antes del amanecer. Encontró una carta bajo el reloj. " +
                "Al leerla comprendió que su hermano seguía vivo y decidió buscarlo esa misma mañana."

        val assessment =
            LocalRecallQuestionProvider { "id-${nextId++}" }.createAssessment(
                ComprehensionAssessmentRequest(
                    userId = LOCAL_READER_ID,
                    sessionId = "session-1",
                    bookId = "book-1",
                    bookTitle = "Historia sintética",
                    text = text,
                    startCharacterOffset = 0,
                    endCharacterOffset = text.length,
                    catalog = catalog(),
                    createdAtMillis = 10,
                ),
            ) ?: error("Expected an assessment")

        assertEquals(4, assessment.questions.size)
        assertEquals(2, assessment.questions.count { it.type == ComprehensionQuestionType.LITERAL })
        assertTrue(assessment.questions.any { it.type == ComprehensionQuestionType.MAIN_IDEA })
        assertTrue(assessment.questions.any { it.type == ComprehensionQuestionType.INFERENCE })
        assessment.questions.forEach { question ->
            assertEquals(LOCAL_READER_ID, question.userId)
            assertEquals("book-1", question.bookId)
            assertTrue(question.evidenceStartCharacterOffset >= 0)
            assertTrue(question.evidenceEndCharacterOffset <= text.length)
            assertTrue(question.rubric.fullCredit.isNotBlank())
            assertTrue(question.rubric.partialCredit.isNotBlank())
            assertTrue(question.rubric.noCredit.isNotBlank())
            val evidence =
                text.substring(
                    question.evidenceStartCharacterOffset,
                    question.evidenceEndCharacterOffset,
                ).trim()
            assertTrue(question.expectedAnswer.contains(evidence))
        }
    }

    @Test
    fun `assessment completes only after every question has an answer`() {
        val questions =
            listOf(
                question("first", ComprehensionQuestionType.LITERAL),
                question("second", ComprehensionQuestionType.MAIN_IDEA),
            )
        val firstAnswer = answer("first", ComprehensionRating.UNDERSTOOD)
        val secondAnswer = answer("second", ComprehensionRating.PARTIAL)

        val inProgress = assessment(questions).answer(firstAnswer)
        val completed = inProgress.answer(secondAnswer)

        assertEquals(ComprehensionAssessmentStatus.IN_PROGRESS, inProgress.status)
        assertEquals(ComprehensionAssessmentStatus.COMPLETED, completed.status)
        assertEquals(2, completed.answers.size)
    }

    @Test
    fun `provider refuses a session without enough readable content`() {
        val assessment =
            LocalRecallQuestionProvider { "id" }.createAssessment(
                ComprehensionAssessmentRequest(
                    userId = LOCAL_READER_ID,
                    sessionId = "session",
                    bookId = "book",
                    bookTitle = "Breve",
                    text = "Texto corto.",
                    startCharacterOffset = 0,
                    endCharacterOffset = 12,
                    catalog = catalog(),
                    createdAtMillis = 0,
                ),
            )

        assertNull(assessment)
    }

    @Test
    fun `weighted score respects each comprehension dimension`() {
        val assessment = completedAssessment(listOf(100, 100, 0, 0))

        assertEquals(40, ComprehensionScorer.score(assessment))
        assertEquals(100.0, ComprehensionScorer.categoryScore(assessment, ComprehensionQuestionType.LITERAL), 0.0)
    }

    @Test
    fun `partial answers produce a balanced score`() {
        assertEquals(50, ComprehensionScorer.score(completedAssessment(listOf(50, 50, 50, 50))))
    }

    @Test
    fun `accumulated estimate becomes stable after three completed sessions`() {
        val completed = completedAssessment(listOf(100, 100, 100, 100))
        val unfinished = completed.copy(id = "unfinished", status = ComprehensionAssessmentStatus.IN_PROGRESS)

        assertEquals(100, ComprehensionScorer.accumulatedScore(listOf(completed, unfinished)))
        assertFalse(ComprehensionScorer.isStable(listOf(completed, completed.copy(id = "second"))))
        assertTrue(
            ComprehensionScorer.isStable(
                listOf(completed, completed.copy(id = "second"), completed.copy(id = "third")),
            ),
        )
    }

    private fun completedAssessment(scores: List<Int>): ComprehensionAssessment {
        val types =
            listOf(
                ComprehensionQuestionType.LITERAL,
                ComprehensionQuestionType.LITERAL,
                ComprehensionQuestionType.MAIN_IDEA,
                ComprehensionQuestionType.INFERENCE,
            )
        val questions = types.mapIndexed { index, type -> question("question-$index", type) }
        val ratings = scores.map(::ratingFor)
        val answers =
            questions.mapIndexed { index, question ->
                ComprehensionAnswer(
                    questionId = question.id,
                    responseText = "Respuesta sintética",
                    confidence = ComprehensionConfidence.MEDIUM,
                    rating = ratings[index],
                    answeredAtMillis = index.toLong(),
                )
            }
        return assessment(questions).copy(
            answers = answers,
            status = ComprehensionAssessmentStatus.COMPLETED,
            completedAtMillis = 10,
        )
    }

    private fun assessment(questions: List<ComprehensionQuestion>) =
        ComprehensionAssessment(
            id = "assessment",
            userId = LOCAL_READER_ID,
            sessionId = "session",
            bookId = "book",
            bookTitle = "Libro sintético",
            sourceStartCharacterOffset = 0,
            sourceEndCharacterOffset = 100,
            questions = questions,
            createdAtMillis = 0,
        )

    private fun question(
        id: String,
        type: ComprehensionQuestionType,
    ) = ComprehensionQuestion(
        id = id,
        userId = LOCAL_READER_ID,
        bookId = "book",
        type = type,
        prompt = "Pregunta",
        expectedAnswer = "Respuesta esperada",
        rubric = ComprehensionRubric("Completa", "Parcial", "Incorrecta"),
        evidenceStartCharacterOffset = 0,
        evidenceEndCharacterOffset = 100,
    )

    private fun ratingFor(score: Int) = ComprehensionRating.entries.first { it.score == score }

    private fun answer(
        questionId: String,
        rating: ComprehensionRating,
    ) = ComprehensionAnswer(
        questionId = questionId,
        responseText = "Respuesta sintética",
        confidence = ComprehensionConfidence.MEDIUM,
        rating = rating,
        answeredAtMillis = 10,
    )

    private fun catalog() =
        ComprehensionQuestionCatalog(
            firstLiteralPrompt = "Hecho inicial",
            secondLiteralPrompt = "Hecho final",
            mainIdeaPrompt = "Idea principal",
            inferencePrompt = "Inferencia",
            literalExpectedAnswer = "Hecho fiel",
            mainIdeaExpectedAnswer = "Síntesis fiel",
            inferenceExpectedAnswer = "Conclusión apoyada",
            rubric = ComprehensionRubric("Completa", "Parcial", "Incorrecta"),
        )
}
