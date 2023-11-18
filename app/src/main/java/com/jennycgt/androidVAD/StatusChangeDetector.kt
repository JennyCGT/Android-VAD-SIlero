package com.jennycgt.androidVAD

import android.os.Handler
import android.os.Looper
import android.util.Log

class StatusChangeDetector(private val onChangeTimeout: Long = 3000,
                           private val callback: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastValue: String? = null


    private val runnable = Runnable {
        // Check if the variable hasn't changed its value for the specified timeout
        if (lastValue == "noise") {
            // Execute your function here
            executeFunction()
        }
    }

    fun startMonitoring() {
        handler.postDelayed(runnable, onChangeTimeout)
    }

    fun stopMonitoring() {
        handler.removeCallbacks(runnable)
        lastValue = null
    }

    fun updateVariable(value: String) {
        if (lastValue != value) {
            lastValue = value
//            Log.i("STATUS", "New Value $value")
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, onChangeTimeout)
        }
    }

    private fun executeFunction() {
        // Implement your desired functionality here
        // This function will be called if the variable hasn't changed its value in the specified timeout
        callback()
    }
}