package com.example.re_pro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LrcLine(val timeMs: Long, val text: String)

private val LRC_TIME_REGEX = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[\\.:](\\d{1,3}))?\\]")
private val LRC_OFFSET_REGEX = Regex("\\[offset:\\s*([+-]?\\d+)\\]", RegexOption.IGNORE_CASE)

fun parseLrcText(lrcRaw: String): List<LrcLine> {
    if (lrcRaw.isBlank()) return emptyList()
    val globalOffset = LRC_OFFSET_REGEX.find(lrcRaw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    val lines = mutableListOf<LrcLine>()

    lrcRaw.lines().forEach { line ->
        val timestamps = LRC_TIME_REGEX.findAll(line).toList()
        if (timestamps.isEmpty()) return@forEach
        val text = line.replace(LRC_TIME_REGEX, "").trim()
        if (text.isEmpty()) return@forEach
        timestamps.forEach { match ->
            val (min, sec, frac) = match.destructured
            val minMs = min.toLong() * 60 * 1000
            val secMs = sec.toLong() * 1000
            val fracMs = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            val time = (minMs + secMs + fracMs + globalOffset).coerceAtLeast(0L)
            lines.add(LrcLine(time, text))
        }
    }
    return lines.sortedBy { it.timeMs }
}

@Composable
fun LyricsView(
    lyricsRaw: String,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    dimTextColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    syncLeadMs: Long = 220L,
    onSeekToLine: ((timeMs: Long) -> Unit)? = null
) {
    val parsedLines = remember(lyricsRaw) { parseLrcText(lyricsRaw) }
    val listState = rememberLazyListState()

    val activeIndex = remember(currentPositionMs, parsedLines, syncLeadMs) {
        if (parsedLines.isEmpty()) -1
        else parsedLines.indexOfLast { it.timeMs <= currentPositionMs + syncLeadMs }
    }

    LaunchedEffect(activeIndex, parsedLines) {
        if (activeIndex >= 0) {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                val targetOffset = -(viewportHeight * 0.35f).toInt()
                val first = listState.firstVisibleItemIndex
                if (kotlin.math.abs(activeIndex - first) > 8) {
                    listState.scrollToItem(activeIndex, targetOffset)
                } else {
                    try {
                        listState.animateScrollToItem(activeIndex, targetOffset)
                    } catch (_: Exception) {
                        listState.scrollToItem(activeIndex, targetOffset)
                    }
                }
            }
        }
    }

    when {
        parsedLines.isEmpty() && lyricsRaw.isBlank() -> EmptyLyrics(modifier)
        parsedLines.isEmpty() -> PlainLyrics(lyricsRaw, modifier, baseTextColor)
        else -> SyncedLyrics(
            lines = parsedLines,
            activeIndex = activeIndex,
            currentPositionMs = currentPositionMs,
            accentColor = accentColor,
            baseTextColor = baseTextColor,
            dimTextColor = dimTextColor,
            listState = listState,
            modifier = modifier,
            onSeekToLine = onSeekToLine
        )
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LrcLine>,
    activeIndex: Int,
    currentPositionMs: Long,
    accentColor: Color,
    baseTextColor: Color,
    dimTextColor: Color,
    listState: LazyListState,
    modifier: Modifier,
    onSeekToLine: ((Long) -> Unit)?
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val fontSizeAnim by animateFloatAsState(
                targetValue = if (isActive) 20f else 16f,
                animationSpec = tween(160),
                label = "lyricFontSize"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) baseTextColor else dimTextColor,
                animationSpec = tween(160),
                label = "lyricTextColor"
            )
            val highlight by animateColorAsState(
                targetValue = if (isActive) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                animationSpec = tween(160),
                label = "lyricHighlight"
            )
            val lineWeight by animateFloatAsState(
                targetValue = if (isActive) 0f else 1f,
                animationSpec = tween(160),
                label = "lyricLineWeight"
            )
            val lineAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(160),
                label = "lyricLineAlpha"
            )

            val progressInLine = remember(lines, index, activeIndex, currentPositionMs, isActive) {
                if (!isActive) 0f
                else {
                    val start = lines[index].timeMs
                    val end = if (index + 1 < lines.size) lines[index + 1].timeMs else start + 6000L
                    val duration = (end - start).coerceAtLeast(1L)
                    ((currentPositionMs - start).toFloat() / duration).coerceIn(0f, 1f)
                }
            }

            val displayText = remember(line.text, progressInLine, isActive, accentColor) {
                if (!isActive) {
                    androidx.compose.ui.text.AnnotatedString(line.text)
                } else {
                    val fillCount = (line.text.length * progressInLine).toInt()
                        .coerceIn(0, line.text.length)
                    buildAnnotatedString {
                        if (fillCount > 0) {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = accentColor,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) { append(line.text.take(fillCount)) }
                        }
                        append(line.text.drop(fillCount))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 18.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor.copy(alpha = 0.85f * lineAlpha * (1f - lineWeight)))
                )
                Text(
                    text = displayText,
                    textAlign = TextAlign.Center,
                    fontSize = fontSizeAnim.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    lineHeight = if (isActive) 28.sp else 24.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeekToLine?.invoke(line.timeMs) }
                        .clip(RoundedCornerShape(14.dp))
                        .background(highlight)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun PlainLyrics(raw: String, modifier: Modifier, textColor: Color) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = raw,
            color = textColor,
            fontSize = 17.sp,
            lineHeight = 27.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyLyrics(modifier: Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lyrics,
                contentDescription = null,
                tint = onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Letra no disponible",
                color = onSurface.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Esta canción no tiene letra",
                color = onSurface.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }
    }
}