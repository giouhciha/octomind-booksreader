package com.octomind.booksreader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octomind.booksreader.R
import com.octomind.booksreader.domain.ComprehensionAssessment
import com.octomind.booksreader.domain.ComprehensionAssessmentStatus
import com.octomind.booksreader.domain.ComprehensionQuestionType
import com.octomind.booksreader.domain.ComprehensionScorer

@Composable
internal fun ComprehensionResultScreen(
    latestAssessment: ComprehensionAssessment,
    assessments: List<ComprehensionAssessment>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val completed = assessments.filter { it.status == ComprehensionAssessmentStatus.COMPLETED }
    Scaffold(
        topBar = { ComprehensionTopBar(stringResource(R.string.comprehension_result_title), onBack) },
        containerColor = ComprehensionBackground,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScoreSummary(latestAssessment, completed) }
            item { CategoryScores(latestAssessment) }
            item {
                Text(
                    stringResource(R.string.comprehension_history),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            itemsIndexed(completed) { index, assessment ->
                AssessmentHistoryCard(
                    number = completed.size - index,
                    assessment = assessment,
                )
            }
        }
    }
}

@Composable
private fun ScoreSummary(
    latest: ComprehensionAssessment,
    completed: List<ComprehensionAssessment>,
) {
    Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(latest.bookTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.comprehension_session_score, ComprehensionScorer.score(latest)),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF215A3D),
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    R.string.comprehension_accumulated_score,
                    ComprehensionScorer.accumulatedScore(completed),
                ),
                color = ComprehensionText,
            )
            Text(
                if (ComprehensionScorer.isStable(completed)) {
                    stringResource(R.string.comprehension_stable_estimate)
                } else {
                    stringResource(R.string.comprehension_initial_estimate, completed.size)
                },
                color = ComprehensionSecondaryText,
            )
        }
    }
}

@Composable
private fun CategoryScores(assessment: ComprehensionAssessment) {
    Surface(shape = ComprehensionShape, color = ComprehensionSurface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.comprehension_dimensions), fontWeight = FontWeight.Bold)
            ComprehensionQuestionType.entries.forEach { type ->
                val score = ComprehensionScorer.categoryScore(assessment, type).toInt()
                Text(stringResource(R.string.comprehension_category_score, type.localizedName(), score))
            }
        }
    }
}

@Composable
private fun AssessmentHistoryCard(
    number: Int,
    assessment: ComprehensionAssessment,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = ComprehensionSurface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.comprehension_history_session, number), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.percentage_value, ComprehensionScorer.score(assessment)))
        }
    }
}
