package com.example.swara_editor.videoeditor.utils

import com.example.swara_editor.videoeditor.models.SpeedCurvePoint

object SpeedCurveEngine {

    /**
     * Interpolates the playback speed for a normalized timestamp in the clip.
     */
    fun getSpeedAt(points: List<SpeedCurvePoint>, progress: Float): Float {
        if (points.isEmpty()) return 1.0f
        if (progress <= points.first().progress) return points.first().speed
        if (progress >= points.last().progress) return points.last().speed

        val nextIndex = points.indexOfFirst { it.progress > progress }
        if (nextIndex <= 0) return points.last().speed

        val p0 = points[nextIndex - 1]
        val p1 = points[nextIndex]

        val span = p1.progress - p0.progress
        if (span <= 0f) return p0.speed

        val t = (progress - p0.progress) / span
        // Smooth step interpolation
        val smoothT = t * t * (3f - 2f * t)
        return p0.speed + (p1.speed - p0.speed) * smoothT
    }

    /**
     * Calculates the new total timeline duration in milliseconds after applying the curve.
     */
    fun computeRampedDurationMs(originalDurationMs: Long, points: List<SpeedCurvePoint>, steps: Int = 100): Long {
        if (points.isEmpty()) return originalDurationMs
        var accumulatedTimeMs = 0.0
        val dt = 1.0 / steps
        for (i in 0 until steps) {
            val prog = ((i + 0.5f) * dt).toFloat()
            val speed = getSpeedAt(points, prog)
            val segmentDuration = (originalDurationMs * dt) / speed.coerceAtLeast(0.1f)
            accumulatedTimeMs += segmentDuration
        }
        return accumulatedTimeMs.toLong()
    }
}
