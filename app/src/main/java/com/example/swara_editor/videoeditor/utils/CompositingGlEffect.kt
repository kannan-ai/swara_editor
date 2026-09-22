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
import com.example.swara_editor.videoeditor.models.ChromaKeySettings
import com.example.swara_editor.videoeditor.models.MaskSettings
import com.example.swara_editor.videoeditor.models.MaskType

@UnstableApi
class CompositingGlEffect(
    private val chroma: ChromaKeySettings,
    private val mask: MaskSettings
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return CompositingShaderProgram(useHdr, chroma, mask)
    }
}

@UnstableApi
private class CompositingShaderProgram(
    useHdr: Boolean,
    private val chroma: ChromaKeySettings,
    private val mask: MaskSettings
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
            varying vec2 vTexCoords;

            // Chroma Key Uniforms
            uniform int uChromaEnabled;
            uniform vec3 uKeyColor;
            uniform float uSimilarity;
            uniform float uSmoothness;

            // Mask Uniforms
            uniform int uMaskType; // 0: None, 1: Rect, 2: Circle, 3: Linear
            uniform vec2 uMaskCenter;
            uniform vec2 uMaskSize;
            uniform float uMaskFeather;
            uniform int uMaskInvert;

            void main() {
                vec4 color = texture2D(uTexSampler, vTexCoords);

                // 1. Chroma Key Pass
                if (uChromaEnabled == 1) {
                    float dist = distance(color.rgb, uKeyColor);
                    float alpha = smoothstep(uSimilarity, uSimilarity + uSmoothness, dist);
                    color.a *= alpha;
                }

                // 2. Geometric Mask Pass
                if (uMaskType > 0) {
                    float maskAlpha = 1.0;
                    vec2 d = abs(vTexCoords - uMaskCenter);

                    if (uMaskType == 1) { // Rectangle
                        vec2 edge = d - (uMaskSize * 0.5);
                        float distOutside = length(max(edge, 0.0));
                        maskAlpha = 1.0 - smoothstep(0.0, max(uMaskFeather, 0.001), distOutside);
                    } else if (uMaskType == 2) { // Circle / Ellipse
                        vec2 normDist = d / max(uMaskSize * 0.5, vec2(0.001));
                        float dist = length(normDist);
                        maskAlpha = 1.0 - smoothstep(1.0 - uMaskFeather, 1.0, dist);
                    } else if (uMaskType == 3) { // Linear Split
                        float lineDist = vTexCoords.y - uMaskCenter.y;
                        maskAlpha = smoothstep(-max(uMaskFeather, 0.001), max(uMaskFeather, 0.001), lineDist);
                    }

                    if (uMaskInvert == 1) {
                        maskAlpha = 1.0 - maskAlpha;
                    }
                    color.a *= clamp(maskAlpha, 0.0, 1.0);
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

        // Bind Chroma Parameters
        glProgram.setIntUniform("uChromaEnabled", if (chroma.isEnabled) 1 else 0)
        val r = ((chroma.keyColorHex shr 16) and 0xFF) / 255f
        val g = ((chroma.keyColorHex shr 8) and 0xFF) / 255f
        val b = (chroma.keyColorHex and 0xFF) / 255f
        glProgram.setFloatsUniform("uKeyColor", floatArrayOf(r, g, b))
        glProgram.setFloatUniform("uSimilarity", chroma.similarityThreshold)
        glProgram.setFloatUniform("uSmoothness", chroma.smoothness)

        // Bind Mask Parameters
        val mType = when (mask.type) {
            MaskType.RECTANGLE -> 1
            MaskType.CIRCLE -> 2
            MaskType.LINEAR, MaskType.MIRROR -> 3
            else -> 0
        }
        glProgram.setIntUniform("uMaskType", mType)
        glProgram.setFloatsUniform("uMaskCenter", floatArrayOf(mask.centerX, mask.centerY))
        glProgram.setFloatsUniform("uMaskSize", floatArrayOf(mask.width, mask.height))
        glProgram.setFloatUniform("uMaskFeather", mask.feather)
        glProgram.setIntUniform("uMaskInvert", if (mask.isInverted) 1 else 0)

        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
