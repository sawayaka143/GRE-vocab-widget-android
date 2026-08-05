package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Deck
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.SettingsStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordState
import com.example.myapplication.data.WordPicker
import com.example.myapplication.ui.theme.MagooshAmber
import com.example.myapplication.ui.theme.MagooshBlue
import com.example.myapplication.ui.theme.MagooshGreen

@Composable
fun FlashcardScreen(
    deck: Deck,
    progressStore: ProgressStore,
    sessionStore: SessionStore,
    settingsStore: SettingsStore,
    onBack: () -> Unit
) {
    // Observe revisions so progress/session changes from the widget recompose this screen.
    progressStore.revision.intValue
    sessionStore.revision.intValue
    settingsStore.revision.intValue

    var showSettings by remember { mutableStateOf(false) }

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
    val picker = remember(progressStore) { WordPicker(progressStore::stateOf) }
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

    if (showSettings) {
        SettingsScreen(
            settingsStore = settingsStore,
            onBack = { showSettings = false }
        )
        return
    }

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
            // Top bar: back + deck title + settings gear
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "⚙",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { showSettings = true }
                        .semantics { contentDescription = "Settings" }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The flashcard
            Flashcard(
                word = word,
                state = state,
                flipped = flipped,
                showFlipBackBar = settingsStore.showFlipBackBar(),
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
                color = MagooshBlue
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
        targetValue = if (isPressed) Color(0xFFE0E0E0) else Color(0xFFF2F2F2),
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
            color = Color(0xFF666666),
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
    showFlipBackBar: Boolean,
    onFlip: () -> Unit,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!flipped) {
                // Front: badge + centered word + full-width bottom bar
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StatusBadge(state = state, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = word.word,
                        fontFamily = FontFamily.Serif,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }

                // Divider + full-width bottom bar (the flip tap target)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    TapBar(text = "Tap to see meaning →", onTap = onFlip)
                }
            } else {
                // Back: content (badge + word + definition) padded; buttons edge-to-edge
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    StatusBadge(state = state, modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = word.word,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.definition ?: "(definition coming soon)",
                        fontSize = 18.sp,
                        color = Color(0xFF333333)
                    )
                    word.example?.let { example ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "\"$example\"",
                            fontSize = 15.sp,
                            color = Color(0xFF555555)
                        )
                    }
                }

                // Bottom of the back face: optional "Tap to go back" bar, then action buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    if (showFlipBackBar) {
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        TapBar(text = "Tap to go back", onTap = onFlip)
                    }
                    // Top button: green, dark green text
                    val knewInteractionSource = remember { MutableInteractionSource() }
                    val knewIsPressed by knewInteractionSource.collectIsPressedAsState()
                    val knewBg by animateColorAsState(
                        targetValue = if (knewIsPressed) Color(0xFF96C4A3) else Color(0xFFBCF5CB),
                        label = "knewBg"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(knewBg)
                            .clickable(
                                interactionSource = knewInteractionSource,
                                indication = null,
                                onClick = onKnew
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ I knew this word",
                            color = Color(0xFF30B961),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Bottom button: pink, dark red text
                    val didntKnowInteractionSource = remember { MutableInteractionSource() }
                    val didntKnowIsPressed by didntKnowInteractionSource.collectIsPressedAsState()
                    val didntKnowBg by animateColorAsState(
                        targetValue = if (didntKnowIsPressed) Color(0xFFCAA6A8) else Color(0xFFFDCFD1),
                        label = "didntKnowBg"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(didntKnowBg)
                            .clickable(
                                interactionSource = didntKnowInteractionSource,
                                indication = null,
                                onClick = onDidntKnow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✗ I didn't know this word",
                            color = Color(0xFFC07571),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(state: WordState, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (state) {
        WordState.MASTERED -> Triple("MASTERED", Color(0xFFBAF5CA), Color(0xFF30B961))
        WordState.REVIEWING -> Triple("REVIEWING", Color(0xFFFFE3C2), Color(0xFFEBA15A))
        WordState.LEARNING -> Triple("LEARNING", Color(0xFFF9D1D2), Color(0xFFC07571))
        WordState.NEW -> Triple("NEW WORD", Color(0xFFF2F2F2), Color(0xFF666666))
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
private fun ProgressStat(label: String, fraction: Float, color: Color) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit
) {
    val showFlipBackBar = settingsStore.showFlipBackBar()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            // Top bar: back + title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .semantics { contentDescription = "Back to flashcard" }
                        .padding(4.dp)
                )
                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flip-back bar toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show \"Tap to go back\" on card back",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showFlipBackBar,
                    onCheckedChange = { settingsStore.setShowFlipBackBar(it) }
                )
            }
        }
    }
}
