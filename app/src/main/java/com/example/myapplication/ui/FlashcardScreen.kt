package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Deck
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordState
import com.example.myapplication.ui.theme.MagooshAmber
import com.example.myapplication.ui.theme.MagooshAmberLight
import com.example.myapplication.ui.theme.MagooshBlue
import com.example.myapplication.ui.theme.MagooshBlueLight
import com.example.myapplication.ui.theme.MagooshGreen
import com.example.myapplication.ui.theme.MagooshGreenLight
import com.example.myapplication.ui.theme.MagooshPink

@Composable
fun FlashcardScreen(
    deck: Deck,
    progressStore: ProgressStore,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val currentWord = deck.words[currentIndex]
    val state = progressStore.stateOf(currentWord.word)
    val (mastered, reviewing, learning) = progressStore.countsFor(deck)
    val total = deck.words.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = deck.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The flashcard
            Flashcard(
                word = currentWord,
                state = state,
                flipped = flipped,
                onFlip = { flipped = !flipped }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons (only when flipped)
            if (flipped) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            progressStore.markKnew(currentWord.word)
                            flipped = false
                            currentIndex = (currentIndex + 1) % total
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MagooshGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✓ I knew this word", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            progressStore.markDidntKnow(currentWord.word)
                            flipped = false
                            currentIndex = (currentIndex + 1) % total
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MagooshPink),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✗ I didn't know this word", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress stats
            ProgressStat(
                label = "You have mastered $mastered of $total words",
                fraction = if (total == 0) 0f else mastered.toFloat() / total,
                color = MagooshGreen
            )
            ProgressStat(
                label = "You are reviewing $reviewing of $total words",
                fraction = if (total == 0) 0f else reviewing.toFloat() / total,
                color = MagooshAmber
            )
            ProgressStat(
                label = "You are learning $learning of $total words",
                fraction = if (total == 0) 0f else learning.toFloat() / total,
                color = MagooshBlue
            )
        }
    }
}

@Composable
private fun Flashcard(
    word: Word,
    state: WordState,
    flipped: Boolean,
    onFlip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable { onFlip() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            // Status badge
            StatusBadge(state = state, modifier = Modifier.align(Alignment.TopEnd))

            if (!flipped) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = word.word,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = Color(0xFFF2F2F2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tap to see meaning →",
                            color = Color(0xFF666666),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp)
                ) {
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
            }
        }
    }
}

@Composable
private fun StatusBadge(state: WordState, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (state) {
        WordState.MASTERED -> Triple("MASTERED", MagooshGreenLight, MagooshGreen)
        WordState.REVIEWING -> Triple("REVIEWING", MagooshAmberLight, MagooshAmber)
        WordState.LEARNING -> Triple("LEARNING", MagooshBlueLight, MagooshBlue)
        WordState.NEW -> Triple("NEW", Color(0xFFF2F2F2), Color(0xFF666666))
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
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
                .height(8.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
    }
}
