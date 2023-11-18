package com.jrtec.grabadora.Silero
import android.content.Context
import com.jrtec.grabadora.Silero.config.FrameSize
import com.jrtec.grabadora.Silero.config.Mode
import com.jrtec.grabadora.Silero.config.SampleRate

class Vad private constructor() {
    private lateinit var context: Context
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    fun setContext(context: Context): Vad = apply {
        this.context = context.applicationContext ?: context
    }

    fun setSampleRate(sampleRate: SampleRate): Vad = apply {
        this.sampleRate = sampleRate
    }

    fun setFrameSize(frameSize: FrameSize): Vad = apply {
        this.frameSize = frameSize
    }

    fun setMode(mode: Mode): Vad = apply {
        this.mode = mode
    }

    fun setSpeechDurationMs(speechDurationMs: Int): Vad = apply {
        this.speechDurationMs = speechDurationMs
    }

    fun setSilenceDurationMs(silenceDurationMs: Int): Vad = apply {
        this.silenceDurationMs = silenceDurationMs
    }

    /**
     * <p>
     * Builds and returns a VadModel instance based on the specified parameters.
     * </p>
     * @return The constructed VadModel.
     */
    fun build(): VadSilero {
        return VadSilero(
            context,
            sampleRate,
            frameSize,
            mode,
            speechDurationMs,
            silenceDurationMs
        )
    }

    companion object {
        @JvmStatic
        fun builder(): Vad {
            return Vad()
        }
    }
}