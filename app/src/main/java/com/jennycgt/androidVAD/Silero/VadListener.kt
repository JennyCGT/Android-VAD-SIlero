package com.jrtec.grabadora.Silero

interface VadListener {
    fun onSpeechDetected()
    fun onNoiseDetected()
}