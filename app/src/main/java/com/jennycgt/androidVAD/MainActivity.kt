package com.jennycgt.androidVAD

import VadListener
import VadSilero
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity(), VoiceRecorder.AudioCallback {

    private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K
    private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_512
    private val DEFAULT_MODE = Mode.NORMAL
    private val DEFAULT_SILENCE_DURATION_MS = 50
    private val DEFAULT_SPEECH_DURATION_MS = 100


    private lateinit var vad: VadSilero
    var vadText = mutableStateOf("")
    var isRecording = mutableStateOf(false)
    var isPlaying = mutableStateOf(false)
    var player: MediaPlayer? = null
    var archivo: File? = null
    var recorder_media: MediaRecorder? = null

    private lateinit var recorder: VoiceRecorder
    private lateinit var statusChangeDetector: StatusChangeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.INTERNET,
                ),
                1000
            )
        }
        vad = Vad.builder()
            .setContext(this)
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .build()
        recorder = VoiceRecorder(this)
        statusChangeDetector = StatusChangeDetector {
            executeFunction()
        }

        setContent {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    HomeScreen("VAD Android ", vadText.value,
                        isRecording.value,
                        isPlaying.value,
                        { isRecording.value = !it
                            if(isRecording.value){
                                startRecording()
                            }else{
                                stopRecording()
                            }
                        },
                        {isPlaying.value = !it
                            if(isPlaying.value){
                                startPlaying()
                            }}
                    )
                }

        }
    }
    override fun onAudio(audioData: ShortArray) {
            vad.setContinuousSpeechListener(audioData, object : VadListener {
            override fun onSpeechDetected() {
                vadText.value = getString(R.string.speech)
                statusChangeDetector.updateVariable("speech")
            }

            override fun onNoiseDetected() {
                statusChangeDetector.updateVariable("noise")
                vadText.value = getString(R.string.noise)

            }
        })
    }
    private fun startRecording() {
        recorder.start(vad.sampleRate.value, vad.frameSize.value)
        statusChangeDetector.startMonitoring()
        vadText.value = ""
        startRecordingFile()
    }

    private fun stopRecording() {
        recorder.stop()
        statusChangeDetector.stopMonitoring()
        stopRecordingFile()

    }

    private fun startRecordingFile() {
//        Toast.makeText(applicationContext, "Recording", Toast.LENGTH_LONG).show()
        val any = try {
            archivo = File.createTempFile("temporal", ".m4a", applicationContext.cacheDir)
        } catch (e: IOException) {

            Log.e("error archive", "$e")
        }
        recorder_media = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(archivo!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
                start()
            }catch (e:IllegalStateException ) {
                Log.e("error", "prepare() failed ${e.printStackTrace()}")
            } catch (e: IOException) {
                Log.e("error", "prepare() failed")
            }
        }
    }

    private fun stopRecordingFile() {
        recorder_media?.apply {
            stop()
            release()
        }
        recorder_media = null
        Toast.makeText(applicationContext, "Save", Toast.LENGTH_SHORT).show()

    }
    private fun executeFunction() {
        // This function will be called if the variable hasn't changed its value in the specified timeout
        stopRecording()
        isRecording.value = false
        vadText.value = ""
    }
    private fun startPlaying() {
        if(archivo !== null){
        player = MediaPlayer()
        try {
            Log.i("path playing",archivo!!.absolutePath)
            player!!.setDataSource(archivo!!.absolutePath)
        } catch (e: IOException) {
        }
        try {
            player!!.prepare()
        } catch (e: IOException) {
        }
        player?.start()
        player?.setOnCompletionListener {
            isPlaying.value = false
        }
        }
        else{
            isPlaying.value = false
            Toast.makeText(applicationContext, "There is not audio recorded", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun HomeScreen(name: String,
               vadText:String,
               isRecording: Boolean,
               isPlaying: Boolean,
               ChangeRecordStatus: (state:Boolean) -> Unit,
               ChangePlayStatus: (state:Boolean) -> Unit,
               modifier: Modifier= Modifier) {
//    var isPlaying by remember {mutableStateOf(false)}



    Box(modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.android),
            contentDescription = "Content",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "$name!",
        modifier = modifier,
        fontSize = 25.sp
    )

    Spacer(modifier = Modifier.height(20.dp))
    ButtonIcon(
        state = isRecording,
        onclick = {if(!isPlaying){ChangeRecordStatus(isRecording)}},
        resource1 = R.drawable.baseline_fiber_manual_record_24,
        resource2 = R.drawable.baseline_stop_circle_24,
        text1 = "Click to start Recording", text2 = "Recording ...")

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "$vadText",
        modifier = modifier,
        fontSize = 20.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    Divider(thickness = 5.dp, color = Color.Green)
    Spacer(modifier = Modifier.height(40.dp))
    ButtonIcon(state = isPlaying,
        onclick = { if(!isRecording){ChangePlayStatus(isPlaying)}},
        resource1 = R.drawable.baseline_play_circle_filled_24,
        resource2 = R.drawable.baseline_stop_circle_24,
        text1 = "Click to play", text2 = "Playing ...")

}

@SuppressLint("SuspiciousIndentation")
@Composable
fun ButtonIcon(
    state: Boolean,
    onclick:(state: Boolean)->Unit,
    resource1: Int ,
    resource2: Int ,
    text1 :String ,
    text2 : String
) {
    var iconRecord: Painter
    var iconColor: ColorFilter
    var textAction:String = if (!state) text1 else text2
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = textAction,
                fontSize = 15.sp
            )

            IconButton(
                onClick = {
                    onclick(state)
                },
                modifier = Modifier.size(100.dp),

            ) {
                iconRecord = if (!state) {
                    painterResource(id = resource1)
                } else {
                    painterResource(id = resource2)
                }
                iconColor = if(!state) ColorFilter.tint(Color.Black) else ColorFilter.tint(Color.Red)

                Image(
                    modifier = Modifier.size(100.dp),
                    painter = iconRecord, contentDescription = "Image",
                    contentScale = ContentScale.FillWidth,
                    colorFilter = iconColor
                )

            }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    VADAndroidTheme {
//        HomeScreen("Android", vadText = "noise",
//            Unit
//        )
//    }
//}