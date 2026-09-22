package com.example.swara_editor.videoeditor.utils

import com.example.swara_editor.videoeditor.models.KeyframePoint

object KeyframeInterpolator {

    fun interpolate(
        keyframes: List<KeyframePoint>,
        currentTimeMs: Long,
        fallback: KeyframePoint = KeyframePoint(0L)
    ): KeyframePoint {
        if (keyframes.isEmpty()) return fallback
        if (keyframes.size == 1) return keyframes.first()

        // Before first keyframe
        if (currentTimeMs <= keyframes.first().timeMs) {
            return keyframes.first()
        }
        // After last keyframe
        if (currentTimeMs >= keyframes.last().timeMs) {
            return keyframes.last()
        }

        // Find surrounding keyframe pair
        val nextIndex = keyframes.indexOfFirst { it.timeMs > currentTimeMs }
        if (nextIndex <= 0) return keyframes.last()

        val prev = keyframes[nextIndex - 1]
        val next = keyframes[nextIndex]

        val span = (next.timeMs - prev.timeMs).toFloat()
        if (span <= 0f) return prev

        val rawProgress = ((currentTimeMs - prev.timeMs).toFloat() / span).coerceIn(0f, 1f)
        // Smooth ease-in-out Hermite curve
        val t = rawProgress * rawProgress * (3f - 2f * rawProgress)

        return KeyframePoint(
            timeMs = currentTimeMs,
            translationX = prev.translationX + (next.translationX - prev.translationX) * t,
            translationY = prev.translationY + (next.translationY - prev.translationY) * t,
            scale = prev.scale + (next.scale - prev.scale) * t,
            rotation = prev.rotation + (next.rotation - prev.rotation) * t,
            alpha = prev.alpha + (next.alpha - prev.alpha) * t
        )
    }
}
