package com.octomind.booksreader.data

import com.octomind.booksreader.domain.ComprehensionAnswer
import com.octomind.booksreader.domain.ComprehensionAssessment
import com.octomind.booksreader.domain.ComprehensionConfidence
import com.octomind.booksreader.domain.ComprehensionQuestion
import com.octomind.booksreader.domain.ComprehensionQuestionType
import com.octomind.booksreader.domain.ComprehensionRating
import com.octomind.booksreader.domain.ComprehensionRubric
import org.json.JSONArray
import org.json.JSONObject

internal object ComprehensionJsonCodec {
    fun encode(assessments: List<ComprehensionAssessment>): String {
        assessments.forEach(ComprehensionAssessment::validateOwnership)
        val items = JSONArray().apply { assessments.forEach { put(it.toJson()) } }
        return JSONObject().put("schemaVersion", SCHEMA_VERSION).put("assessments", items).toString()
    }

    fun decode(value: String): List<ComprehensionAssessment> {
        val root = JSONObject(value)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) {
            "Versión de comprensión no compatible"
        }
        val items = root.getJSONArray("assessments")
        return List(items.length()) { index ->
            items.getJSONObject(index).toAssessment().also(ComprehensionAssessment::validateOwnership)
        }
    }

    private const val SCHEMA_VERSION = 1
}

private fun ComprehensionAssessment.toJson() =
    JSONObject()
        .put("id", id)
        .put("userId", userId)
        .put("sessionId", sessionId)
        .put("bookId", bookId)
        .put("bookTitle", bookTitle)
        .put("sourceStartCharacterOffset", sourceStartCharacterOffset)
        .put("sourceEndCharacterOffset", sourceEndCharacterOffset)
        .put("questions", JSONArray().apply { questions.forEach { put(it.toJson()) } })
        .put("answers", JSONArray().apply { answers.forEach { put(it.toJson()) } })
        .put("status", status.name)
        .put("createdAtMillis", createdAtMillis)
        .put("completedAtMillis", completedAtMillis ?: JSONObject.NULL)

private fun ComprehensionQuestion.toJson() =
    JSONObject()
        .put("id", id)
        .put("userId", userId)
        .put("bookId", bookId)
        .put("type", type.name)
        .put("prompt", prompt)
        .put("expectedAnswer", expectedAnswer)
        .put("rubric", rubric.toJson())
        .put("evidenceStartCharacterOffset", evidenceStartCharacterOffset)
        .put("evidenceEndCharacterOffset", evidenceEndCharacterOffset)

private fun ComprehensionRubric.toJson() =
    JSONObject()
        .put("fullCredit", fullCredit)
        .put("partialCredit", partialCredit)
        .put("noCredit", noCredit)

private fun ComprehensionAnswer.toJson() =
    JSONObject()
        .put("questionId", questionId)
        .put("responseText", responseText)
        .put("confidence", confidence.name)
        .put("rating", rating.name)
        .put("answeredAtMillis", answeredAtMillis)

private fun JSONObject.toAssessment(): ComprehensionAssessment =
    ComprehensionAssessment(
        id = getString("id"),
        userId = getString("userId"),
        sessionId = getString("sessionId"),
        bookId = getString("bookId"),
        bookTitle = getString("bookTitle"),
        sourceStartCharacterOffset = getInt("sourceStartCharacterOffset"),
        sourceEndCharacterOffset = getInt("sourceEndCharacterOffset"),
        questions = getJSONArray("questions").objects(JSONObject::toQuestion),
        answers = getJSONArray("answers").objects(JSONObject::toAnswer),
        status = enumValueOf(getString("status")),
        createdAtMillis = getLong("createdAtMillis"),
        completedAtMillis = optLongOrNull("completedAtMillis"),
    )

private fun JSONObject.toQuestion(): ComprehensionQuestion =
    ComprehensionQuestion(
        id = getString("id"),
        userId = getString("userId"),
        bookId = getString("bookId"),
        type = enumValueOf<ComprehensionQuestionType>(getString("type")),
        prompt = getString("prompt"),
        expectedAnswer = getString("expectedAnswer"),
        rubric = getJSONObject("rubric").toRubric(),
        evidenceStartCharacterOffset = getInt("evidenceStartCharacterOffset"),
        evidenceEndCharacterOffset = getInt("evidenceEndCharacterOffset"),
    )

private fun JSONObject.toRubric(): ComprehensionRubric =
    ComprehensionRubric(
        fullCredit = getString("fullCredit"),
        partialCredit = getString("partialCredit"),
        noCredit = getString("noCredit"),
    )

private fun JSONObject.toAnswer(): ComprehensionAnswer =
    ComprehensionAnswer(
        questionId = getString("questionId"),
        responseText = getString("responseText"),
        confidence = enumValueOf<ComprehensionConfidence>(getString("confidence")),
        rating = enumValueOf<ComprehensionRating>(getString("rating")),
        answeredAtMillis = getLong("answeredAtMillis"),
    )

private fun <T> JSONArray.objects(transform: (JSONObject) -> T): List<T> {
    val itemCount = length()
    return List(itemCount) { index -> transform(getJSONObject(index)) }
}

private fun JSONObject.optLongOrNull(name: String): Long? = if (isNull(name) || !has(name)) null else getLong(name)
