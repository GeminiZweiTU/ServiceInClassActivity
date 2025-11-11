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

    // Handler for receiving updates from the Service
    private val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            counterTextView.text = msg.what.toString()
        }
    }

    // Connection to the TimerService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder?.setHandler(timerHandler)
            isBound = true
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
            if (isBound && timerBinder != null) {
                val binder = timerBinder!!

                when {
                    !binder.isRunning && !binder.paused -> {
                        // Timer is stopped → start it
                        binder.start(10)
                        startPauseButton.text = "Pause"
                    }

                    binder.isRunning && !binder.paused -> {
                        // Timer is running → pause it
                        binder.pause()
                        startPauseButton.text = "Unpause"
                    }

                    binder.paused -> {
                        // Timer is paused → unpause it
                        binder.pause()
                        startPauseButton.text = "Pause"
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            if (isBound && timerBinder != null) {
                timerBinder!!.stop()
                startPauseButton.text = "Start"
                counterTextView.text = "0"
            }
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