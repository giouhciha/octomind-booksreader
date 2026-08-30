package com.octomind.booksreader.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.octomind.booksreader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfOriginalReader(
    title: String,
    filePath: String,
    requestedPageIndex: Int,
    readingProgress: Float,
    onBack: () -> Unit,
    onAdaptedReading: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onFinishBook: () -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        TextButton(onClick = onAdaptedReading) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = null)
                            Text(stringResource(R.string.pdf_adapted_reading))
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                )
                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val targetWidth = with(LocalDensity.current) { maxWidth.roundToPx() }
            val pageState by produceState<PdfPageUiState>(
                initialValue = PdfPageUiState.Loading,
                filePath,
                requestedPageIndex,
                targetWidth,
            ) {
                value = PdfPageUiState.Loading
                value =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            PdfPageRenderer.render(filePath, requestedPageIndex, targetWidth)
                        }.fold(
                            onSuccess = { PdfPageUiState.Loaded(it) },
                            onFailure = { PdfPageUiState.Error },
                        )
                    }
            }

            when (val currentState = pageState) {
                PdfPageUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                PdfPageUiState.Error -> PdfRenderError(onAdaptedReading = onAdaptedReading)
                is PdfPageUiState.Loaded -> {
                    DisposableEffect(currentState.page.bitmap) {
                        onDispose { currentState.page.bitmap.recycle() }
                    }
                    PdfPage(
                        page = currentState.page,
                        onPageChanged = onPageChanged,
                        onFinishBook = onFinishBook,
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    page: RenderedPdfPage,
    onPageChanged: (Int) -> Unit,
    onFinishBook: () -> Unit,
) {
    fun changePage(delta: Int) {
        val target = (page.pageIndex + delta).coerceIn(0, page.pageCount - 1)
        if (target != page.pageIndex) onPageChanged(target)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = page.bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.pdf_page_description, page.pageIndex + 1),
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = PDF_PAGE_HORIZONTAL_PADDING_DP.dp,
                        top = PDF_PAGE_VERTICAL_PADDING_DP.dp,
                        end = PDF_PAGE_HORIZONTAL_PADDING_DP.dp,
                        bottom = PDF_PAGE_BOTTOM_CLEARANCE_DP.dp,
                    ).pointerInput(page.pageIndex, page.pageCount) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                horizontalDrag += amount
                            },
                            onDragEnd = {
                                when {
                                    horizontalDrag < -PDF_SWIPE_THRESHOLD_DP.dp.toPx() -> changePage(1)
                                    horizontalDrag > PDF_SWIPE_THRESHOLD_DP.dp.toPx() -> changePage(-1)
                                }
                            },
                        )
                    },
        )
        Surface(
            tonalElevation = PDF_NAVIGATION_ELEVATION_DP.dp,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = PDF_NAVIGATION_HORIZONTAL_PADDING_DP.dp,
                            vertical = PDF_PAGE_VERTICAL_PADDING_DP.dp,
                        ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { changePage(-1) },
                    enabled = page.pageIndex > 0,
                    modifier = Modifier.size(PDF_NAVIGATION_BUTTON_SIZE_DP.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        stringResource(R.string.pdf_previous_page),
                    )
                }
                Text(
                    stringResource(R.string.pdf_page_counter, page.pageIndex + 1, page.pageCount),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (page.pageIndex == page.pageCount - 1) {
                    TextButton(onClick = onFinishBook) {
                        Text(stringResource(R.string.pdf_finish_book))
                    }
                } else {
                    IconButton(
                        onClick = { changePage(1) },
                        modifier = Modifier.size(PDF_NAVIGATION_BUTTON_SIZE_DP.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            stringResource(R.string.pdf_next_page),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfRenderError(onAdaptedReading: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(PDF_ERROR_PADDING_DP.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.pdf_render_error),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onAdaptedReading,
            modifier = Modifier.padding(top = PDF_ERROR_ACTION_SPACING_DP.dp),
        ) {
            Text(stringResource(R.string.pdf_adapted_reading))
        }
    }
}

private sealed interface PdfPageUiState {
    data object Loading : PdfPageUiState

    data object Error : PdfPageUiState

    data class Loaded(
        val page: RenderedPdfPage,
    ) : PdfPageUiState
}

private data class RenderedPdfPage(
    val bitmap: Bitmap,
    val pageIndex: Int,
    val pageCount: Int,
)

private object PdfPageRenderer {
    fun render(
        filePath: String,
        requestedPageIndex: Int,
        targetWidth: Int,
    ): RenderedPdfPage {
        val file = File(filePath)
        require(file.isFile) { "PDF original no disponible" }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount > 0) { "PDF sin páginas" }
                val pageIndex = requestedPageIndex.coerceIn(0, renderer.pageCount - 1)
                renderer.openPage(pageIndex).use { page ->
                    val requestedWidth = targetWidth.coerceIn(MINIMUM_RENDER_WIDTH, MAXIMUM_RENDER_WIDTH)
                    val requestedHeight =
                        (
                            requestedWidth.toFloat() * page.height.toFloat() / page.width.coerceAtLeast(1)
                        ).toInt().coerceAtLeast(1)
                    val bitmapHeight = requestedHeight.coerceAtMost(MAXIMUM_RENDER_HEIGHT)
                    val bitmapWidth =
                        if (requestedHeight > MAXIMUM_RENDER_HEIGHT) {
                            (requestedWidth.toFloat() * bitmapHeight / requestedHeight).toInt().coerceAtLeast(1)
                        } else {
                            requestedWidth
                        }
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return RenderedPdfPage(bitmap, pageIndex, renderer.pageCount)
                }
            }
        }
    }

    private const val MINIMUM_RENDER_WIDTH = 320
    private const val MAXIMUM_RENDER_WIDTH = 2_048
    private const val MAXIMUM_RENDER_HEIGHT = 4_096
}

private const val PDF_PAGE_HORIZONTAL_PADDING_DP = 12
private const val PDF_PAGE_VERTICAL_PADDING_DP = 8
private const val PDF_PAGE_BOTTOM_CLEARANCE_DP = 64
private const val PDF_SWIPE_THRESHOLD_DP = 56
private const val PDF_NAVIGATION_HORIZONTAL_PADDING_DP = 18
private const val PDF_NAVIGATION_BUTTON_SIZE_DP = 48
private const val PDF_NAVIGATION_ELEVATION_DP = 6
private const val PDF_ERROR_PADDING_DP = 32
private const val PDF_ERROR_ACTION_SPACING_DP = 16
