package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Deck
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WidgetRefreshSettingsStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordState
import com.example.myapplication.data.WordPicker
import com.example.myapplication.ui.theme.AppThemeMode
import com.example.myapplication.ui.theme.DarkDangerBg
import com.example.myapplication.ui.theme.DarkDangerBgPressed
import com.example.myapplication.ui.theme.DarkDangerFg
import com.example.myapplication.ui.theme.DarkNewBg
import com.example.myapplication.ui.theme.DarkNewFg
import com.example.myapplication.ui.theme.DarkReviewingBg
import com.example.myapplication.ui.theme.DarkReviewingFg
import com.example.myapplication.ui.theme.DarkSuccessBg
import com.example.myapplication.ui.theme.DarkSuccessBgPressed
import com.example.myapplication.ui.theme.DarkSuccessFg
import com.example.myapplication.ui.theme.MagooshAmber
import com.example.myapplication.ui.theme.MagooshGreen
import com.example.myapplication.ui.theme.MagooshPink
import com.example.myapplication.ui.theme.OledChipBg
import com.example.myapplication.ui.theme.OledChipPressed
import com.example.myapplication.ui.theme.OledDangerFg
import com.example.myapplication.ui.theme.OledLearningFg
import com.example.myapplication.ui.theme.OledNewFg
import com.example.myapplication.ui.theme.OledReviewingFg
import com.example.myapplication.ui.theme.OledSuccessFg
import com.example.myapplication.ui.theme.currentThemeMode

@Composable
fun FlashcardScreen(
    deck: Deck,
    progressStore: ProgressStore,
    sessionStore: SessionStore,
    settingsStore: WidgetRefreshSettingsStore,
    onBack: () -> Unit
) {
    // Observe revisions so progress/session changes from the widget recompose this screen.
    progressStore.revision.intValue
    sessionStore.revision.intValue
    settingsStore.revision.intValue

    // Initialize from the shared session; re-keyed on session revision so a change
    // made anywhere (app or widget) is reflected here.
    var currentWord by remember(sessionStore.revision.intValue) {
        mutableStateOf(sessionStore.currentWord(deck.name, deck.words))
    }
    var flipped by remember(sessionStore.revision.intValue) {
        mutableStateOf(sessionStore.flipped(deck.name))
    }

    // Only advance when there are words; an empty deck must not divide by zero.
    val total = deck.words.size
    // Weighted-random next word (NEW/LEARNING/REVIEWING, skip MASTERED while possible).
    // Rebuilt when the user's per-state weights change so picks reflect the setting.
    val weights = settingsStore.stateWeights()
    val picker = remember(progressStore, weights) { WordPicker(progressStore::stateOf, weights = weights) }
    val advance: () -> Unit = {
        if (total > 0) {
            val next = picker.pickNext(deck.words, exclude = currentWord?.word)
            if (next != null) sessionStore.setCurrentWord(deck.name, next)
        }
    }
    // Seed the shared session on first ever open of this deck.
    LaunchedEffect(Unit) {
        if (!sessionStore.hasWord(deck.name)) {
            picker.pickNext(deck.words)?.let { sessionStore.setCurrentWord(deck.name, it) }
        }
    }
    // Guard against empty decks.
    if (total == 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No words in this deck yet",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    // Start with a random word on first composition.
    val word = currentWord ?: return
    val state = progressStore.stateOf(word.word)
    val (mastered, reviewing, learning) = progressStore.countsFor(deck)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: back + deck title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .semantics { contentDescription = "Back to deck list" }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = deck.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The flashcard
            Flashcard(
                word = word,
                state = state,
                flipped = flipped,
                onFlip = { sessionStore.setFlipped(deck.name, !flipped) },
                onKnew = {
                    progressStore.markKnew(word.word)
                    flipped = false
                    advance()
                },
                onDidntKnow = {
                    progressStore.markDidntKnow(word.word)
                    flipped = false
                    advance()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress stats (close to the card)
            ProgressStat(
                label = "You have mastered $mastered out of $total words",
                fraction = if (total == 0) 0f else mastered.toFloat() / total,
                color = MagooshGreen
            )
            ProgressStat(
                label = "You are reviewing $reviewing out of $total words",
                fraction = if (total == 0) 0f else reviewing.toFloat() / total,
                color = MagooshAmber
            )
            ProgressStat(
                label = "You are learning $learning out of $total words",
                fraction = if (total == 0) 0f else learning.toFloat() / total,
                color = MagooshPink
            )
        }
    }
}

/** Full-width tappable bottom bar; highlights only itself when pressed. */
@Composable
private fun TapBar(text: String, onTap: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "tapBarBg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Flashcard(
    word: Word,
    state: WordState,
    flipped: Boolean,
    onFlip: () -> Unit,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
        ) { constraints ->
            val faceConstraints = constraints.copy(
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            val front = subcompose("front") {
                FrontFace(word = word, state = state, onFlip = onFlip)
            }.single().measure(faceConstraints)
            val back = subcompose("back") {
                BackFace(
                    word = word,
                    state = state,
                    onKnew = onKnew,
                    onDidntKnow = onDidntKnow
                )
            }.single().measure(faceConstraints)
            val cardWidth = maxOf(front.width, back.width)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            val cardHeight = maxOf(front.height, back.height)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            val placementConstraints = faceConstraints.copy(
                minWidth = cardWidth,
                maxWidth = cardWidth,
                minHeight = cardHeight,
                maxHeight = cardHeight
            )
            val activeFace = subcompose(if (flipped) "back-placement" else "front-placement") {
                if (flipped) {
                    BackFace(
                        word = word,
                        state = state,
                        onKnew = onKnew,
                        onDidntKnow = onDidntKnow,
                        fillHeight = true
                    )
                } else {
                    FrontFace(
                        word = word,
                        state = state,
                        onFlip = onFlip,
                        fillHeight = true
                    )
                }
            }.single().measure(placementConstraints)

            layout(cardWidth, cardHeight) {
                activeFace.placeRelative(0, 0)
            }
        }
    }
}

@Composable
private fun FrontFace(
    word: Word,
    state: WordState,
    onFlip: () -> Unit,
    fillHeight: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusBadge(state = state, modifier = Modifier.align(Alignment.End))
        if (fillHeight) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }
        Text(
            text = word.word,
            modifier = Modifier.fillMaxWidth(),
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (fillHeight) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        TapBar(text = "Tap to see meaning →", onTap = onFlip)
    }
}

@Composable
private fun BackFace(
    word: Word,
    state: WordState,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit,
    fillHeight: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            StatusBadge(state = state, modifier = Modifier.align(Alignment.End))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = word.word,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = word.definition ?: "(definition coming soon)",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            word.example?.let { example ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "\"$example\"",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (fillHeight) {
            Spacer(modifier = Modifier.weight(1f))
        }
        KnewButton(onClick = onKnew)
        DidntKnowButton(onClick = onDidntKnow)
    }
}

@Composable
private fun KnewButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (bg, bgPressed, fg) = when (currentThemeMode()) {
        AppThemeMode.DARK -> Triple(DarkSuccessBg, DarkSuccessBgPressed, DarkSuccessFg)
        AppThemeMode.OLED -> Triple(OledChipBg, OledChipPressed, OledSuccessFg)
        AppThemeMode.LIGHT -> Triple(Color(0xFFBCF5CB), Color(0xFF96C4A3), Color(0xFF30B961))
    }
    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) bgPressed else bg,
        label = "knewBg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓ I knew this word",
            color = fg,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DidntKnowButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (bg, bgPressed, fg) = when (currentThemeMode()) {
        AppThemeMode.DARK -> Triple(DarkDangerBg, DarkDangerBgPressed, DarkDangerFg)
        AppThemeMode.OLED -> Triple(OledChipBg, OledChipPressed, OledDangerFg)
        AppThemeMode.LIGHT -> Triple(Color(0xFFFDCFD1), Color(0xFFCAA6A8), Color(0xFFC07571))
    }
    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) bgPressed else bg,
        label = "didntKnowBg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✗ I didn't know this word",
            color = fg,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun StatusBadge(state: WordState, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (currentThemeMode()) {
        AppThemeMode.DARK -> when (state) {
            WordState.MASTERED -> Triple("MASTERED", DarkSuccessBg, DarkSuccessFg)
            WordState.REVIEWING -> Triple("REVIEWING", DarkReviewingBg, DarkReviewingFg)
            WordState.LEARNING -> Triple("LEARNING", DarkDangerBg, DarkDangerFg)
            WordState.NEW -> Triple("NEW WORD", DarkNewBg, DarkNewFg)
        }
        AppThemeMode.OLED -> when (state) {
            WordState.MASTERED -> Triple("MASTERED", OledChipBg, OledSuccessFg)
            WordState.REVIEWING -> Triple("REVIEWING", OledChipBg, OledReviewingFg)
            WordState.LEARNING -> Triple("LEARNING", OledChipBg, OledLearningFg)
            WordState.NEW -> Triple("NEW WORD", OledChipBg, OledNewFg)
        }
        AppThemeMode.LIGHT -> when (state) {
            WordState.MASTERED -> Triple("MASTERED", Color(0xFFBAF5CA), Color(0xFF30B961))
            WordState.REVIEWING -> Triple("REVIEWING", Color(0xFFFFE3C2), Color(0xFFEBA15A))
            WordState.LEARNING -> Triple("LEARNING", Color(0xFFF9D1D2), Color(0xFFC07571))
            WordState.NEW -> Triple("NEW WORD", Color(0xFFF2F2F2), Color(0xFF666666))
        }
    }
    Surface(
        modifier = modifier,
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun ProgressStat(label: String, fraction: Float, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
