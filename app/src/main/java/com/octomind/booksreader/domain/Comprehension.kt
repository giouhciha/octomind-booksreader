package com.octomind.booksreader.domain

import java.util.UUID
import kotlin.math.roundToInt

const val LOCAL_READER_ID = "local-reader"
private const val PARTIAL_COMPREHENSION_SCORE = 50
private const val FULL_COMPREHENSION_SCORE = 100

enum class ComprehensionQuestionType {
    LITERAL,
    MAIN_IDEA,
    INFERENCE,
}

enum class ComprehensionConfidence {
    LOW,
    MEDIUM,
    HIGH,
}

enum class ComprehensionRating(
    val score: Int,
) {
    NOT_YET(0),
    PARTIAL(PARTIAL_COMPREHENSION_SCORE),
    UNDERSTOOD(FULL_COMPREHENSION_SCORE),
}

enum class ComprehensionAssessmentStatus {
    IN_PROGRESS,
    COMPLETED,
}

data class ComprehensionRubric(
    val fullCredit: String,
    val partialCredit: String,
    val noCredit: String,
)

data class ComprehensionQuestion(
    val id: String,
    val userId: String,
    val bookId: String,
    val type: ComprehensionQuestionType,
    val prompt: String,
    val expectedAnswer: String,
    val rubric: ComprehensionRubric,
    val evidenceStartCharacterOffset: Int,
    val evidenceEndCharacterOffset: Int,
)

data class ComprehensionAnswer(
    val questionId: String,
    val responseText: String,
    val confidence: ComprehensionConfidence,
    val rating: ComprehensionRating,
    val answeredAtMillis: Long,
)

data class ComprehensionAssessment(
    val id: String,
    val userId: String,
    val sessionId: String,
    val bookId: String,
    val bookTitle: String,
    val sourceStartCharacterOffset: Int,
    val sourceEndCharacterOffset: Int,
    val questions: List<ComprehensionQuestion>,
    val answers: List<ComprehensionAnswer> = emptyList(),
    val status: ComprehensionAssessmentStatus = ComprehensionAssessmentStatus.IN_PROGRESS,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,
) {
    fun answer(answer: ComprehensionAnswer): ComprehensionAssessment {
        require(questions.any { it.id == answer.questionId }) { "La respuesta no pertenece a esta evaluación" }
        val updatedAnswers = answers.filterNot { it.questionId == answer.questionId } + answer
        val completed = questions.all { question -> updatedAnswers.any { it.questionId == question.id } }
        return copy(
            answers = updatedAnswers,
            status =
                if (completed) {
                    ComprehensionAssessmentStatus.COMPLETED
                } else {
                    ComprehensionAssessmentStatus.IN_PROGRESS
                },
            completedAtMillis = if (completed) answer.answeredAtMillis else null,
        )
    }
}

data class ComprehensionQuestionCatalog(
    val firstLiteralPrompt: String,
    val secondLiteralPrompt: String,
    val mainIdeaPrompt: String,
    val inferencePrompt: String,
    val literalExpectedAnswer: String,
    val mainIdeaExpectedAnswer: String,
    val inferenceExpectedAnswer: String,
    val rubric: ComprehensionRubric,
)

data class ComprehensionAssessmentRequest(
    val userId: String,
    val sessionId: String,
    val bookId: String,
    val bookTitle: String,
    val text: String,
    val startCharacterOffset: Int,
    val endCharacterOffset: Int,
    val catalog: ComprehensionQuestionCatalog,
    val createdAtMillis: Long,
)

interface ComprehensionQuestionProvider {
    fun createAssessment(request: ComprehensionAssessmentRequest): ComprehensionAssessment?
}

class LocalRecallQuestionProvider(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ComprehensionQuestionProvider {
    override fun createAssessment(request: ComprehensionAssessmentRequest): ComprehensionAssessment? {
        val safeStart = request.startCharacterOffset.coerceIn(0, request.text.length)
        val safeEnd = request.endCharacterOffset.coerceIn(safeStart, request.text.length)
        val readableCharacters = request.text.substring(safeStart, safeEnd).count { !it.isWhitespace() }
        if (readableCharacters < MINIMUM_READABLE_CHARACTERS) return null

        val midpoint = findEvidenceBoundary(request.text, safeStart, safeEnd)
        val firstEvidence = boundedEvidence(safeStart, midpoint, preferEnd = false)
        val secondEvidence = boundedEvidence(midpoint, safeEnd, preferEnd = true)
        val centralEvidence = centeredEvidence(safeStart, safeEnd)
        val finalEvidence = boundedEvidence(safeStart, safeEnd, preferEnd = true)
        val questionSpecs = questionSpecs(request, firstEvidence, secondEvidence, centralEvidence, finalEvidence)
        return ComprehensionAssessment(
            id = idFactory(),
            userId = request.userId,
            sessionId = request.sessionId,
            bookId = request.bookId,
            bookTitle = request.bookTitle,
            sourceStartCharacterOffset = safeStart,
            sourceEndCharacterOffset = safeEnd,
            questions = createQuestions(request, questionSpecs),
            createdAtMillis = request.createdAtMillis,
        )
    }

    private fun questionSpecs(
        request: ComprehensionAssessmentRequest,
        firstEvidence: EvidenceRange,
        secondEvidence: EvidenceRange,
        centralEvidence: EvidenceRange,
        finalEvidence: EvidenceRange,
    ): List<QuestionSpec> =
        listOf(
            QuestionSpec(
                ComprehensionQuestionType.LITERAL,
                request.catalog.firstLiteralPrompt,
                request.catalog.literalExpectedAnswer,
                firstEvidence,
            ),
            QuestionSpec(
                ComprehensionQuestionType.LITERAL,
                request.catalog.secondLiteralPrompt,
                request.catalog.literalExpectedAnswer,
                secondEvidence,
            ),
            QuestionSpec(
                ComprehensionQuestionType.MAIN_IDEA,
                request.catalog.mainIdeaPrompt,
                request.catalog.mainIdeaExpectedAnswer,
                centralEvidence,
            ),
            QuestionSpec(
                ComprehensionQuestionType.INFERENCE,
                request.catalog.inferencePrompt,
                request.catalog.inferenceExpectedAnswer,
                finalEvidence,
            ),
        )

    private fun createQuestions(
        request: ComprehensionAssessmentRequest,
        specs: List<QuestionSpec>,
    ): List<ComprehensionQuestion> =
        specs.map { spec ->
            ComprehensionQuestion(
                id = idFactory(),
                userId = request.userId,
                bookId = request.bookId,
                type = spec.type,
                prompt = spec.prompt,
                expectedAnswer =
                    spec.expectedAnswer +
                        "\n\n" +
                        request.text.substring(spec.evidence.start, spec.evidence.end).trim(),
                rubric = request.catalog.rubric,
                evidenceStartCharacterOffset = spec.evidence.start,
                evidenceEndCharacterOffset = spec.evidence.end,
            )
        }

    private fun findEvidenceBoundary(
        text: String,
        start: Int,
        end: Int,
    ): Int {
        val target = start + (end - start) / 2
        val followingBoundary = text.indexOfAny(EVIDENCE_BOUNDARIES, startIndex = target)
        return if (followingBoundary in (target + 1) until end) followingBoundary + 1 else target
    }

    private fun boundedEvidence(
        start: Int,
        end: Int,
        preferEnd: Boolean,
    ): EvidenceRange {
        if (end - start <= MAXIMUM_EVIDENCE_CHARACTERS) return EvidenceRange(start, end)
        return if (preferEnd) {
            EvidenceRange(end - MAXIMUM_EVIDENCE_CHARACTERS, end)
        } else {
            EvidenceRange(start, start + MAXIMUM_EVIDENCE_CHARACTERS)
        }
    }

    private fun centeredEvidence(
        start: Int,
        end: Int,
    ): EvidenceRange {
        if (end - start <= MAXIMUM_EVIDENCE_CHARACTERS) return EvidenceRange(start, end)
        val evidenceStart = ((start + end) / 2 - MAXIMUM_EVIDENCE_CHARACTERS / 2).coerceAtLeast(start)
        return EvidenceRange(evidenceStart, evidenceStart + MAXIMUM_EVIDENCE_CHARACTERS)
    }

    private data class QuestionSpec(
        val type: ComprehensionQuestionType,
        val prompt: String,
        val expectedAnswer: String,
        val evidence: EvidenceRange,
    )

    private data class EvidenceRange(
        val start: Int,
        val end: Int,
    )

    private companion object {
        const val MINIMUM_READABLE_CHARACTERS = 80
        const val MAXIMUM_EVIDENCE_CHARACTERS = 1_200
        val EVIDENCE_BOUNDARIES = charArrayOf('.', '?', '!', '\n')
    }
}

object ComprehensionScorer {
    fun score(assessment: ComprehensionAssessment): Int {
        if (assessment.questions.isEmpty()) return 0
        return TYPE_WEIGHTS.entries
            .sumOf { (type, weight) -> categoryScore(assessment, type) * weight / MAXIMUM_SCORE }
            .roundToInt()
            .coerceIn(0, MAXIMUM_SCORE)
    }

    fun categoryScore(
        assessment: ComprehensionAssessment,
        type: ComprehensionQuestionType,
    ): Double {
        val questionIds =
            assessment.questions
                .filter { it.type == type }
                .map { it.id }
                .toSet()
        val ratings =
            assessment.answers
                .filter { it.questionId in questionIds }
                .map { it.rating.score }
        return ratings.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    fun accumulatedScore(assessments: List<ComprehensionAssessment>): Int {
        val completed = assessments.filter { it.status == ComprehensionAssessmentStatus.COMPLETED }
        return completed
            .takeIf { it.isNotEmpty() }
            ?.map(::score)
            ?.average()
            ?.roundToInt()
            ?: 0
    }

    fun isStable(assessments: List<ComprehensionAssessment>): Boolean =
        assessments.count { it.status == ComprehensionAssessmentStatus.COMPLETED } >= STABLE_ASSESSMENT_COUNT

    private const val MAXIMUM_SCORE = FULL_COMPREHENSION_SCORE
    private const val STABLE_ASSESSMENT_COUNT = 3
    private val TYPE_WEIGHTS =
        mapOf(
            ComprehensionQuestionType.LITERAL to 40.0,
            ComprehensionQuestionType.MAIN_IDEA to 35.0,
            ComprehensionQuestionType.INFERENCE to 25.0,
        )
}
