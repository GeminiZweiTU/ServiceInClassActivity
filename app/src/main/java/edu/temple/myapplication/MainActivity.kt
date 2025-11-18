package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private lateinit var counterTextView: TextView

    private var startPauseMenuItem: MenuItem? = null
    private var stopMenuItem: MenuItem? = null

    // Handler for countdown updates
    private val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            counterTextView.text = msg.what.toString()
        }
    }

    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder?.setHandler(timerHandler)
            isBound = true
            updateMenuLabels()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            timerBinder = null
            updateMenuLabels()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterTextView = findViewById(R.id.textView)
    }

    // Menu Creation
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        startPauseMenuItem = menu.findItem(R.id.action_start_pause)
        stopMenuItem = menu.findItem(R.id.action_stop)
        updateMenuLabels()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_start_pause -> { handleStartPauseAction(); true }
            R.id.action_stop -> { handleStopAction(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleStartPauseAction() {
        if (!isBound || timerBinder == null) return
        val binder = timerBinder!!

        when {
            !binder.isRunning && !binder.paused -> binder.start(10)
            binder.isRunning && !binder.paused -> binder.pause()
            binder.paused -> binder.start(10)
        }

        updateMenuLabels()
    }

    private fun handleStopAction() {
        if (!isBound || timerBinder == null) return
        timerBinder!!.stop()
        counterTextView.text = "0"
        updateMenuLabels()
    }

    // Update action bar labels ("Start", "Pause", "Unpause")
    private fun updateMenuLabels() {
        val label = if (!isBound || timerBinder == null) {
            "Start"
        } else {
            val binder = timerBinder!!
            when {
                !binder.isRunning && !binder.paused -> "Start"
                binder.paused -> "Unpause"
                else -> "Pause"
            }
        }

        startPauseMenuItem?.title = label

        // Stop enabled only when running
        stopMenuItem?.isEnabled = isBound && timerBinder?.isRunning == true
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