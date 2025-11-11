package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private lateinit var counterTextView: TextView

    // Connection object to manage binding to the Service
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

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton  = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            if (isBound) {
                // Choose your starting value here (e.g., 10 seconds, 30, etc.)
                timerBinder?.start(10)
            }
        }

        stopButton.setOnClickListener {
            if (isBound) {
                timerBinder?.stop()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to TimerService
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