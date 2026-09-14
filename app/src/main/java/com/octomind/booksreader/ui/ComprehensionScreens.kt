package com.octomind.booksreader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octomind.booksreader.R
import com.octomind.booksreader.domain.ComprehensionConfidence
import com.octomind.booksreader.domain.ComprehensionQuestion
import com.octomind.booksreader.domain.ComprehensionQuestionType
import com.octomind.booksreader.domain.ComprehensionRating

internal val ComprehensionBackground = Color(0xFF8A551F)
internal val ComprehensionSurface = Color(0xFFF9E8C5)
internal val ComprehensionText = Color(0xFF352817)
internal val ComprehensionSecondaryText = Color(0xFF6C5B43)
internal val ComprehensionShape = RoundedCornerShape(24.dp)

@Composable
internal fun ComprehensionCheckScreen(
    state: ComprehensionCheckState,
    onBack: () -> Unit,
    onResponseChange: (String) -> Unit,
    onConfidenceChange: (ComprehensionConfidence) -> Unit,
    onRevealEvidence: () -> Unit,
    onRate: (ComprehensionRating) -> Unit,
) {
    BackHandler(onBack = onBack)
    val question = state.assessment.questions[state.currentQuestionIndex]
    Scaffold(
        topBar = { ComprehensionTopBar(stringResource(R.string.comprehension_check), onBack) },
        containerColor = ComprehensionBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { QuestionProgress(state) }
            item { QuestionCard(question) }
            item {
                Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
                    OutlinedTextField(
                        value = state.responseText,
                        onValueChange = onResponseChange,
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        minLines = 4,
                        label = { Text(stringResource(R.string.comprehension_response_hint)) },
                    )
                }
            }
            item { ConfidenceSelector(state.confidence, onConfidenceChange) }
            item {
                EvidenceActions(
                    evidenceVisible = state.evidenceVisible,
                    onRevealEvidence = onRevealEvidence,
                    onNoRecall = { onRate(ComprehensionRating.NOT_YET) },
                )
            }
            if (state.evidenceVisible) {
                item { EvidenceCard(state, question) }
                item { RatingSelector(onRate) }
            }
        }
    }
}

@Composable
private fun QuestionProgress(state: ComprehensionCheckState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(
                R.string.comprehension_question_progress,
                state.currentQuestionIndex + 1,
                state.assessment.questions.size,
            ),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = { (state.currentQuestionIndex + 1f) / state.assessment.questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuestionCard(question: ComprehensionQuestion) {
    Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(question.type.localizedName(), color = Color(0xFF215A3D), fontWeight = FontWeight.Bold)
            Text(question.prompt, style = MaterialTheme.typography.headlineSmall, color = ComprehensionText)
            Text(stringResource(R.string.comprehension_recall_first), color = ComprehensionSecondaryText)
        }
    }
}

@Composable
private fun ConfidenceSelector(
    selected: ComprehensionConfidence,
    onSelected: (ComprehensionConfidence) -> Unit,
) {
    Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.comprehension_confidence), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComprehensionConfidence.entries.forEach { confidence ->
                    FilterChip(
                        selected = selected == confidence,
                        onClick = { onSelected(confidence) },
                        label = { Text(confidence.localizedName()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceActions(
    evidenceVisible: Boolean,
    onRevealEvidence: () -> Unit,
    onNoRecall: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onRevealEvidence,
            enabled = !evidenceVisible,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.comprehension_reveal_evidence))
        }
        TextButton(onClick = onNoRecall, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.comprehension_no_recall))
        }
    }
}

@Composable
private fun EvidenceCard(
    state: ComprehensionCheckState,
    question: ComprehensionQuestion,
) {
    val excerpt =
        state.document.text.substring(
            question.evidenceStartCharacterOffset.coerceIn(0, state.document.text.length),
            question.evidenceEndCharacterOffset.coerceIn(0, state.document.text.length),
        )
    Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.comprehension_source_evidence), fontWeight = FontWeight.Bold)
            Text(excerpt, color = ComprehensionText)
            Text(stringResource(R.string.comprehension_expected_guidance), fontWeight = FontWeight.Bold)
            Text(question.expectedAnswer, color = ComprehensionSecondaryText)
            Text(question.rubric.fullCredit, color = ComprehensionSecondaryText)
            Text(question.rubric.partialCredit, color = ComprehensionSecondaryText)
            Text(question.rubric.noCredit, color = ComprehensionSecondaryText)
        }
    }
}

@Composable
private fun RatingSelector(onRate: (ComprehensionRating) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.comprehension_self_rate), color = Color.White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComprehensionRating.entries.forEach { rating ->
                OutlinedButton(onClick = { onRate(rating) }, modifier = Modifier.weight(1f)) {
                    Text(rating.localizedName())
                }
            }
        }
    }
}

@Composable
internal fun ComprehensionTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Surface(color = Color(0xFFF3D293), shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.return_session))
            }
            Text(
                title,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun ComprehensionQuestionType.localizedName(): String =
    stringResource(
        when (this) {
            ComprehensionQuestionType.LITERAL -> R.string.comprehension_literal
            ComprehensionQuestionType.MAIN_IDEA -> R.string.comprehension_main_idea
            ComprehensionQuestionType.INFERENCE -> R.string.comprehension_inference
        },
    )

@Composable
private fun ComprehensionConfidence.localizedName(): String =
    stringResource(
        when (this) {
            ComprehensionConfidence.LOW -> R.string.comprehension_confidence_low
            ComprehensionConfidence.MEDIUM -> R.string.comprehension_confidence_medium
            ComprehensionConfidence.HIGH -> R.string.comprehension_confidence_high
        },
    )

@Composable
private fun ComprehensionRating.localizedName(): String =
    stringResource(
        when (this) {
            ComprehensionRating.NOT_YET -> R.string.comprehension_rating_not_yet
            ComprehensionRating.PARTIAL -> R.string.comprehension_rating_partial
            ComprehensionRating.UNDERSTOOD -> R.string.comprehension_rating_understood
        },
    )
