package com.octomind.booksreader.data

import android.content.Context
import com.octomind.booksreader.domain.ComprehensionAnswer
import com.octomind.booksreader.domain.ComprehensionAssessment
import com.octomind.booksreader.domain.ComprehensionAssessmentStatus
import com.octomind.booksreader.domain.ComprehensionQuestion
import com.octomind.booksreader.domain.LOCAL_READER_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LocalComprehensionRepository(
    context: Context,
) {
    private val lock = Any()
    private val libraryDirectory = File(context.filesDir, LIBRARY_DIRECTORY).apply { mkdirs() }
    private val assessmentsFile = File(libraryDirectory, ASSESSMENTS_FILE)

    suspend fun listForBook(bookId: String): List<ComprehensionAssessment> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                readAll()
                    .filter { it.bookId == bookId && it.userId == LOCAL_READER_ID }
                    .sortedByDescending { it.createdAtMillis }
            }
        }

    suspend fun save(assessment: ComprehensionAssessment) =
        withContext(Dispatchers.IO) {
            assessment.validateOwnership()
            synchronized(lock) {
                val updated =
                    readAll().filterNot { stored -> stored.shouldBeReplacedBy(assessment) } + assessment
                writeAll(updated)
            }
        }

    suspend fun deleteForBook(bookId: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                writeAll(readAll().filterNot { it.bookId == bookId })
            }
        }

    private fun readAll(): List<ComprehensionAssessment> {
        if (!assessmentsFile.isFile) return emptyList()
        return runCatching {
            ComprehensionJsonCodec.decode(assessmentsFile.readText())
        }.getOrElse { cause ->
            throw IllegalStateException("El historial de comprensión local está dañado", cause)
        }
    }

    private fun writeAll(assessments: List<ComprehensionAssessment>) {
        libraryDirectory.mkdirs()
        val temporary = File(libraryDirectory, "$ASSESSMENTS_FILE.tmp")
        temporary.writeText(ComprehensionJsonCodec.encode(assessments))
        try {
            Files.move(
                temporary.toPath(),
                assessmentsFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: IOException) {
            Files.move(temporary.toPath(), assessmentsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val LIBRARY_DIRECTORY = "library"
        const val ASSESSMENTS_FILE = "comprehension.json"
    }
}

internal fun ComprehensionAssessment.validateOwnership() {
    require(userId == LOCAL_READER_ID) { "La evaluación no pertenece al lector local" }
    require(sourceStartCharacterOffset >= 0 && sourceEndCharacterOffset >= sourceStartCharacterOffset) {
        "La evaluación contiene una ubicación no válida"
    }
    require(questions.isNotEmpty()) { "La evaluación no contiene preguntas" }
    require(questions.all { it.userId == userId && it.bookId == bookId }) {
        "Las preguntas no pertenecen a la evaluación"
    }
    require(questions.all { it.isValidFor(this) }) { "Las preguntas no contienen evidencia o rúbricas válidas" }
    val questionIds = questions.map { it.id }.toSet()
    val answerQuestionIds = answers.map { it.questionId }
    require(questionIds.size == questions.size) { "La evaluación contiene preguntas duplicadas" }
    require(answerQuestionIds.toSet().size == answerQuestionIds.size) { "La evaluación contiene respuestas duplicadas" }
    require(answers.all { it.isValidFor(questionIds) }) { "Las respuestas no pertenecen a la evaluación" }
    val completedIsValid =
        status == ComprehensionAssessmentStatus.COMPLETED &&
            answers.size == questions.size &&
            completedAtMillis != null
    val inProgressIsValid =
        status == ComprehensionAssessmentStatus.IN_PROGRESS &&
            answers.size < questions.size &&
            completedAtMillis == null
    require(completedIsValid || inProgressIsValid) {
        "El estado de la evaluación no coincide con sus respuestas"
    }
}

private fun ComprehensionAssessment.shouldBeReplacedBy(updated: ComprehensionAssessment): Boolean {
    if (id == updated.id) return true
    if (updated.status != ComprehensionAssessmentStatus.IN_PROGRESS) return false
    return bookId == updated.bookId && status == ComprehensionAssessmentStatus.IN_PROGRESS
}

private fun ComprehensionQuestion.isValidFor(assessment: ComprehensionAssessment): Boolean {
    if (prompt.isBlank() || expectedAnswer.isBlank()) return false
    if (rubric.fullCredit.isBlank()) return false
    if (rubric.partialCredit.isBlank()) return false
    if (rubric.noCredit.isBlank()) return false
    if (evidenceStartCharacterOffset < assessment.sourceStartCharacterOffset) return false
    if (evidenceEndCharacterOffset > assessment.sourceEndCharacterOffset) return false
    return evidenceEndCharacterOffset >= evidenceStartCharacterOffset
}

private fun ComprehensionAnswer.isValidFor(questionIds: Set<String>): Boolean {
    if (questionId !in questionIds || responseText.isBlank()) return false
    return responseText.length <= MAXIMUM_RESPONSE_CHARACTERS
}

private const val MAXIMUM_RESPONSE_CHARACTERS = 4_000
