package com.example.rewind1137.audio

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val EMPTY_OUTPUT: ByteBuffer = ByteBuffer.allocateDirect(0).apply { flip() }

/**
 * Estado global de la eliminación de voz (modo karaoke).
 * Se lee desde el hilo de audio en cada buffer, por eso es volatile:
 * activar/desactivar surte efecto al instante, sin reconstruir el player.
 */
object VocalRemovalState {
    @Volatile
    var enabled: Boolean = false
}

/**
 * Renderers factory que inyecta [VocalRemovalAudioProcessor] dentro de DefaultAudioSink.
 * Sustituye a DefaultRenderersFactory.buildAudioSink añadiendo el procesador,
 * manteniendo el resto del pipeline de audio de Media3 intacto.
 */
class VocalRemovalRenderersFactory(context: Context) :
    DefaultRenderersFactory(context.applicationContext) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context.applicationContext)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(VocalRemovalAudioProcessor))
            .build()
    }
}

/**
 * Cancelación de canal central (voz) sobre audio PCM estéreo.
 * Soporta tanto PCM de 16 bits como PCM float (que es lo que usa Media3 por defecto),
 * porque si el sink entrega float y el procesador solo acepta 16 bits el efecto
 * queda inactivo de forma silenciosa y el botón "eliminar voz" no hace nada.
 * Cada trama (L, R) se convierte en (L - R) / 2, lo que anula el centro (voz/melodía
 * principal) y conserva los instrumentos distribuidos en estéreo.
 * Cuando [VocalRemovalState.enabled] es false se comporta como passthrough.
 */
object VocalRemovalAudioProcessor : AudioProcessor {

    private var inputFormat: AudioProcessor.AudioFormat? = null
    private var inputEnded = false
    private var outputBufferReturned = true
    private var buffer: ByteBuffer = EMPTY_OUTPUT

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        inputFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        val format = inputFormat ?: return false
        if (!VocalRemovalState.enabled || format.channelCount != 2) return false
        return format.encoding == C.ENCODING_PCM_16BIT || format.encoding == C.ENCODING_PCM_FLOAT
    }

    override fun queueInput(input: ByteBuffer) {
        val format = inputFormat ?: return
        if (input.remaining() < format.bytesPerFrame) return

        val frameCount = input.remaining() / format.bytesPerFrame
        if (!VocalRemovalState.enabled) {
            // Passthrough: copiamos la entrada tal cual.
            appendBuffer(input)
            return
        }

        ensureCapacity(input.remaining())
        buffer.clear()
        if (format.encoding == C.ENCODING_PCM_FLOAT) {
            val inputFloats = input.asFloatBuffer()
            val outputFloats = buffer.asFloatBuffer()
            for (i in 0 until frameCount) {
                val channel = i * 2
                val left = inputFloats.get(channel)
                val right = inputFloats.get(channel + 1)
                val vocalFree = (left - right) * 0.5f
                outputFloats.put(vocalFree)
                outputFloats.put(vocalFree)
            }
            buffer.position(outputFloats.position() * 4)
        } else {
            val inputShorts = input.asShortBuffer()
            val outputShorts = buffer.asShortBuffer()
            for (i in 0 until frameCount) {
                val channel = i * 2
                val left = inputShorts.get(channel).toInt()
                val right = inputShorts.get(channel + 1).toInt()
                val vocalFree = ((left - right) / 2).toShort()
                outputShorts.put(vocalFree)
                outputShorts.put(vocalFree)
            }
            buffer.position(outputShorts.position() * 2)
        }
        buffer.flip()
        outputBufferReturned = false
        input.position(input.position() + frameCount * format.bytesPerFrame)
    }

    private fun appendBuffer(input: ByteBuffer) {
        val remaining = input.remaining()
        if (remaining == 0) return
        ensureCapacity(remaining)
        buffer.clear()
        buffer.put(input)
        buffer.flip()
        outputBufferReturned = false
    }

    private fun ensureCapacity(size: Int) {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        if (outputBufferReturned) return EMPTY_OUTPUT
        outputBufferReturned = true
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBufferReturned

    override fun flush() {
        inputEnded = false
        outputBufferReturned = true
        buffer.clear()
    }

    override fun reset() {
        inputEnded = false
        outputBufferReturned = true
        buffer = EMPTY_OUTPUT
        inputFormat = null
    }
}