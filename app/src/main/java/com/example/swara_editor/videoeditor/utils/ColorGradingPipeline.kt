package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.media3.common.Effect
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import com.example.swara_editor.videoeditor.models.ColorGradingSettings

@UnstableApi
class ColorGradingMatrix(
    private val settings: ColorGradingSettings
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)

        // 1. Contrast & Brightness scaling
        val c = settings.contrast
        val b = settings.brightness
        val contrastBrightnessMatrix = floatArrayOf(
            c, 0f, 0f, 0f,
            0f, c, 0f, 0f,
            0f, 0f, c, 0f,
            b + (0.5f * (1f - c)), b + (0.5f * (1f - c)), b + (0.5f * (1f - c)), 1f
        )
        Matrix.multiplyMM(matrix, 0, matrix.clone(), 0, contrastBrightnessMatrix, 0)

        // 2. Saturation (Rec. 709 Luminance weights: 0.2126 R, 0.7152 G, 0.0722 B)
        val s = settings.saturation
        val rw = 0.2126f * (1f - s)
        val gw = 0.7152f * (1f - s)
        val bw = 0.0722f * (1f - s)
        val saturationMatrix = floatArrayOf(
            rw + s, rw, rw, 0f,
            gw, gw + s, gw, 0f,
            bw, bw, bw + s, 0f,
            0f, 0f, 0f, 1f
        )
        Matrix.multiplyMM(matrix, 0, matrix.clone(), 0, saturationMatrix, 0)

        // 3. Color Temperature (Amber Warmth vs Blue Coolness)
        val t = settings.temperature
        val tempMatrix = floatArrayOf(
            1.0f + (t * 0.25f), 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f,
            0f, 0f, 1.0f - (t * 0.25f), 0f,
            0f, 0f, 0f, 1f
        )
        Matrix.multiplyMM(matrix, 0, matrix.clone(), 0, tempMatrix, 0)

        return matrix
    }
}

@UnstableApi
class VignetteEffect(val intensity: Float) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return VignetteShaderProgram(useHdr, intensity)
    }
}

@UnstableApi
private class VignetteShaderProgram(useHdr: Boolean, private val intensity: Float) :
    BaseGlShaderProgram(useHdr, 1) {

    private val glProgram: GlProgram

    init {
        val vertexShader = """
            attribute vec4 aFramePosition;
            varying vec2 vTexCoords;
            void main() {
                gl_Position = aFramePosition;
                vTexCoords = (aFramePosition.xy + vec2(1.0)) * 0.5;
            }
        """.trimIndent()

        val fragmentShader = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform float uIntensity;
            varying vec2 vTexCoords;
            void main() {
                vec4 color = texture2D(uTexSampler, vTexCoords);
                if (uIntensity > 0.0) {
                    vec2 uv = vTexCoords - vec2(0.5);
                    float dist = length(uv);
                    float vignette = smoothstep(0.75, 0.75 - (0.45 * uIntensity), dist);
                    color.rgb *= vignette;
                }
                gl_FragColor = color;
            }
        """.trimIndent()

        glProgram = GlProgram(vertexShader, fragmentShader)
        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            4
        )
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.setFloatUniform("uIntensity", intensity)
        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}

@UnstableApi
fun buildGradingPipeline(settings: ColorGradingSettings): List<Effect> {
    val pipeline = mutableListOf<Effect>()

    // 1. Chained Brightness, Contrast, Saturation, and Warmth
    pipeline.add(ColorGradingMatrix(settings))

    // 2. Custom Vignette Shader (if enabled)
    if (settings.vignette > 0f) {
        pipeline.add(VignetteEffect(settings.vignette))
    }

    return pipeline
}

@UnstableApi
fun applyGradingToPlayer(player: ExoPlayer, settings: ColorGradingSettings) {
    player.setVideoEffects(buildGradingPipeline(settings))
    if (!player.isPlaying) {
        player.seekTo(player.currentPosition) // Refresh paused frame
    }
}
