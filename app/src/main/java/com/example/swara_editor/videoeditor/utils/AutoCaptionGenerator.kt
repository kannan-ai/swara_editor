package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.net.Uri
import com.example.swara_editor.videoeditor.models.AutoCaptionTrack
import com.example.swara_editor.videoeditor.models.SubtitleWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AutoCaptionGenerator {

    suspend fun transcribeTimelineAudio(
        context: Context,
        audioTrackUri: Uri,
        totalDurationMs: Long
    ): List<AutoCaptionTrack> = withContext(Dispatchers.IO) {
        val captions = mutableListOf<AutoCaptionTrack>()

        val sampleSentences = listOf(
            "Welcome to Swara Editor" to 2500L,
            "Create high performance video edits" to 3200L,
            "Render and export with ease" to 2800L
        )

        var cursorMs = 500L
        sampleSentences.forEachIndexed { index, (sentence, duration) ->
            if (cursorMs >= totalDurationMs && totalDurationMs > 0L) return@forEachIndexed

            val words = sentence.split(" ")
            val wordDuration = duration / words.size
            val wordList = words.mapIndexed { wIdx, text ->
                SubtitleWord(
                    word = text,
                    startMs = cursorMs + (wIdx * wordDuration),
                    endMs = cursorMs + ((wIdx + 1) * wordDuration)
                )
            }

            captions.add(
                AutoCaptionTrack(
                    id = "caption_$index",
                    fullText = sentence,
                    startMs = cursorMs,
                    endMs = (cursorMs + duration).coerceAtMost(totalDurationMs.coerceAtLeast(3000L)),
                    words = wordList
                )
            )
            cursorMs += duration + 400L
        }

        captions
    }
}
