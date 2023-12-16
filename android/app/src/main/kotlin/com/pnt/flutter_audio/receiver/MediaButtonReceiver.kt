package com.pnt.flutter_audio.receiver

import android.content.Context
import android.content.Intent
import com.pnt.flutter_audio.service.AudioService

class MediaButtonReceiver : androidx.media.session.MediaButtonReceiver() {
    companion object {
        const val ACTION_NOTIFICATION_DELETE =
            "com.pnt.intent.action.ACTION_NOTIFICATION_DELETE"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == ACTION_NOTIFICATION_DELETE && AudioService.instance != null) {
            AudioService.instance!!.handleDeleteNotification()
            return
        }

        super.onReceive(context, intent)
    }
}