package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private lateinit var counterTextView: TextView
    private lateinit var startPauseButton: Button
    private lateinit var stopButton: Button

    // Handler for receiving countdown values from the service
    private val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            counterTextView.text = msg.what.toString()
        }
    }

    // ServiceConnection to bind to TimerService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder?.setHandler(timerHandler)
            isBound = true
            updateStartButtonLabel()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            timerBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterTextView = findViewById(R.id.textView)
        startPauseButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startPauseButton.setOnClickListener {
            if (!isBound || timerBinder == null) return@setOnClickListener

            val binder = timerBinder!!

            when {
                // Service says: not running and not paused → start fresh
                !binder.isRunning && !binder.paused -> {
                    binder.start(10)          // Start countdown from 10
                    // In the service, start() will set isRunning = true, paused = false
                }

                // Service says: running and not paused → pause
                binder.isRunning && !binder.paused -> {
                    binder.pause()=
                }

                // Service says: paused → unpause
                binder.paused -> {
                    binder.start(10)
                }
            }

            updateStartButtonLabel()
        }

        stopButton.setOnClickListener {
            if (isBound && timerBinder != null) {
                timerBinder!!.stop()
                counterTextView.text = "0"
                updateStartButtonLabel()
            }
        }
    }

    private fun updateStartButtonLabel() {
        if (!isBound || timerBinder == null) {
            startPauseButton.text = "Start"
            return
        }

        val binder = timerBinder!!
        startPauseButton.text = when {
            !binder.isRunning && !binder.paused -> "Start"    // idle
            binder.paused -> "Unpause"                        // paused
            else -> "Pause"                                   // running
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}