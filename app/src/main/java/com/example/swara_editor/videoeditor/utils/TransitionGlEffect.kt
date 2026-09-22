package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.swara_editor.videoeditor.models.TransitionType

@UnstableApi
class TransitionGlEffect(
    val type: TransitionType,
    val transitionStartUs: Long,
    val transitionDurationUs: Long,
    val isOutgoing: Boolean
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return TransitionShaderProgram(useHdr, type, transitionStartUs, transitionDurationUs, isOutgoing)
    }
}

@UnstableApi
private class TransitionShaderProgram(
    useHdr: Boolean,
    private val type: TransitionType,
    private val startUs: Long,
    private val durationUs: Long,
    private val isOutgoing: Boolean
) : BaseGlShaderProgram(useHdr, 1) {

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
            uniform float uProgress; // 0.0 to 1.0
            uniform int uType;       // 1: Fade Black, 2: Wipe/Slide
            varying vec2 vTexCoords;

            void main() {
                vec4 color = texture2D(uTexSampler, vTexCoords);
                if (uType == 1) {
                    // Fade to black ramp
                    color.rgb *= (1.0 - uProgress);
                } else if (uType == 2) {
                    // Wipe transition clip boundary
                    if (vTexCoords.x < uProgress) {
                        discard;
                    }
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
        val safeDuration = durationUs.coerceAtLeast(1L)
        val rawProgress = ((presentationTimeUs - startUs).toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        val progress = if (isOutgoing) rawProgress else (1.0f - rawProgress)

        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.setFloatUniform("uProgress", progress)
        val typeId = when (type) {
            TransitionType.FADE_TO_BLACK -> 1
            TransitionType.WIPE_RIGHT, TransitionType.SLIDE_LEFT -> 2
            else -> 0
        }
        glProgram.setIntUniform("uType", typeId)
        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
