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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Deck
import com.example.myapplication.data.FillInTheBlankQuestion
import com.example.myapplication.data.QuizProgressStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordPicker
import com.example.myapplication.data.makeQuestion
import com.example.myapplication.ui.theme.MagooshAmber
import com.example.myapplication.ui.theme.MagooshGreen
import com.example.myapplication.ui.theme.MagooshPink

/**
 * Fill-in-the-blank quiz for a deck. The sentence for the current word is shown
 * with the word blanked out, along with 5 choices (one correct). The question
 * word is picked with the same weighted-randomness [WordPicker] as the
 * flashcards; the 4 wrong-answer choices are picked uniformly from the deck.
 *
 * Layout mirrors the reference "GRE vocab" app:
 *  - Before answering: status badge, the sentence inside a white card (dark
 *    text on white, fixing the unreadable purple-on-dark bug), then choices.
 *  - After answering: a white result card (Correct/Incorrect header, the word
 *    with a speaker glyph, definition, italic example with the word bolded,
 *    and a grey "Next" bar), then a white card listing the distractor words,
 *    then the progress stats with light-grey tracks.
 */
@Composable
fun QuizScreen(
    deck: Deck,
    quizProgressStore: QuizProgressStore,
    onBack: () -> Unit
) {
    quizProgressStore.revision.intValue

    // The weighted picker for choosing which word's sentence to ask about.
    val picker = remember(quizProgressStore) { WordPicker(quizProgressStore::stateOf) }

    // State for the current question.
    var currentWord by remember { mutableStateOf<Word?>(null) }
    var question by remember { mutableStateOf<FillInTheBlankQuestion?>(null) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    var showWordCard by remember { mutableStateOf<Word?>(null) }

    // Build a fresh question for [word]: pick 4 uniform random distractors and shuffle.
    fun buildQuestion(word: Word): FillInTheBlankQuestion {
        val others = deck.words.filter { it.word != word.word }
        val distractors = others.shuffled().take(4)
        return makeQuestion(word, distractors)
    }

    // Seed the first question.
    LaunchedEffect(Unit) {
        picker.pickNext(deck.words)?.let { word ->
            currentWord = word
            question = buildQuestion(word)
        }
    }

    // Advance to the next question, avoiding the just-asked word.
    fun advance() {
        val current = currentWord ?: return
        picker.pickNext(deck.words, exclude = current.word)?.let { word ->
            currentWord = word
            question = buildQuestion(word)
        }
        selectedWord = null
        answered = false
        showWordCard = null
    }

    if (deck.words.isEmpty()) {
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

    val word = currentWord ?: return
    val q = question ?: return
    val state = quizProgressStore.stateOf(word.word)
    val (mastered, reviewing, learning) = quizProgressStore.countsFor(deck)

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
            // Top bar: back + deck name
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                // Keep the title centered by balancing the back button's width.
                Spacer(modifier = Modifier.width(0.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!answered) {
                    // ---------------- STATE A: question ----------------

                    // Status badge (same labels as flashcards).
                    StatusBadge(state = state)

                    Spacer(modifier = Modifier.height(16.dp))

                    // One white card: the sentence with the blank, then the
                    // choices as flat rows separated by dividers (reference
                    // "GRE vocab" before-answering layout).
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = q.sentence,
                                fontSize = 18.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F),
                                modifier = Modifier.padding(16.dp)
                            )

                            HorizontalDivider(color = Color(0xFFE0E0E0))

                            q.choices.forEachIndexed { index, choice ->
                                Text(
                                    text = choice.word.word,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1F1F1F),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!answered) {
                                                selectedWord = choice.word.word
                                                answered = true
                                                quizProgressStore.recordResult(
                                                    deckName = deck.name,
                                                    word = word.word,
                                                    correct = choice.isCorrect
                                                )
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                )
                                if (index < q.choices.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFE0E0E0))
                                }
                            }
                        }
                    }
                } else {
                    // ---------------- STATE B: result (reference layout) ----------------

                    ResultCard(
                        word = word,
                        correct = selectedWord == word.word,
                        onNext = { advance() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Learn the other words.
                    Text(
                        text = "Learn the meanings of the other words:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OtherWordsCard(
                        words = q.choices.filter { !it.isCorrect }.map { it.word },
                        onWordClick = { showWordCard = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Progress stats (shown in both states). Shared [ProgressStat] from
                // FlashcardScreen so quiz and flashcard bars look identical.
                ProgressStat(
                    label = "You have mastered $mastered out of ${deck.words.size} words",
                    fraction = if (deck.words.isEmpty()) 0f else mastered.toFloat() / deck.words.size,
                    color = MagooshGreen
                )
                ProgressStat(
                    label = "You are reviewing $reviewing out of ${deck.words.size} words",
                    fraction = if (deck.words.isEmpty()) 0f else reviewing.toFloat() / deck.words.size,
                    color = MagooshAmber
                )
                ProgressStat(
                    label = "You are learning $learning out of ${deck.words.size} words",
                    fraction = if (deck.words.isEmpty()) 0f else learning.toFloat() / deck.words.size,
                    color = MagooshPink
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Word-card popup for the "learn the other words" list.
    showWordCard?.let { other ->
        WordCardPopup(
            word = other,
            onClose = { showWordCard = null }
        )
    }
}

/**
 * The white result card shown after answering, matching the reference app:
 * Correct/Incorrect header, divider, the word with a speaker glyph,
 * definition (bold part-of-speech lead-in), italic example with the target
 * word bolded, divider, and a grey full-width "Next" bar.
 */
@Composable
private fun ResultCard(
    word: Word,
    correct: Boolean,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header.
            Text(
                text = if (correct) "Correct" else "Incorrect",
                color = if (correct) MagooshGreen else Color(0xFFD9534F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // Word + speaker + definition + example.
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = word.word,
                        fontSize = 28.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\uD83D\uDD0A", // 🔊 speaker glyph, decorative
                        fontSize = 20.sp,
                        modifier = Modifier.semantics { contentDescription = "Play audio" }
                    )
                }

                word.definition?.let {
                    Text(
                        text = buildDefinitionText(it),
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                word.example?.let {
                    Text(
                        text = buildExampleText(it, word.word),
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // Next bar (grey-on-grey like the reference, but clickable).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0))
                    .clickable(onClick = onNext)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Next",
                    color = Color(0xFF999999),
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * One white card listing the distractor words as rows separated by dividers;
 * each row is clickable and opens the word popup.
 */
@Composable
private fun OtherWordsCard(
    words: List<Word>,
    onWordClick: (Word) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            words.forEachIndexed { index, other ->
                Text(
                    text = other.word,
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWordClick(other) }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
                if (index < words.lastIndex) {
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

/**
 * Definition text with a bold part-of-speech lead-in (e.g. "adjective:")
 * when the definition starts with one, matching the reference app.
 */
private fun buildDefinitionText(definition: String): AnnotatedString = buildAnnotatedString {
    val match = Regex("^([A-Za-z-]+:)\\s*").find(definition)
    if (match != null) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        append(" ")
        append(definition.substring(match.range.last + 1))
    } else {
        append(definition)
    }
}

/**
 * Example sentence rendered fully italic, with every occurrence of the
 * target word bold-italic, matching the reference app.
 */
private fun buildExampleText(example: String, target: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        val lowerExample = example.lowercase()
        val lowerTarget = target.lowercase()
        var index = 0
        while (lowerTarget.isNotEmpty()) {
            val found = lowerExample.indexOf(lowerTarget, index)
            if (found < 0) break
            append(example.substring(index, found))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(example.substring(found, found + target.length))
            }
            index = found + target.length
        }
        append(example.substring(index))
    }
}

/** Overlay card showing a word's definition and example, dismissible via Close. */
@Composable
private fun WordCardPopup(word: Word, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = word.word,
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                word.definition?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center
                    )
                }
                word.example?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"$it\"",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}