package com.wilson.assignment

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationActivity : AppCompatActivity() {
    companion object {
        const val INTENT_NOTIFICATION_ID = "notif_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent.getStringExtra(INTENT_NOTIFICATION_ID)?.let { id ->
            Notification.getNotification(id)
                    .addOnSuccessListener {
                        it?.run {
                            findViewById<TextView>(R.id.notificationFullTitle).text = title
                            findViewById<TextView>(R.id.notificationFullTime).text = formatTime()
                            findViewById<TextView>(R.id.notificationContent).text = content
                        } ?: errorToast("Notification format error", IllegalArgumentException())
                    }
                    .addOnFailureListener {
                        errorToast("Failed to retrieve id", it)
                    }
        } ?: errorToast("No notification id provided", IllegalArgumentException())
    }
}