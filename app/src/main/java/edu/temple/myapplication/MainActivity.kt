package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private lateinit var counterTextView: TextView
    private lateinit var startPauseButton: Button
    private lateinit var stopButton: Button

    // Menu action items
    private var startPauseMenuItem: MenuItem? = null
    private var stopMenuItem: MenuItem? = null

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
            updateStartButtonLabel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterTextView = findViewById(R.id.textView)
        startPauseButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startPauseButton.setOnClickListener {
            handleStartPauseAction()
        }

        stopButton.setOnClickListener {
            handleStopAction()
        }
    }

    // Menu Setup
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        startPauseMenuItem = menu.findItem(R.id.action_start_pause)
        stopMenuItem = menu.findItem(R.id.action_stop)
        updateStartButtonLabel()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_start_pause -> {
                handleStartPauseAction()
                true
            }
            R.id.action_stop -> {
                handleStopAction()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleStartPauseAction() {
        if (!isBound || timerBinder == null) return

        val binder = timerBinder!!

        when {
            // Service says: not running and not paused → start fresh
            !binder.isRunning && !binder.paused -> {
                binder.start(10)          // Start countdown from 10
            }

            // Service says: running and not paused → pause
            binder.isRunning && !binder.paused -> {
                binder.pause()
            }

            // Service says: paused → unpause
            binder.paused -> {
                // Your TimerBinder.start() unpauses if paused == true
                binder.start(10)
            }
        }

        updateStartButtonLabel()
    }

    private fun handleStopAction() {
        if (isBound && timerBinder != null) {
            timerBinder!!.stop()
            counterTextView.text = "0"
            updateStartButtonLabel()
        }
    }

    private fun updateStartButtonLabel() {
        val label = if (!isBound || timerBinder == null) {
            "Start"
        } else {
            val binder = timerBinder!!
            when {
                !binder.isRunning && !binder.paused -> "Start"    // idle
                binder.paused -> "Unpause"                        // paused
                else -> "Pause"                                   // running
            }
        }

        // Button label
        if (this::startPauseButton.isInitialized) {
            startPauseButton.text = label
        }

        // Menu item label
        startPauseMenuItem?.title = label

        //enable Stop based on running state
        val stopEnabled = isBound && timerBinder?.isRunning == true
        if (this::stopButton.isInitialized) {
            stopButton.isEnabled = stopEnabled
        }
        stopMenuItem?.isEnabled = stopEnabled
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