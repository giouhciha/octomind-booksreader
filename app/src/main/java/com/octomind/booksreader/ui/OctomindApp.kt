package com.octomind.booksreader.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.octomind.booksreader.R
import com.octomind.booksreader.audio.AmbientAudioController
import com.octomind.booksreader.domain.AmbientIntensity
import com.octomind.booksreader.domain.AmbientSoundscape
import com.octomind.booksreader.domain.BookSummary
import com.octomind.booksreader.domain.CompletedReading
import com.octomind.booksreader.domain.FocusNavigation
import com.octomind.booksreader.domain.FocusPresentation
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.NarratorPagination
import com.octomind.booksreader.domain.PageTheme
import com.octomind.booksreader.domain.ReaderFontStyle
import com.octomind.booksreader.domain.ReadingAmbience
import com.octomind.booksreader.domain.ReadingAmbienceSelector
import com.octomind.booksreader.domain.ReadingBlock
import com.octomind.booksreader.domain.ReadingParagraph
import com.octomind.booksreader.domain.ReadingSessionSummary
import com.octomind.booksreader.domain.SavedQuote
import com.octomind.booksreader.ui.theme.ReaderPageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor

@Composable
fun OctomindApp(viewModel: OctomindViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.resumeForForeground()
                    Lifecycle.Event.ON_STOP -> viewModel.pauseForBackground()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = state.screen) {
            AppScreen.Starting -> LoadingScreen()
            AppScreen.Onboarding -> OnboardingScreen(onConfirm = viewModel::confirmAdult)
            AppScreen.Library ->
                LibraryScreen(
                    books = state.books,
                    busy = state.busy,
                    onImport = viewModel::importBook,
                    onOpen = viewModel::openBook,
                    onDelete = viewModel::deleteBook,
                    onShowQuotes = viewModel::showQuotes,
                    onCreateBackup = viewModel::createBackup,
                    onRestoreBackup = viewModel::restoreBackup,
                )
            AppScreen.Quotes ->
                QuotesScreen(
                    quotes = state.quotes,
                    books = state.books,
                    customAvatarPath = state.customNarratorAvatarPath,
                    onBack = viewModel::closeQuotes,
                    onOpen = viewModel::openQuote,
                    onDelete = viewModel::deleteQuote,
                )
            is AppScreen.Reader ->
                ReaderScreen(
                    state = screen.state,
                    onBack = viewModel::finishReader,
                    onToggleFocus = viewModel::toggleFocus,
                    onMoveBlock = viewModel::moveBlock,
                    onVisibleParagraph = viewModel::setVisibleParagraph,
                    onReachedBookEnd = viewModel::completeCurrentBook,
                    onPageTheme = viewModel::updatePageTheme,
                    onFontStyle = viewModel::updateFontStyle,
                    onFontSize = viewModel::updateFontSize,
                    onFocusDimming = viewModel::updateFocusDimming,
                    onShowFocusMascot = viewModel::updateShowFocusMascot,
                    onFocusPresentation = viewModel::updateFocusPresentation,
                    onNarratorAvatar = viewModel::updateNarratorAvatar,
                    onImportCustomAvatar = viewModel::importCustomNarratorAvatar,
                    onDeleteCustomAvatar = viewModel::deleteCustomNarratorAvatar,
                    onReaderControlsExpanded = viewModel::updateReaderControlsExpanded,
                    onAmbientIntensity = viewModel::updateAmbientIntensity,
                    onAmbientAudioEnabled = viewModel::updateAmbientAudioEnabled,
                    onAmbientSoundscape = viewModel::updateAmbientSoundscape,
                    onAmbientAudioVolume = viewModel::updateAmbientAudioVolume,
                    onSaveQuote = viewModel::saveCurrentQuote,
                    onSaveSelectedQuote = viewModel::saveSelectedQuote,
                    onNarratorGestureLearned = viewModel::dismissNarratorGestureHint,
                    onShowOriginalPdf = viewModel::showOriginalPdf,
                    onHideOriginalPdf = viewModel::hideOriginalPdf,
                    onOriginalPdfPage = viewModel::setOriginalPdfPage,
                    onFinishOriginalPdf = viewModel::finishOriginalPdfReading,
                )
            is AppScreen.SessionResult ->
                SessionResultScreen(
                    summary = screen.summary,
                    previousReadings = screen.previousReadings,
                    restartAvailable = screen.restartAvailable,
                    onFinish = viewModel::returnToLibrary,
                    onRestart = { viewModel.restartCompletedBook(screen.bookId) },
                )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OnboardingScreen(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                Text(
                    text = stringResource(R.string.onboarding_badge),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal,
            )
        }

        Column {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(stringResource(R.string.adult_only_title), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.adult_only_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.confirm_adult))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.privacy_note),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    books: List<BookSummary>,
    busy: Boolean,
    onImport: (Uri) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onShowQuotes: () -> Unit,
    onCreateBackup: (Uri, CharArray) -> Unit,
    onRestoreBackup: (Uri, CharArray) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<BookSummary?>(null) }
    var backupDialog by rememberSaveable { mutableStateOf<BackupDialog?>(null) }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingRestoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onImport)
        }
    val createBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
        ) { uri ->
            val password = pendingPassword
            pendingPassword = null
            if (uri != null && password != null) onCreateBackup(uri, password) else password?.fill('\u0000')
        }
    val restoreBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                pendingRestoreUri = uri
                backupDialog = BackupDialog.RESTORE_PASSWORD
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.library_brand), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.library_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    FilledIconButton(onClick = { backupDialog = BackupDialog.MENU }, enabled = !busy) {
                        Icon(Icons.Rounded.CloudSync, stringResource(R.string.backup))
                    }
                    FilledIconButton(onClick = onShowQuotes) {
                        Icon(Icons.Rounded.Bookmark, stringResource(R.string.my_quotes))
                    }
                    FilledIconButton(
                        onClick = {
                            launcher.launch(arrayOf("text/plain", "application/epub+zip", "application/pdf"))
                        },
                        enabled = !busy,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Rounded.Add, stringResource(R.string.import_book))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3D293)),
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            EmptyLibrary(
                modifier = Modifier.padding(padding),
                onImport = {
                    launcher.launch(arrayOf("text/plain", "application/epub+zip", "application/pdf"))
                },
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                val columns = if (maxWidth >= 600.dp) 5 else 3
                val horizontalPadding = 16.dp
                val spacing = 12.dp
                val coverWidth = (maxWidth - horizontalPadding * 2 - spacing * (columns - 1)) / columns
                val coverHeight = coverWidth * 1.48f
                LazyColumn(
                    modifier = Modifier.fillMaxSize().woodLibraryBackground(),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(bottom = 24.dp),
                ) {
                    items(books.chunked(columns), key = { row -> row.joinToString("|") { it.id } }) { rowBooks ->
                        BookshelfRow(
                            books = rowBooks,
                            coverWidth = coverWidth,
                            coverHeight = coverHeight,
                            onOpen = onOpen,
                            onDelete = { pendingDelete = it },
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(book.id)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    when (backupDialog) {
        BackupDialog.MENU ->
            BackupMenuDialog(
                onDismiss = { backupDialog = null },
                onCreate = { backupDialog = BackupDialog.CREATE_PASSWORD },
                onRestore = {
                    backupDialog = null
                    restoreBackupLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream"))
                },
            )
        BackupDialog.CREATE_PASSWORD ->
            BackupPasswordDialog(
                restoring = false,
                onDismiss = { backupDialog = null },
                onConfirm = { password ->
                    pendingPassword = password.toCharArray()
                    backupDialog = null
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    createBackupLauncher.launch("octomind-$date.octomind")
                },
            )
        BackupDialog.RESTORE_PASSWORD ->
            BackupPasswordDialog(
                restoring = true,
                onDismiss = {
                    pendingRestoreUri = null
                    backupDialog = null
                },
                onConfirm = { password ->
                    pendingRestoreUri?.let { onRestoreBackup(it, password.toCharArray()) }
                    pendingRestoreUri = null
                    backupDialog = null
                },
            )
        null -> Unit
    }
}

private enum class BackupDialog { MENU, CREATE_PASSWORD, RESTORE_PASSWORD }

private const val BACKUP_MIME_TYPE = "application/vnd.octomind.backup"
private const val NARRATOR_LINE_HEIGHT_MULTIPLIER = 1.45f
private const val NARRATOR_HORIZONTAL_RESERVED_DP = 104
private const val NARRATOR_MINIMUM_TEXT_WIDTH_DP = 160
private const val NARRATOR_VERTICAL_RESERVED_DP = 360
private const val NARRATOR_MINIMUM_TEXT_HEIGHT_DP = 150
private const val READING_TITLE_SIZE_INCREASE_SP = 4
private const val READING_SECTION_SIZE_INCREASE_SP = 2
private const val MINIMUM_AMBIENT_VOLUME = 0f
private const val MAXIMUM_AMBIENT_VOLUME = 50f
private const val AMBIENT_VOLUME_STEP = 5f
private const val AMBIENT_VOLUME_SLIDER_STEPS = 9

@Composable
private fun BackupMenuDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_body))
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_backup))
                }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.restore_backup))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun BackupPasswordDialog(
    restoring: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = password.length >= 8 && (restoring || password == confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (restoring) R.string.restore_backup else R.string.protect_backup))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        if (restoring) R.string.restore_backup_warning else R.string.backup_password_hint,
                    ),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!restoring) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(stringResource(R.string.confirm_backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }, enabled = valid) {
                Text(stringResource(if (restoring) R.string.restore else R.string.continue_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EmptyLibrary(
    modifier: Modifier = Modifier,
    onImport: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().woodLibraryBackground().padding(28.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF9E8C5).copy(alpha = 0.96f),
            shadowElevation = 8.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp).size(38.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(stringResource(R.string.empty_library_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.empty_library_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onImport) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.import_book))
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.import_formats), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotesScreen(
    quotes: List<SavedQuote>,
    books: List<BookSummary>,
    customAvatarPath: String?,
    onBack: () -> Unit,
    onOpen: (SavedQuote) -> Unit,
    onDelete: (SavedQuote) -> Unit,
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    var pendingShare by remember { mutableStateOf<SavedQuote?>(null) }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_quotes), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3D293)),
            )
        },
    ) { padding ->
        if (quotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).woodLibraryBackground(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFF9E8C5),
                    shadowElevation = 7.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.empty_quotes), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.empty_quotes_hint), color = Color(0xFF6C5B43))
                    }
                }
            }
        } else {
            val groupedQuotes = quotes.groupBy(SavedQuote::bookTitle)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).woodLibraryBackground(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groupedQuotes.forEach { (bookTitle, bookQuotes) ->
                    item(key = "title-$bookTitle") {
                        Text(
                            bookTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9E8C5),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }
                    items(bookQuotes, key = SavedQuote::id) { quote ->
                        Surface(
                            modifier =
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.open_quote),
                                    onClick = { onOpen(quote) },
                                ),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFF9E8C5),
                            shadowElevation = 5.dp,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    quote.chapterTitle?.let { chapter ->
                                        Text(
                                            chapter,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF76511F),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text(
                                        "“${quote.text}”",
                                        fontFamily = LoraFontFamily,
                                        color = Color(0xFF352817),
                                        lineHeight = 22.sp,
                                    )
                                }
                                Column {
                                    IconButton(onClick = { pendingShare = quote }) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            stringResource(R.string.share_quote),
                                            tint = Color(0xFF256D4B),
                                        )
                                    }
                                    IconButton(onClick = { onDelete(quote) }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            stringResource(R.string.delete_quote),
                                            tint = Color(0xFF76511F),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingShare?.let { quote ->
        val sharedBook = books.firstOrNull { it.id == quote.bookId }
        val avatar = sharedBook?.narratorAvatar ?: NarratorAvatar.OCTI
        val narratorName =
            when (avatar) {
                NarratorAvatar.OCTI -> stringResource(R.string.narrator_avatar_octi)
                NarratorAvatar.LOVECRAFT_ILLUSTRATION -> stringResource(R.string.narrator_avatar_lovecraft)
                NarratorAvatar.SCHOPENHAUER_ILLUSTRATION -> stringResource(R.string.narrator_avatar_schopenhauer)
                NarratorAvatar.NIETZSCHE_ILLUSTRATION -> stringResource(R.string.narrator_avatar_nietzsche)
                NarratorAvatar.CAMUS_ILLUSTRATION -> stringResource(R.string.narrator_avatar_camus)
                NarratorAvatar.STRANGER_ILLUSTRATION -> stringResource(R.string.narrator_avatar_stranger)
                NarratorAvatar.LILA_ILLUSTRATION -> stringResource(R.string.narrator_avatar_lila)
                NarratorAvatar.ACHU_ILLUSTRATION -> stringResource(R.string.narrator_avatar_achu)
                NarratorAvatar.FRANK_N_FURTER_ILLUSTRATION ->
                    stringResource(R.string.narrator_avatar_frank_n_furter)
                NarratorAvatar.CUSTOM_IMAGE -> stringResource(R.string.narrator_avatar_custom)
            }
        AlertDialog(
            onDismissRequest = { pendingShare = null },
            title = { Text(stringResource(R.string.share_quote_title)) },
            text = { Text(stringResource(R.string.share_quote_privacy)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareScope.launch {
                            val uri =
                                withContext(Dispatchers.IO) {
                                    createNarratorQuoteCard(
                                        context = context,
                                        quote = quote,
                                        avatar = avatar,
                                        customAvatarPath = customAvatarPath,
                                        narratorName = narratorName,
                                        coverImagePath = sharedBook?.coverImagePath,
                                        fallbackBookColor = bookCoverPalette(quote.bookTitle).first.toArgb(),
                                    )
                                }
                            shareQuoteImage(context, uri)
                        }
                        pendingShare = null
                    },
                ) { Text(stringResource(R.string.share_with_narrator)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            shareQuote(context, quote, narratorName = null)
                            pendingShare = null
                        },
                    ) { Text(stringResource(R.string.share_quote_only)) }
                    TextButton(onClick = { pendingShare = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }
}

private fun shareQuote(
    context: Context,
    quote: SavedQuote,
    narratorName: String?,
) {
    val chapter = quote.chapterTitle?.let { "\n$it" }.orEmpty()
    val narrator = narratorName?.let { "Narrador: $it\n\n" }.orEmpty()
    val sharedText = "$narrator“${quote.text}”\n— ${quote.bookTitle}$chapter\n\nOctomind"
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharedText)
        }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_quote_chooser)),
    )
}

private fun createNarratorQuoteCard(
    context: Context,
    quote: SavedQuote,
    avatar: NarratorAvatar,
    customAvatarPath: String?,
    narratorName: String,
    coverImagePath: String?,
    fallbackBookColor: Int,
): Uri {
    val cardWidth = 1_200
    val bubbleLeft = 410f
    val bubbleRight = 1_120f
    val textWidth = (bubbleRight - bubbleLeft - 96f).toInt()
    val quotePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(50, 35, 18)
            textSize =
                when {
                    quote.text.length <= 180 -> 48f
                    quote.text.length <= 420 -> 42f
                    else -> 36f
                }
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
    val quoteLayout =
        StaticLayout.Builder
            .obtain("“${quote.text}”", 0, quote.text.length + 2, quotePaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(8f, 1f)
            .build()
    val bubbleHeight = (quoteLayout.height + 210).coerceAtLeast(460)
    val cardHeight = (bubbleHeight + 160).coerceAtLeast(720)
    val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(242, 218, 171))

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(37, 109, 75) }
    canvas.drawRoundRect(RectF(36f, 36f, cardWidth - 36f, cardHeight - 36f), 52f, 52f, accentPaint)
    val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(249, 232, 197) }
    canvas.drawRoundRect(RectF(54f, 54f, cardWidth - 54f, cardHeight - 54f), 42f, 42f, paperPaint)

    val bubbleTop = 82f
    val bubbleBottom = bubbleTop + bubbleHeight
    val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(216, 244, 229) }
    canvas.drawRoundRect(RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom), 42f, 42f, bubblePaint)
    canvas.drawPath(
        Path().apply {
            moveTo(bubbleLeft + 36f, bubbleBottom - 76f)
            lineTo(bubbleLeft - 56f, bubbleBottom - 28f)
            lineTo(bubbleLeft + 42f, bubbleBottom - 10f)
            close()
        },
        bubblePaint,
    )

    canvas.save()
    canvas.translate(bubbleLeft + 48f, bubbleTop + 52f)
    quoteLayout.draw(canvas)
    canvas.restore()

    val metadataPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(118, 81, 31)
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    canvas.drawText(quote.bookTitle.take(46), bubbleLeft + 48f, bubbleBottom - 72f, metadataPaint)
    quote.chapterTitle?.takeIf(String::isNotBlank)?.let { chapter ->
        metadataPaint.textSize = 24f
        metadataPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText(chapter.take(58), bubbleLeft + 48f, bubbleBottom - 34f, metadataPaint)
    }

    val avatarBitmap =
        if (avatar == NarratorAvatar.CUSTOM_IMAGE) {
            customAvatarPath?.let(BitmapFactory::decodeFile)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.octi_reader)
        } else {
            createBookColoredNarratorBitmap(
                context = context,
                avatar = avatar,
                coverImagePath = coverImagePath,
                fallbackBookColor = fallbackBookColor,
            ) ?: BitmapFactory.decodeResource(context.resources, R.drawable.octi_reader)
        }
    val avatarRect = RectF(92f, cardHeight - 430f, 372f, cardHeight - 150f)
    canvas.save()
    canvas.clipPath(Path().apply { addOval(avatarRect, Path.Direction.CW) })
    canvas.drawBitmap(avatarBitmap, null, avatarRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.restore()
    canvas.drawOval(
        avatarRect,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 12f
        },
    )

    val narratorPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(50, 40, 23)
            textSize = 27f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    canvas.drawText(narratorName.take(24), avatarRect.centerX(), cardHeight - 104f, narratorPaint)
    narratorPaint.textAlign = Paint.Align.RIGHT
    narratorPaint.textSize = 25f
    canvas.drawText("Octomind", cardWidth - 86f, cardHeight - 82f, narratorPaint)

    val shareDirectory = File(context.cacheDir, "shared_quotes").apply { mkdirs() }
    val output = File(shareDirectory, "quote-${quote.id}.png")
    output.outputStream().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            "No fue posible preparar la tarjeta"
        }
    }
    bitmap.recycle()
    avatarBitmap.recycle()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
}

private fun shareQuoteImage(
    context: Context,
    uri: Uri,
) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_quote_chooser)),
    )
}

@Composable
private fun BookshelfRow(
    books: List<BookSummary>,
    coverWidth: androidx.compose.ui.unit.Dp,
    coverHeight: androidx.compose.ui.unit.Dp,
    onOpen: (String) -> Unit,
    onDelete: (BookSummary) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(coverHeight + 42.dp)
                .woodShelfPanel(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            books.forEach { book ->
                BookCover(
                    book = book,
                    onOpen = { onOpen(book.id) },
                    onDelete = { onDelete(book) },
                    modifier = Modifier.width(coverWidth).height(coverHeight),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .shadow(8.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF0B35C), Color(0xFF9B531C), Color(0xFF71340F)),
                        ),
                    ),
        )
    }
}

@Composable
private fun BookCover(
    book: BookSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = bookCoverPalette(book.title)
    val coverBitmap by produceState<ImageBitmap?>(null, book.coverImagePath) {
        value =
            withContext(Dispatchers.IO) {
                book.coverImagePath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
            }
    }
    Surface(
        modifier =
            modifier
                .shadow(7.dp, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .clickable(
                    onClickLabel = stringResource(R.string.open_book, book.title),
                    onClick = onOpen,
                ),
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
        color = palette.first,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = stringResource(R.string.book_cover, book.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                color = palette.second.copy(alpha = 0.09f),
                                radius = size.minDimension * 0.55f,
                                center =
                                    androidx.compose.ui.geometry
                                        .Offset(size.width * 0.82f, size.height * 0.68f),
                            )
                            drawLine(
                                color = palette.second.copy(alpha = 0.28f),
                                start =
                                    androidx.compose.ui.geometry
                                        .Offset(size.width * 0.12f, 0f),
                                end =
                                    androidx.compose.ui.geometry
                                        .Offset(size.width * 0.12f, size.height),
                                strokeWidth = 2.dp.toPx(),
                            )
                        },
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 14.dp, top = 28.dp, end = 10.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        book.title,
                        color = palette.second,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LoraFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = palette.second.copy(alpha = 0.78f),
                    )
                    book.author?.let {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            it,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            color = palette.second.copy(alpha = 0.82f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = palette.second,
                        trackColor = palette.second.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(R.string.cover_progress, (book.progress * 100).roundToInt()),
                        fontSize = 9.sp,
                        color = palette.second,
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    stringResource(R.string.delete_book, book.title),
                    tint = palette.second.copy(alpha = 0.76f),
                    modifier = Modifier.size(17.dp),
                )
            }
            if (book.isCompleted) {
                val completedDescription =
                    stringResource(
                        R.string.completed_book_description,
                        book.title,
                    )
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                            .size(30.dp)
                            .semantics {
                                contentDescription = completedDescription
                            },
                    shape = CircleShape,
                    color = Color(0xFF2E7D52),
                    shadowElevation = 4.dp,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
    }
}

private fun bookCoverPalette(title: String): Pair<Color, Color> {
    val palettes =
        listOf(
            Color(0xFF174C5B) to Color(0xFFF6E8C8),
            Color(0xFF7A2731) to Color(0xFFFFE9C6),
            Color(0xFFE8D8AA) to Color(0xFF3C3528),
            Color(0xFF26355E) to Color(0xFFF4D06F),
            Color(0xFF4D6641) to Color(0xFFF4E7C5),
            Color(0xFF6B4B77) to Color(0xFFFFE9D6),
        )
    return palettes[(title.hashCode() and Int.MAX_VALUE) % palettes.size]
}

private fun Modifier.woodLibraryBackground(): Modifier =
    drawBehind {
        drawRect(
            brush =
                Brush.verticalGradient(
                    listOf(Color(0xFFD98B32), Color(0xFFB56521), Color(0xFFD18A35)),
                ),
        )
        val grain = 28.dp.toPx()
        var x = 8.dp.toPx()
        while (x < size.width) {
            drawLine(
                color = Color(0xFF6F3513).copy(alpha = 0.16f),
                start =
                    androidx.compose.ui.geometry
                        .Offset(x, 0f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(x + 8.dp.toPx(), size.height),
                strokeWidth = 1.dp.toPx(),
            )
            x += grain
        }
    }

private fun Modifier.woodShelfPanel(): Modifier =
    drawBehind {
        drawRect(
            brush =
                Brush.verticalGradient(
                    listOf(Color(0xFFB96825), Color(0xFFD98A33), Color(0xFFAA591D)),
                ),
        )
        repeat(6) { index ->
            val y = size.height * (index + 1) / 7f
            drawLine(
                color = Color(0xFF6E3513).copy(alpha = 0.14f),
                start =
                    androidx.compose.ui.geometry
                        .Offset(0f, y),
                end =
                    androidx.compose.ui.geometry
                        .Offset(size.width, y + 5.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    state: ReaderState,
    onBack: () -> Unit,
    onToggleFocus: () -> Unit,
    onMoveBlock: (Int) -> Unit,
    onVisibleParagraph: (Int) -> Unit,
    onReachedBookEnd: () -> Unit,
    onPageTheme: (PageTheme) -> Unit,
    onFontStyle: (ReaderFontStyle) -> Unit,
    onFontSize: (Int) -> Unit,
    onFocusDimming: (Int) -> Unit,
    onShowFocusMascot: (Boolean) -> Unit,
    onFocusPresentation: (FocusPresentation) -> Unit,
    onNarratorAvatar: (NarratorAvatar) -> Unit,
    onImportCustomAvatar: (Uri) -> Unit,
    onDeleteCustomAvatar: () -> Unit,
    onReaderControlsExpanded: (Boolean) -> Unit,
    onAmbientIntensity: (AmbientIntensity) -> Unit,
    onAmbientAudioEnabled: (Boolean) -> Unit,
    onAmbientSoundscape: (AmbientSoundscape) -> Unit,
    onAmbientAudioVolume: (Int) -> Unit,
    onSaveQuote: () -> Unit,
    onSaveSelectedQuote: (String, Int, Int) -> Unit,
    onNarratorGestureLearned: () -> Unit,
    onShowOriginalPdf: () -> Unit,
    onHideOriginalPdf: () -> Unit,
    onOriginalPdfPage: (Int) -> Unit,
    onFinishOriginalPdf: () -> Unit,
) {
    ReaderPageTheme(state.settings.pageTheme) {
        val originalFilePath = state.document.originalFilePath
        if (state.originalPdfVisible && originalFilePath != null) {
            BackHandler(onBack = onHideOriginalPdf)
            PdfOriginalReader(
                title = state.document.summary.title,
                filePath = originalFilePath,
                requestedPageIndex = state.originalPdfPageIndex,
                readingProgress = state.progress,
                onBack = onHideOriginalPdf,
                onAdaptedReading = onHideOriginalPdf,
                onPageChanged = onOriginalPdfPage,
                onFinishBook = onFinishOriginalPdf,
            )
        } else {
            ReaderScreenContent(
                state = state,
                onBack = onBack,
                onToggleFocus = onToggleFocus,
                onMoveBlock = onMoveBlock,
                onVisibleParagraph = onVisibleParagraph,
                onReachedBookEnd = onReachedBookEnd,
                onPageTheme = onPageTheme,
                onFontStyle = onFontStyle,
                onFontSize = onFontSize,
                onFocusDimming = onFocusDimming,
                onShowFocusMascot = onShowFocusMascot,
                onFocusPresentation = onFocusPresentation,
                onNarratorAvatar = onNarratorAvatar,
                onImportCustomAvatar = onImportCustomAvatar,
                onDeleteCustomAvatar = onDeleteCustomAvatar,
                onReaderControlsExpanded = onReaderControlsExpanded,
                onAmbientIntensity = onAmbientIntensity,
                onAmbientAudioEnabled = onAmbientAudioEnabled,
                onAmbientSoundscape = onAmbientSoundscape,
                onAmbientAudioVolume = onAmbientAudioVolume,
                onSaveQuote = onSaveQuote,
                onSaveSelectedQuote = onSaveSelectedQuote,
                onNarratorGestureLearned = onNarratorGestureLearned,
                onShowOriginalPdf = onShowOriginalPdf,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreenContent(
    state: ReaderState,
    onBack: () -> Unit,
    onToggleFocus: () -> Unit,
    onMoveBlock: (Int) -> Unit,
    onVisibleParagraph: (Int) -> Unit,
    onReachedBookEnd: () -> Unit,
    onPageTheme: (PageTheme) -> Unit,
    onFontStyle: (ReaderFontStyle) -> Unit,
    onFontSize: (Int) -> Unit,
    onFocusDimming: (Int) -> Unit,
    onShowFocusMascot: (Boolean) -> Unit,
    onFocusPresentation: (FocusPresentation) -> Unit,
    onNarratorAvatar: (NarratorAvatar) -> Unit,
    onImportCustomAvatar: (Uri) -> Unit,
    onDeleteCustomAvatar: () -> Unit,
    onReaderControlsExpanded: (Boolean) -> Unit,
    onAmbientIntensity: (AmbientIntensity) -> Unit,
    onAmbientAudioEnabled: (Boolean) -> Unit,
    onAmbientSoundscape: (AmbientSoundscape) -> Unit,
    onAmbientAudioVolume: (Int) -> Unit,
    onSaveQuote: () -> Unit,
    onSaveSelectedQuote: (String, Int, Int) -> Unit,
    onNarratorGestureLearned: () -> Unit,
    onShowOriginalPdf: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val readerView = LocalView.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val narratorTextMeasurer = rememberTextMeasurer()
    val ambientAudioController = remember(context) { AmbientAudioController(context) }
    var ambientAudioPlaying by remember(state.document.summary.id) { mutableStateOf(false) }
    DisposableEffect(ambientAudioController, lifecycleOwner) {
        ambientAudioController.onPlaybackChanged = { ambientAudioPlaying = it }
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) ambientAudioController.pause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ambientAudioController.close()
        }
    }
    LaunchedEffect(state.settings.ambientAudioEnabled) {
        if (!state.settings.ambientAudioEnabled) ambientAudioController.pause()
    }
    LaunchedEffect(
        state.settings.ambientSoundscape,
        state.settings.ambientAudioVolumePercent,
    ) {
        ambientAudioController.update(
            state.settings.ambientSoundscape,
            state.settings.ambientAudioVolumePercent,
        )
    }
    DisposableEffect(readerView, state.focusEnabled) {
        val previousKeepScreenOn = readerView.keepScreenOn
        readerView.keepScreenOn = state.focusEnabled
        onDispose { readerView.keepScreenOn = previousKeepScreenOn }
    }
    val currentBlock = state.plan.blocks.getOrNull(state.currentBlockIndex)
    val initialParagraph = currentBlock?.paragraphIndex ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialParagraph)
    var activeBlockCenterInWindowY by remember(currentBlock?.index) { mutableStateOf<Float?>(null) }
    var listTopInWindow by remember { mutableFloatStateOf(0f) }
    var mascotPageTurnKey by rememberSaveable(state.document.summary.id) { mutableIntStateOf(0) }
    var mascotPageTurnDirection by remember { mutableIntStateOf(1) }
    var narratorPageIndex by rememberSaveable(state.document.summary.id) { mutableIntStateOf(0) }
    var narratorBlockIndex by remember(state.document.summary.id) {
        mutableIntStateOf(currentBlock?.index ?: -1)
    }
    val narratorTextStyle =
        MaterialTheme.typography.headlineSmall.copy(
            fontFamily = readerFontFamily(state.settings.fontStyle),
            fontSize = state.settings.fontSizeSp.sp,
            lineHeight = (state.settings.fontSizeSp * NARRATOR_LINE_HEIGHT_MULTIPLIER).sp,
        )
    val narratorTextWidthPixels =
        with(density) {
            (configuration.screenWidthDp.dp - NARRATOR_HORIZONTAL_RESERVED_DP.dp)
                .coerceAtLeast(NARRATOR_MINIMUM_TEXT_WIDTH_DP.dp)
                .roundToPx()
        }
    val narratorTextHeightPixels =
        with(density) {
            (configuration.screenHeightDp.dp - NARRATOR_VERTICAL_RESERVED_DP.dp)
                .coerceAtLeast(NARRATOR_MINIMUM_TEXT_HEIGHT_DP.dp)
                .roundToPx()
        }
    val narratorPages =
        remember(
            currentBlock?.index,
            currentBlock?.text,
            narratorTextStyle,
            narratorTextWidthPixels,
            narratorTextHeightPixels,
        ) {
            NarratorPagination.paginate(currentBlock?.text.orEmpty()) { candidate ->
                narratorTextMeasurer
                    .measure(
                        text = AnnotatedString(candidate),
                        style = narratorTextStyle,
                        constraints = Constraints(maxWidth = narratorTextWidthPixels),
                    ).size.height <= narratorTextHeightPixels
            }
        }

    fun navigateBlock(delta: Int) {
        val targetIndex = state.currentBlockIndex + delta
        val completesBook = delta > 0 && state.currentBlockIndex == state.plan.blocks.lastIndex
        if (delta == 0 || (targetIndex !in state.plan.blocks.indices && !completesBook)) return
        mascotPageTurnDirection = delta
        if (!completesBook) mascotPageTurnKey += 1
        onMoveBlock(delta)
    }

    fun navigateFocus(delta: Int) {
        if (delta == 0) return
        if (state.settings.focusPresentation == FocusPresentation.OCTI_NARRATOR) {
            val targetPage = narratorPageIndex + delta
            if (targetPage in narratorPages.indices) {
                narratorPageIndex = targetPage
                mascotPageTurnDirection = delta
                mascotPageTurnKey += 1
                return
            }
        }
        navigateBlock(delta)
    }

    LaunchedEffect(currentBlock?.index, narratorPages.size) {
        if (currentBlock?.index != narratorBlockIndex) {
            narratorPageIndex = if (mascotPageTurnDirection < 0) narratorPages.lastIndex else 0
            narratorBlockIndex = currentBlock?.index ?: -1
        } else {
            narratorPageIndex = narratorPageIndex.coerceIn(0, narratorPages.lastIndex)
        }
    }

    LaunchedEffect(state.focusEnabled, currentBlock?.index) {
        val block = currentBlock ?: return@LaunchedEffect
        if (!state.focusEnabled && !state.quotePreview) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == block.paragraphIndex }) {
            listState.scrollToItem(block.paragraphIndex)
        }
    }
    LaunchedEffect(state.focusEnabled, currentBlock?.index, activeBlockCenterInWindowY, listTopInWindow) {
        if (!state.focusEnabled && !state.quotePreview) return@LaunchedEffect
        val markerCenterInWindowY = activeBlockCenterInWindowY ?: return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val targetCenterInListY =
            FocusNavigation.targetCenterInList(
                screenHeightPixels = readerView.height,
                listTopInWindowPixels = listTopInWindow,
                viewportStartPixels = layoutInfo.viewportStartOffset,
                viewportEndPixels = layoutInfo.viewportEndOffset,
            )
        val targetCenterInWindowY = listTopInWindow + targetCenterInListY
        val distanceToCenter = markerCenterInWindowY - targetCenterInWindowY
        if (kotlin.math.abs(distanceToCenter) > 1f) {
            listState.scrollBy(distanceToCenter)
        }
    }
    LaunchedEffect(state.focusEnabled, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { !state.focusEnabled }
            .drop(1)
            .collect(onVisibleParagraph)
    }
    LaunchedEffect(state.focusEnabled, listState) {
        if (state.focusEnabled) return@LaunchedEffect
        var userScrolled = false
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }
            .distinctUntilChanged()
            .collect { (isScrolling, canScrollForward) ->
                if (isScrolling) {
                    userScrolled = true
                } else if (userScrolled && !canScrollForward) {
                    userScrolled = false
                    onReachedBookEnd()
                }
            }
    }

    Scaffold(
        topBar = {
            if (!state.focusEnabled) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(state.document.summary.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when {
                                        state.quotePreview -> stringResource(R.string.quote_preview)
                                        state.focusEnabled -> stringResource(R.string.focus_mode)
                                        else -> stringResource(R.string.normal_quote_hint)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                        },
                        actions = {
                            if (state.document.originalFilePath != null) {
                                TextButton(onClick = onShowOriginalPdf) {
                                    Text(stringResource(R.string.pdf_original_page))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            if (!state.focusEnabled || state.settings.readerControlsExpanded) {
                ReaderControls(
                    state = state,
                    onToggleFocus = onToggleFocus,
                    onMoveBlock = ::navigateFocus,
                    onPageTheme = onPageTheme,
                    onFontStyle = onFontStyle,
                    onFontSize = onFontSize,
                    onFocusDimming = onFocusDimming,
                    onShowFocusMascot = onShowFocusMascot,
                    onFocusPresentation = onFocusPresentation,
                    onNarratorAvatar = onNarratorAvatar,
                    onImportCustomAvatar = onImportCustomAvatar,
                    onDeleteCustomAvatar = onDeleteCustomAvatar,
                    onReaderControlsExpanded = onReaderControlsExpanded,
                    onAmbientIntensity = onAmbientIntensity,
                    onAmbientAudioEnabled = onAmbientAudioEnabled,
                    onAmbientSoundscape = onAmbientSoundscape,
                    onAmbientAudioVolume = onAmbientAudioVolume,
                )
            }
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val verticalContentPadding =
                if (state.focusEnabled) {
                    (maxHeight / 2 - 24.dp).coerceAtLeast(24.dp)
                } else {
                    24.dp
                }
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            listTopInWindow = coordinates.positionInWindow().y
                        }.pointerInput(
                            state.focusEnabled,
                            state.currentBlockIndex,
                            state.plan.blocks.size,
                            state.settings.focusPresentation,
                            narratorPageIndex,
                            narratorPages.size,
                        ) {
                            if (!state.focusEnabled) return@pointerInput
                            var accumulatedDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { accumulatedDragY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDragY += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 48.dp.toPx()
                                    val delta = FocusNavigation.blockDelta(accumulatedDragY, threshold)
                                    val targetIndex = state.currentBlockIndex + delta
                                    val completesBook =
                                        delta > 0 &&
                                            state.currentBlockIndex == state.plan.blocks.lastIndex
                                    val hasNarratorPage =
                                        state.settings.focusPresentation ==
                                            FocusPresentation.OCTI_NARRATOR &&
                                            narratorPageIndex + delta in narratorPages.indices
                                    val canNavigate =
                                        hasNarratorPage ||
                                            targetIndex in state.plan.blocks.indices ||
                                            completesBook
                                    if (delta != 0 && canNavigate) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (state.settings.focusPresentation == FocusPresentation.OCTI_NARRATOR) {
                                            onNarratorGestureLearned()
                                        }
                                        navigateFocus(delta)
                                    }
                                },
                            )
                        },
                userScrollEnabled = !state.focusEnabled,
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 24.dp,
                        vertical = verticalContentPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(state.plan.paragraphs, key = ReadingParagraph::startCharacterOffset) { paragraph ->
                    val activeBlock =
                        currentBlock?.takeIf {
                            (state.focusEnabled || state.quotePreview) &&
                                it.paragraphIndex == paragraph.index
                        }
                    ReaderParagraph(
                        paragraph = paragraph,
                        activeBlock = activeBlock,
                        fontStyle = state.settings.fontStyle,
                        fontSizeSp = state.settings.fontSizeSp,
                        focusEnabled = state.focusEnabled,
                        focusDimmingPercent = state.settings.focusDimmingPercent,
                        onActiveBlockCenter =
                            if (activeBlock == null) {
                                null
                            } else {
                                { centerY -> activeBlockCenterInWindowY = centerY }
                            },
                        onSaveQuote = onSaveQuote,
                        quoteStartCharacterOffset = state.previewQuoteStartOffset,
                        quoteEndCharacterOffset = state.previewQuoteEndOffset,
                        enableQuoteSelection = !state.focusEnabled && !state.quotePreview,
                        onSaveSelectedQuote = onSaveSelectedQuote,
                    )
                }
            }
            if (
                state.focusEnabled &&
                state.settings.focusPresentation == FocusPresentation.OCTI_NARRATOR &&
                currentBlock != null
            ) {
                val currentChapter =
                    state.document.chapters
                        .lastOrNull { it.startCharacterOffset <= currentBlock.startCharacterOffset }
                val chapterStart = currentChapter?.startCharacterOffset ?: 0
                val nextChapterStart =
                    state.document.chapters
                        .firstOrNull { it.startCharacterOffset > chapterStart }
                        ?.startCharacterOffset
                        ?: state.document.text.length
                val safeChapterStart = chapterStart.coerceIn(0, state.document.text.length)
                val safeChapterEnd = nextChapterStart.coerceIn(safeChapterStart, state.document.text.length)
                val ambience =
                    remember(
                        state.document.summary.id,
                        currentChapter?.startCharacterOffset,
                    ) {
                        ReadingAmbienceSelector.select(
                            title = state.document.summary.title,
                            chapterTitle = currentChapter?.title,
                            chapterSample =
                                state.document.text.substring(
                                    safeChapterStart,
                                    safeChapterEnd,
                                ),
                        )
                    }
                OctiNarratorOverlay(
                    text = narratorPages.getOrElse(narratorPageIndex) { narratorPages.first() },
                    blockIndex = currentBlock.index,
                    continuationIndex = narratorPageIndex,
                    continuationCount = narratorPages.size,
                    textStyle = narratorTextStyle,
                    pageTurnKey = mascotPageTurnKey,
                    pageTurnDirection = mascotPageTurnDirection,
                    ambience = ambience,
                    ambientIntensity = state.settings.ambientIntensity,
                    narratorAvatar = state.settings.narratorAvatar,
                    customAvatarPath = state.customNarratorAvatarPath,
                    customAvatarVersion = state.settings.customNarratorAvatarVersion,
                    coverImagePath = state.document.summary.coverImagePath,
                    fallbackBookColor = bookCoverPalette(state.document.summary.title).first.toArgb(),
                    showGestureHint = !state.settings.narratorGestureHintDismissed,
                    onSaveQuote = onSaveQuote,
                )
            }
            if (state.focusEnabled && !state.settings.readerControlsExpanded) {
                FocusFullscreenTapZones(
                    onBack = onBack,
                    onExitFocus = onToggleFocus,
                    onOpenFocusMenu = { onReaderControlsExpanded(true) },
                )
            }
            if (state.settings.ambientAudioEnabled) {
                AmbientAudioControl(
                    soundscape = state.settings.ambientSoundscape,
                    isPlaying = ambientAudioPlaying,
                    onTogglePlayback = {
                        if (ambientAudioPlaying) {
                            ambientAudioController.pause()
                        } else {
                            ambientAudioController.play(
                                state.settings.ambientSoundscape,
                                state.settings.ambientAudioVolumePercent,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderParagraph(
    paragraph: ReadingParagraph,
    activeBlock: ReadingBlock?,
    fontStyle: ReaderFontStyle,
    fontSizeSp: Int,
    focusEnabled: Boolean,
    focusDimmingPercent: Int,
    onActiveBlockCenter: ((Float) -> Unit)?,
    onSaveQuote: () -> Unit,
    quoteStartCharacterOffset: Int?,
    quoteEndCharacterOffset: Int?,
    enableQuoteSelection: Boolean,
    onSaveSelectedQuote: (String, Int, Int) -> Unit,
) {
    val paragraphEmphasis = readingParagraphEmphasis(paragraph.text)
    val paragraphSizeIncrease =
        when (paragraphEmphasis) {
            ReadingParagraphEmphasis.TITLE -> READING_TITLE_SIZE_INCREASE_SP
            ReadingParagraphEmphasis.SECTION -> READING_SECTION_SIZE_INCREASE_SP
            ReadingParagraphEmphasis.BODY -> 0
        }
    var textTopInWindow by remember { mutableFloatStateOf(0f) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var selectionAnchorStart by remember(paragraph.index) { mutableIntStateOf(0) }
    var selectionAnchorEnd by remember(paragraph.index) { mutableIntStateOf(0) }
    var selectionStart by remember(paragraph.index) { mutableStateOf<Int?>(null) }
    var selectionEnd by remember(paragraph.index) { mutableStateOf<Int?>(null) }
    var magnifierCenter by remember(paragraph.index) { mutableStateOf(Offset.Unspecified) }
    val selectionHaptics = LocalHapticFeedback.current

    fun reportActiveBlockCenter() {
        val block = activeBlock ?: return
        val layoutResult = textLayoutResult ?: return
        if (paragraph.text.isEmpty()) return
        val offset = block.localStartOffset.coerceIn(0, paragraph.text.lastIndex)
        onActiveBlockCenter?.invoke(textTopInWindow + layoutResult.getBoundingBox(offset).center.y)
    }
    val text =
        buildAnnotatedString {
            append(paragraph.text)
            val quoteStart =
                quoteStartCharacterOffset?.let {
                    (it - paragraph.startCharacterOffset).coerceIn(0, paragraph.text.length)
                }
            val quoteEnd =
                quoteEndCharacterOffset?.let {
                    (it - paragraph.startCharacterOffset).coerceIn(0, paragraph.text.length)
                }
            if (quoteStart != null && quoteEnd != null && quoteStart < quoteEnd) {
                addStyle(
                    SpanStyle(
                        background = Color(0xFFFFD66B),
                        color = Color(0xFF33230A),
                        fontWeight = FontWeight.Bold,
                    ),
                    start = quoteStart,
                    end = quoteEnd,
                )
            }
            val selectedStart = selectionStart
            val selectedEnd = selectionEnd
            if (selectedStart != null && selectedEnd != null && selectedStart < selectedEnd) {
                addStyle(
                    SpanStyle(
                        background = Color(0xFFFFD66B),
                        color = Color(0xFF33230A),
                        fontWeight = FontWeight.Bold,
                    ),
                    start = selectedStart,
                    end = selectedEnd,
                )
            }
            if (
                activeBlock != null &&
                quoteStartCharacterOffset == null &&
                activeBlock.localStartOffset in 0..paragraph.text.length
            ) {
                addStyle(
                    SpanStyle(
                        background = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    start = activeBlock.localStartOffset,
                    end = activeBlock.localEndOffset.coerceAtMost(paragraph.text.length),
                )
            }
        }
    Text(
        text = text,
        modifier =
            Modifier
                .onGloballyPositioned { coordinates ->
                    textTopInWindow = coordinates.positionInWindow().y
                    reportActiveBlockCenter()
                }.then(
                    if (activeBlock == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(activeBlock.index) {
                            detectTapGestures(onLongPress = { onSaveQuote() })
                        }
                    },
                ).then(
                    if (!enableQuoteSelection) {
                        Modifier
                    } else {
                        Modifier.pointerInput(paragraph.index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { position ->
                                    val layout = textLayoutResult ?: return@detectDragGesturesAfterLongPress
                                    val offset =
                                        layout
                                            .getOffsetForPosition(position)
                                            .coerceIn(0, paragraph.text.length)
                                    val boundary = layout.getWordBoundary(offset)
                                    selectionAnchorStart = boundary.start
                                    selectionAnchorEnd = boundary.end
                                    selectionStart = boundary.start
                                    selectionEnd = boundary.end
                                    magnifierCenter = position
                                    selectionHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, _ ->
                                    val layout = textLayoutResult ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    val offset =
                                        layout
                                            .getOffsetForPosition(change.position)
                                            .coerceIn(0, paragraph.text.length)
                                    val boundary = layout.getWordBoundary(offset)
                                    selectionStart = minOf(selectionAnchorStart, boundary.start)
                                    selectionEnd = maxOf(selectionAnchorEnd, boundary.end)
                                    magnifierCenter = change.position
                                },
                                onDragEnd = {
                                    val start = selectionStart
                                    val end = selectionEnd
                                    if (start != null && end != null && start < end) {
                                        onSaveSelectedQuote(
                                            paragraph.text.substring(start, end),
                                            paragraph.startCharacterOffset + start,
                                            paragraph.startCharacterOffset + end,
                                        )
                                    }
                                    selectionStart = null
                                    selectionEnd = null
                                    magnifierCenter = Offset.Unspecified
                                },
                                onDragCancel = {
                                    selectionStart = null
                                    selectionEnd = null
                                    magnifierCenter = Offset.Unspecified
                                },
                            )
                        }
                    },
                ).then(
                    if (!enableQuoteSelection) {
                        Modifier
                    } else {
                        Modifier.magnifier(
                            sourceCenter = { magnifierCenter },
                        )
                    },
                ),
        style =
            MaterialTheme.typography.bodyLarge.copy(
                color =
                    MaterialTheme.colorScheme.onBackground.copy(
                        alpha =
                            if (focusEnabled) {
                                1f - focusDimmingPercent.coerceIn(0, 80) / 100f
                            } else {
                                1f
                            },
                    ),
                fontFamily = readerFontFamily(fontStyle),
                fontSize = (fontSizeSp + paragraphSizeIncrease).sp,
                fontWeight =
                    when (paragraphEmphasis) {
                        ReadingParagraphEmphasis.TITLE -> FontWeight.Bold
                        ReadingParagraphEmphasis.SECTION -> FontWeight.SemiBold
                        ReadingParagraphEmphasis.BODY -> FontWeight.Normal
                    },
                lineHeight = ((fontSizeSp + paragraphSizeIncrease) * 1.6f).sp,
            ),
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
            reportActiveBlockCenter()
        },
    )
}

private enum class ReadingParagraphEmphasis {
    TITLE,
    SECTION,
    BODY,
}

private fun readingParagraphEmphasis(text: String): ReadingParagraphEmphasis {
    val normalized = text.trim().lowercase()
    if (normalized in setOf("índice", "contenido", "contenidos", "tabla de contenidos")) {
        return ReadingParagraphEmphasis.TITLE
    }
    if (
        normalized.startsWith("capítulo ") ||
        normalized.startsWith("parte ") ||
        normalized.startsWith("sección ") ||
        normalized in setOf("prólogo", "introducción", "epílogo", "referencias")
    ) {
        return ReadingParagraphEmphasis.SECTION
    }
    return ReadingParagraphEmphasis.BODY
}

@Composable
private fun FocusReadingMascot(
    mascotSize: Dp = 116.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(mascotSize),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.octi_reader),
            contentDescription = stringResource(R.string.focus_mascot_description),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun OctiNarratorOverlay(
    text: String,
    blockIndex: Int,
    continuationIndex: Int,
    continuationCount: Int,
    textStyle: TextStyle,
    pageTurnKey: Int,
    pageTurnDirection: Int,
    ambience: ReadingAmbience,
    ambientIntensity: AmbientIntensity,
    narratorAvatar: NarratorAvatar,
    customAvatarPath: String?,
    customAvatarVersion: Int,
    coverImagePath: String?,
    fallbackBookColor: Int,
    showGestureHint: Boolean,
    onSaveQuote: () -> Unit,
) {
    val bubblePageTurn = remember { Animatable(0f) }
    LaunchedEffect(pageTurnKey) {
        if (pageTurnKey == 0) {
            bubblePageTurn.snapTo(0f)
            return@LaunchedEffect
        }
        bubblePageTurn.snapTo(pageTurnDirection.toFloat())
        bubblePageTurn.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        )
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ReadingAmbientBackdrop(
            ambience = ambience,
            intensity = ambientIntensity,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background.copy(
                        alpha =
                            when (ambientIntensity) {
                                AmbientIntensity.OFF -> 0.92f
                                AmbientIntensity.SUBTLE -> 0.62f
                                AmbientIntensity.IMMERSIVE -> 0.38f
                            },
                    ),
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(blockIndex, continuationIndex) {
                            detectTapGestures(onLongPress = { onSaveQuote() })
                        }.graphicsLayer {
                            rotationY = bubblePageTurn.value * 18f
                            translationX = bubblePageTurn.value * size.width * 0.08f
                            alpha = 1f - kotlin.math.abs(bubblePageTurn.value) * 0.18f
                            transformOrigin =
                                TransformOrigin(
                                    pivotFractionX = if (bubblePageTurn.value >= 0f) 0f else 1f,
                                    pivotFractionY = 0.5f,
                                )
                            cameraDistance = 18f * density
                        },
            ) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp)) {
                    Text(
                        text = text,
                        style = textStyle,
                    )
                    if (continuationCount > 1) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.narrator_continuation,
                                    continuationIndex + 1,
                                    continuationCount,
                                ),
                            modifier = Modifier.align(Alignment.End),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.64f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .offset(y = (-1).dp)
                        .size(22.dp)
                        .graphicsLayer {
                            rotationZ = 45f
                            alpha = 1f - kotlin.math.abs(bubblePageTurn.value) * 0.18f
                        }.background(MaterialTheme.colorScheme.primaryContainer),
            )
            FocusNarratorAvatar(
                avatar = narratorAvatar,
                customAvatarPath = customAvatarPath,
                customAvatarVersion = customAvatarVersion,
                coverImagePath = coverImagePath,
                fallbackBookColor = fallbackBookColor,
            )
            AnimatedVisibility(visible = showGestureHint) {
                Text(
                    stringResource(R.string.gesture_navigation_hint),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun FocusFullscreenTapZones(
    onBack: () -> Unit,
    onExitFocus: () -> Unit,
    onOpenFocusMenu: () -> Unit,
) {
    val backDescription = stringResource(R.string.back)
    val exitDescription = stringResource(R.string.exit_focus)
    val menuDescription = stringResource(R.string.show_reader_menu)
    val backInteraction = remember { MutableInteractionSource() }
    val exitInteraction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
    ) {
        Spacer(
            Modifier
                .align(Alignment.TopStart)
                .size(width = 112.dp, height = 88.dp)
                .semantics { contentDescription = backDescription }
                .clickable(
                    interactionSource = backInteraction,
                    indication = null,
                    onClick = onBack,
                ),
        )
        Spacer(
            Modifier
                .align(Alignment.TopEnd)
                .size(width = 112.dp, height = 88.dp)
                .semantics { contentDescription = exitDescription }
                .clickable(
                    interactionSource = exitInteraction,
                    indication = null,
                    onClick = onExitFocus,
                ),
        )
        Spacer(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .size(width = 200.dp, height = 96.dp)
                .semantics { contentDescription = menuDescription }
                .clickable(
                    interactionSource = menuInteraction,
                    indication = null,
                    onClick = onOpenFocusMenu,
                ),
        )
    }
}

@Composable
private fun FocusNarratorAvatar(
    avatar: NarratorAvatar,
    customAvatarPath: String?,
    customAvatarVersion: Int,
    coverImagePath: String?,
    fallbackBookColor: Int,
) {
    if (avatar == NarratorAvatar.CUSTOM_IMAGE) {
        CustomCircularNarratorAvatar(
            path = customAvatarPath,
            version = customAvatarVersion,
        )
    } else {
        BookColoredNarratorArtwork(
            avatar = avatar,
            coverImagePath = coverImagePath,
            fallbackBookColor = fallbackBookColor,
            modifier = Modifier.size(196.dp),
        )
    }
}

@Composable
private fun CustomCircularNarratorAvatar(
    path: String?,
    version: Int,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, version) {
        value =
            withContext(Dispatchers.IO) {
                path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
            }
    }
    Surface(
        modifier = Modifier.size(168.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        bitmap?.let { customBitmap ->
            Image(
                bitmap = customBitmap,
                contentDescription = stringResource(R.string.custom_avatar_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.AutoStories,
                contentDescription = stringResource(R.string.custom_avatar_missing_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun ReadingAmbientBackdrop(
    ambience: ReadingAmbience,
    intensity: AmbientIntensity,
    modifier: Modifier = Modifier,
) {
    val (targetStart, targetEnd) = ambientPalette(ambience)
    val startColor by animateColorAsState(targetStart, tween(1_000), label = "ambientStart")
    val endColor by animateColorAsState(targetEnd, tween(1_000), label = "ambientEnd")
    val opacity =
        when (intensity) {
            AmbientIntensity.OFF -> 0f
            AmbientIntensity.SUBTLE -> 0.58f
            AmbientIntensity.IMMERSIVE -> 0.9f
        }
    Canvas(modifier = modifier) {
        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end =
                        androidx.compose.ui.geometry
                            .Offset(size.width, size.height),
                ),
            alpha = opacity,
        )
        when (ambience) {
            ReadingAmbience.MYSTERY -> {
                drawCircle(Color(0xFF111827), size.minDimension * 0.38f, center = center, alpha = opacity * 0.34f)
                drawCircle(
                    Color(0xFFD8E3EA),
                    size.minDimension * 0.12f,
                    center =
                        center.copy(
                            x = size.width * 0.78f,
                            y =
                                size.height * 0.18f,
                        ),
                    alpha = opacity * 0.38f,
                )
            }
            ReadingAmbience.FANTASY -> {
                drawCircle(
                    Color(0xFFFFD782),
                    size.minDimension * 0.18f,
                    center = center.copy(x = size.width * 0.22f, y = size.height * 0.2f),
                    alpha =
                        opacity * 0.34f,
                )
                drawCircle(
                    Color(0xFFB8E0C2),
                    size.minDimension * 0.3f,
                    center = center.copy(x = size.width * 0.78f, y = size.height * 0.82f),
                    alpha =
                        opacity * 0.32f,
                )
            }
            ReadingAmbience.SCIENCE_FICTION ->
                repeat(6) { index ->
                    val y = size.height * (0.12f + index * 0.15f)
                    drawLine(
                        Color(0xFF76E4F7),
                        start = center.copy(x = 0f, y = y),
                        end =
                            center.copy(
                                x = size.width,
                                y =
                                    y - size.height * 0.08f,
                            ),
                        strokeWidth = 2f,
                        alpha = opacity * 0.3f,
                    )
                }
            ReadingAmbience.ROMANCE -> {
                drawCircle(
                    Color(0xFFFFCAD4),
                    size.minDimension * 0.32f,
                    center = center.copy(x = size.width * 0.2f, y = size.height * 0.25f),
                    alpha =
                        opacity * 0.42f,
                )
                drawCircle(
                    Color(0xFFFFE5B4),
                    size.minDimension * 0.28f,
                    center =
                        center.copy(
                            x = size.width * 0.85f,
                            y =
                                size.height * 0.78f,
                        ),
                    alpha = opacity * 0.38f,
                )
            }
            ReadingAmbience.NATURE -> {
                drawCircle(
                    Color(0xFFB7D7A8),
                    size.minDimension * 0.42f,
                    center = center.copy(x = size.width * 0.18f, y = size.height * 0.8f),
                    alpha =
                        opacity * 0.35f,
                )
                drawCircle(
                    Color(0xFFB6D7E8),
                    size.minDimension * 0.3f,
                    center = center.copy(x = size.width * 0.82f, y = size.height * 0.18f),
                    alpha =
                        opacity * 0.32f,
                )
            }
            ReadingAmbience.KNOWLEDGE ->
                repeat(5) { index ->
                    val inset = size.minDimension * (0.08f + index * 0.07f)
                    drawCircle(
                        Color(0xFFF4C95D),
                        inset,
                        center = center,
                        style =
                            androidx.compose.ui.graphics.drawscope
                                .Stroke(width = 2f),
                        alpha =
                            opacity * 0.25f,
                    )
                }
            ReadingAmbience.NEUTRAL -> {
                drawCircle(
                    Color.White,
                    size.minDimension * 0.34f,
                    center = center.copy(x = size.width * 0.78f, y = size.height * 0.2f),
                    alpha =
                        opacity * 0.2f,
                )
            }
        }
    }
}

private fun ambientPalette(ambience: ReadingAmbience): Pair<Color, Color> =
    when (ambience) {
        ReadingAmbience.MYSTERY -> Color(0xFF172033) to Color(0xFF44556A)
        ReadingAmbience.FANTASY -> Color(0xFF315C4A) to Color(0xFF7864A8)
        ReadingAmbience.SCIENCE_FICTION -> Color(0xFF102A43) to Color(0xFF5C3D99)
        ReadingAmbience.ROMANCE -> Color(0xFFC66B82) to Color(0xFFF0B67F)
        ReadingAmbience.NATURE -> Color(0xFF3F704D) to Color(0xFF77A6B6)
        ReadingAmbience.KNOWLEDGE -> Color(0xFF315B78) to Color(0xFFC49A45)
        ReadingAmbience.NEUTRAL -> Color(0xFF6D7280) to Color(0xFF9B8065)
    }

@Composable
private fun AmbientAudioControl(
    soundscape: AmbientSoundscape,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val soundscapeName = ambientSoundscapeName(soundscape)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onTogglePlayback, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription =
                        if (isPlaying) {
                            stringResource(R.string.pause_ambient_music, soundscapeName)
                        } else {
                            stringResource(R.string.play_ambient_music, soundscapeName)
                        },
                )
            }
        }
    }
}

@Composable
private fun AmbientAudioSettings(
    enabled: Boolean,
    soundscape: AmbientSoundscape,
    volumePercent: Int,
    onEnabled: (Boolean) -> Unit,
    onSoundscape: (AmbientSoundscape) -> Unit,
    onVolume: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ambient_music), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.ambient_music_hint),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabled)
    }
    AnimatedVisibility(visible = enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AmbientSoundscapeSettings(
                soundscape = soundscape,
                onSoundscape = onSoundscape,
            )
            AmbientVolumeSettings(
                volumePercent = volumePercent,
                onVolume = onVolume,
            )
        }
    }
}

@Composable
private fun AmbientSoundscapeSettings(
    soundscape: AmbientSoundscape,
    onSoundscape: (AmbientSoundscape) -> Unit,
) {
    Text(
        stringResource(R.string.ambient_music_soundscape),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AmbientSoundscape.entries.chunked(2).forEach { optionRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                optionRow.forEach { option ->
                    FilterChip(
                        selected = soundscape == option,
                        onClick = { onSoundscape(option) },
                        label = { Text(ambientSoundscapeName(option), maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (optionRow.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AmbientVolumeSettings(
    volumePercent: Int,
    onVolume: (Int) -> Unit,
) {
    var volumeValue by remember(volumePercent) { mutableFloatStateOf(volumePercent.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.ambient_music_volume),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.ambient_music_volume_value, volumeValue.roundToInt()),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Slider(
        value = volumeValue,
        onValueChange = { volumeValue = (it / AMBIENT_VOLUME_STEP).roundToInt() * AMBIENT_VOLUME_STEP },
        onValueChangeFinished = { onVolume(volumeValue.roundToInt()) },
        valueRange = MINIMUM_AMBIENT_VOLUME..MAXIMUM_AMBIENT_VOLUME,
        steps = AMBIENT_VOLUME_SLIDER_STEPS,
    )
    Text(
        stringResource(R.string.ambient_music_manual_start_hint),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ambientSoundscapeName(soundscape: AmbientSoundscape): String =
    when (soundscape) {
        AmbientSoundscape.CONCENTRATION -> stringResource(R.string.ambient_music_concentration)
        AmbientSoundscape.RAIN -> stringResource(R.string.ambient_music_rain)
        AmbientSoundscape.BROWN_NOISE -> stringResource(R.string.ambient_music_brown_noise)
        AmbientSoundscape.QUIET_NIGHT -> stringResource(R.string.ambient_music_quiet_night)
    }

@Composable
private fun ReaderControls(
    state: ReaderState,
    onToggleFocus: () -> Unit,
    onMoveBlock: (Int) -> Unit,
    onPageTheme: (PageTheme) -> Unit,
    onFontStyle: (ReaderFontStyle) -> Unit,
    onFontSize: (Int) -> Unit,
    onFocusDimming: (Int) -> Unit,
    onShowFocusMascot: (Boolean) -> Unit,
    onFocusPresentation: (FocusPresentation) -> Unit,
    onNarratorAvatar: (NarratorAvatar) -> Unit,
    onImportCustomAvatar: (Uri) -> Unit,
    onDeleteCustomAvatar: () -> Unit,
    onReaderControlsExpanded: (Boolean) -> Unit,
    onAmbientIntensity: (AmbientIntensity) -> Unit,
    onAmbientAudioEnabled: (Boolean) -> Unit,
    onAmbientSoundscape: (AmbientSoundscape) -> Unit,
    onAmbientAudioVolume: (Int) -> Unit,
) {
    val customAvatarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onImportCustomAvatar)
        }
    var focusDimmingValue by remember(state.settings.focusDimmingPercent) {
        mutableFloatStateOf(state.settings.focusDimmingPercent.toFloat())
    }
    if (!state.settings.readerControlsExpanded) {
        CollapsedReaderMenuHandle(
            focusEnabled = state.focusEnabled,
            showMascot = state.settings.showFocusMascot,
            narratorMode = state.settings.focusPresentation == FocusPresentation.OCTI_NARRATOR,
            onMoveBlock = onMoveBlock,
            onExpand = { onReaderControlsExpanded(true) },
        )
        return
    }
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.62f
    Surface(tonalElevation = 6.dp) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxMenuHeight)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.focus_mode), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.focus_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onReaderControlsExpanded(false) }) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        stringResource(R.string.hide_reader_menu),
                    )
                }
                Switch(checked = state.focusEnabled, onCheckedChange = { onToggleFocus() })
            }
            Spacer(Modifier.height(8.dp))
            AmbientAudioSettings(
                enabled = state.settings.ambientAudioEnabled,
                soundscape = state.settings.ambientSoundscape,
                volumePercent = state.settings.ambientAudioVolumePercent,
                onEnabled = onAmbientAudioEnabled,
                onSoundscape = onAmbientSoundscape,
                onVolume = onAmbientAudioVolume,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.focus_presentation),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FocusPresentation.entries.forEach { presentation ->
                    val label =
                        when (presentation) {
                            FocusPresentation.TEXT_MARKER -> stringResource(R.string.focus_presentation_marker)
                            FocusPresentation.OCTI_NARRATOR -> stringResource(R.string.focus_presentation_narrator)
                        }
                    FilterChip(
                        selected = state.settings.focusPresentation == presentation,
                        onClick = { onFocusPresentation(presentation) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.settings.focusPresentation == FocusPresentation.OCTI_NARRATOR) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.focus_narrator_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.narrator_avatar),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NarratorAvatar.entries.chunked(2).forEach { avatarRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                avatarRow.forEach { avatar ->
                                    val label =
                                        when (avatar) {
                                            NarratorAvatar.OCTI -> stringResource(R.string.narrator_avatar_octi)
                                            NarratorAvatar.LOVECRAFT_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_lovecraft)
                                            NarratorAvatar.SCHOPENHAUER_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_schopenhauer)
                                            NarratorAvatar.NIETZSCHE_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_nietzsche)
                                            NarratorAvatar.CAMUS_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_camus)
                                            NarratorAvatar.STRANGER_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_stranger)
                                            NarratorAvatar.LILA_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_lila)
                                            NarratorAvatar.ACHU_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_achu)
                                            NarratorAvatar.FRANK_N_FURTER_ILLUSTRATION ->
                                                stringResource(R.string.narrator_avatar_frank_n_furter)
                                            NarratorAvatar.CUSTOM_IMAGE ->
                                                stringResource(R.string.narrator_avatar_custom)
                                        }
                                    FilterChip(
                                        selected = state.settings.narratorAvatar == avatar,
                                        onClick = {
                                            if (avatar == NarratorAvatar.CUSTOM_IMAGE &&
                                                state.customNarratorAvatarPath == null
                                            ) {
                                                customAvatarLauncher.launch(
                                                    arrayOf("image/png", "image/jpeg", "image/webp"),
                                                )
                                            } else {
                                                onNarratorAvatar(avatar)
                                            }
                                        },
                                        label = { Text(label, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    if (state.settings.narratorAvatar == NarratorAvatar.CUSTOM_IMAGE) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = {
                                    customAvatarLauncher.launch(
                                        arrayOf("image/png", "image/jpeg", "image/webp"),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.change_custom_avatar))
                            }
                            TextButton(onClick = onDeleteCustomAvatar) {
                                Text(
                                    stringResource(R.string.delete_custom_avatar),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.narrator_avatar_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.reading_ambience),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AmbientIntensity.entries.forEach { intensity ->
                            val label =
                                when (intensity) {
                                    AmbientIntensity.OFF -> stringResource(R.string.reading_ambience_off)
                                    AmbientIntensity.SUBTLE -> stringResource(R.string.reading_ambience_subtle)
                                    AmbientIntensity.IMMERSIVE -> stringResource(R.string.reading_ambience_immersive)
                                }
                            FilterChip(
                                selected = state.settings.ambientIntensity == intensity,
                                onClick = { onAmbientIntensity(intensity) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.reading_ambience_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.page_theme),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PageTheme.entries.forEach { pageTheme ->
                    val label =
                        when (pageTheme) {
                            PageTheme.LIGHT -> stringResource(R.string.page_theme_light)
                            PageTheme.SEPIA -> stringResource(R.string.page_theme_sepia)
                            PageTheme.DARK -> stringResource(R.string.page_theme_dark)
                        }
                    FilterChip(
                        selected = state.settings.pageTheme == pageTheme,
                        onClick = { onPageTheme(pageTheme) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FontSettings(
                fontStyle = state.settings.fontStyle,
                fontSizeSp = state.settings.fontSizeSp,
                onFontStyle = onFontStyle,
                onFontSize = onFontSize,
            )
            AnimatedVisibility(state.settings.focusPresentation == FocusPresentation.TEXT_MARKER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.classic_marker_options),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.focus_dimming),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.focus_dimming_hint),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.focus_dimming_value, focusDimmingValue.roundToInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Slider(
                        value = focusDimmingValue,
                        onValueChange = { focusDimmingValue = (it / 5).roundToInt() * 5f },
                        onValueChangeFinished = { onFocusDimming(focusDimmingValue.roundToInt()) },
                        valueRange = 0f..80f,
                        steps = 15,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.focus_mascot),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.focus_mascot_hint),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.settings.showFocusMascot,
                            onCheckedChange = onShowFocusMascot,
                        )
                    }
                }
            }
            AnimatedVisibility(state.focusEnabled) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.gesture_navigation_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onMoveBlock(-1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_block))
                        }
                        IconButton(onClick = { onMoveBlock(1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_block))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSettings(
    fontStyle: ReaderFontStyle,
    fontSizeSp: Int,
    onFontStyle: (ReaderFontStyle) -> Unit,
    onFontSize: (Int) -> Unit,
) {
    Text(
        stringResource(R.string.font_style),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReaderFontStyle.entries.forEach { style ->
            val label =
                when (style) {
                    ReaderFontStyle.SERIF -> stringResource(R.string.font_style_serif)
                    ReaderFontStyle.SANS_SERIF -> stringResource(R.string.font_style_sans_serif)
                    ReaderFontStyle.MONOSPACE -> stringResource(R.string.font_style_monospace)
                }
            FilterChip(
                selected = fontStyle == style,
                onClick = { onFontStyle(style) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        fontFamily = readerFontFamily(style),
                        fontWeight = if (fontStyle == style) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            stringResource(R.string.font_preview),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontFamily = readerFontFamily(fontStyle),
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.font_size),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = { onFontSize(fontSizeSp - 1) },
            enabled = fontSizeSp > 14,
        ) {
            Icon(Icons.Rounded.Remove, stringResource(R.string.decrease_font_size))
        }
        Text(stringResource(R.string.font_size_value, fontSizeSp), fontWeight = FontWeight.SemiBold)
        IconButton(
            onClick = { onFontSize(fontSizeSp + 1) },
            enabled = fontSizeSp < 32,
        ) {
            Icon(Icons.Rounded.Add, stringResource(R.string.increase_font_size))
        }
    }
}

private fun readerFontFamily(style: ReaderFontStyle): FontFamily =
    when (style) {
        ReaderFontStyle.SERIF -> LoraFontFamily
        ReaderFontStyle.SANS_SERIF -> RobotoFontFamily
        ReaderFontStyle.MONOSPACE -> RobotoMonoFontFamily
    }

private val LoraFontFamily = FontFamily(Font(R.font.lora))
private val RobotoFontFamily = FontFamily(Font(R.font.roboto))
private val RobotoMonoFontFamily = FontFamily(Font(R.font.roboto_mono))

@Composable
private fun CollapsedReaderMenuHandle(
    focusEnabled: Boolean,
    showMascot: Boolean,
    narratorMode: Boolean,
    onMoveBlock: (Int) -> Unit,
    onExpand: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (focusEnabled && showMascot && !narratorMode) 116.dp else 52.dp)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (focusEnabled) {
            Surface(
                modifier = Modifier.align(Alignment.CenterStart),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMoveBlock(-1) }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            stringResource(R.string.previous_block),
                        )
                    }
                    IconButton(onClick = { onMoveBlock(1) }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            stringResource(R.string.next_block),
                        )
                    }
                }
            }
        }
        if (focusEnabled && showMascot && !narratorMode) {
            FocusReadingMascot(
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            IconButton(onClick = onExpand, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Rounded.ExpandLess,
                    stringResource(R.string.show_reader_menu),
                )
            }
        }
    }
}

@Composable
private fun SessionResultScreen(
    summary: ReadingSessionSummary,
    previousReadings: List<CompletedReading>,
    restartAvailable: Boolean,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    BackHandler(onBack = onFinish)
    Scaffold(
        topBar = {
            Surface(color = Color(0xFFF3D293), shadowElevation = 4.dp) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        stringResource(R.string.library_brand),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.session_complete),
                        color = Color(0xFF76511F),
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFFF3D293), shadowElevation = 8.dp) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    if (restartAvailable) {
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(stringResource(R.string.restart_book))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(stringResource(R.string.return_library))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .woodLibraryBackground(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    top = 20.dp,
                    end = 18.dp,
                    bottom = 28.dp,
                ),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFFF9E8C5),
                    shadowElevation = 10.dp,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            SessionBookCover(
                                title = summary.bookTitle,
                                coverImagePath = summary.coverImagePath,
                                progress = summary.progress,
                                modifier = Modifier.width(110.dp).height(148.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    summary.bookTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF352817),
                                    fontFamily = LoraFontFamily,
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Text(
                            stringResource(R.string.session_metrics_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF352817),
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionMetricPair(
                            firstLabel = stringResource(R.string.metric_time),
                            firstValue = formatDuration(summary.elapsedMillis),
                            secondLabel = stringResource(R.string.metric_fragments),
                            secondValue = stringResource(R.string.fragments_value, summary.fragmentsRead),
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionMetricPair(
                            firstLabel = stringResource(R.string.metric_words),
                            firstValue = stringResource(R.string.words_value, summary.wordsRead),
                            secondLabel = stringResource(R.string.metric_speed),
                            secondValue = stringResource(R.string.wpm_value, summary.averageWordsPerMinute),
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionMetricPair(
                            firstLabel = stringResource(R.string.metric_pauses),
                            firstValue = summary.pauses.toString(),
                            secondLabel = stringResource(R.string.metric_backwards),
                            secondValue = summary.backwardsMoves.toString(),
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionProgressMetric(summary.progress)
                        Spacer(Modifier.height(18.dp))
                        Text(
                            stringResource(R.string.session_body),
                            color = Color(0xFF6C5B43),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            if (previousReadings.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.previous_readings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9E8C5),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(previousReadings.withIndex().toList()) { indexedReading ->
                    PreviousReadingCard(
                        readingNumber = previousReadings.size - indexedReading.index,
                        reading = indexedReading.value,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviousReadingCard(
    readingNumber: Int,
    reading: CompletedReading,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF9E8C5),
        shadowElevation = 5.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.previous_reading_number, readingNumber),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF352817),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatDuration(reading.elapsedMillis), color = Color(0xFF6C5B43))
                Text(
                    stringResource(R.string.words_value, reading.wordsRead),
                    color = Color(0xFF6C5B43),
                )
                Text(
                    stringResource(R.string.wpm_value, reading.averageWordsPerMinute),
                    color = Color(0xFF6C5B43),
                )
            }
        }
    }
}

@Composable
private fun SessionBookCover(
    title: String,
    coverImagePath: String?,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val palette = bookCoverPalette(title)
    val coverBitmap by produceState<ImageBitmap?>(null, coverImagePath) {
        value =
            withContext(Dispatchers.IO) {
                coverImagePath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
            }
    }
    Surface(
        modifier = modifier.shadow(6.dp, RoundedCornerShape(3.dp)),
        shape = RoundedCornerShape(3.dp),
        color = palette.first,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = stringResource(R.string.book_cover, title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        color = palette.second,
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = palette.second.copy(alpha = 0.82f),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(5.dp),
                color = if (coverBitmap == null) palette.second else MaterialTheme.colorScheme.primary,
                trackColor =
                    if (coverBitmap == null) {
                        palette.second.copy(alpha = 0.2f)
                    } else {
                        Color.Black.copy(alpha = 0.35f)
                    },
            )
        }
    }
}

@Composable
private fun SessionMetricPair(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SessionMetricTile(firstLabel, firstValue, Modifier.weight(1f))
        SessionMetricTile(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun SessionMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 86.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF3DB),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, color = Color(0xFF765F40), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = Color(0xFF215A3D),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SessionProgressMetric(progress: Float) {
    val percentage = (progress.coerceIn(0f, 1f) * 100).roundToInt()
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF3DB)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.metric_progress), color = Color(0xFF765F40), fontSize = 12.sp)
                Text(
                    stringResource(R.string.percentage_value, percentage),
                    color = Color(0xFF215A3D),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF215A3D),
                trackColor = Color(0xFFD6C5A4),
            )
        }
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        stringResource(R.string.minutes_short, minutes, seconds)
    } else {
        stringResource(R.string.seconds_short, seconds)
    }
}
