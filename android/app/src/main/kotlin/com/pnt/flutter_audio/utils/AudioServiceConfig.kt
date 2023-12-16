package com.pnt.flutter_audio.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import org.json.JSONObject

class AudioServiceConfig(context: Context) {
    private val preferences: SharedPreferences
    var androidResumeOnClick: Boolean
    var androidNotificationChannelId: String?
    var androidNotificationChannelName: String?
    var androidNotificationChannelDescription: String?
    var notificationColor: Int
    var androidNotificationIcon: String?
    var androidShowNotificationBadge: Boolean
    var androidNotificationClickStartsActivity: Boolean
    var androidNotificationOngoing: Boolean
    var androidStopForegroundOnPause: Boolean
    var artDownscaleWidth: Int
    var artDownscaleHeight: Int
    var activityClassName: String?
    var browsableRootExtras: String?

    init {
        preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        androidResumeOnClick = preferences.getBoolean(KEY_ANDROID_RESUME_ON_CLICK, true)
        androidNotificationChannelId =
            preferences.getString(KEY_ANDROID_NOTIFICATION_CHANNEL_ID, null)
        androidNotificationChannelName =
            preferences.getString(KEY_ANDROID_NOTIFICATION_CHANNEL_NAME, null)
        androidNotificationChannelDescription = preferences.getString(
            KEY_ANDROID_NOTIFICATION_CHANNEL_DESCRIPTION, null
        )
        notificationColor = preferences.getInt(KEY_NOTIFICATION_COLOR, -1)
        androidNotificationIcon =
            preferences.getString(KEY_ANDROID_NOTIFICATION_ICON, "mipmap/ic_launcher")
        androidShowNotificationBadge =
            preferences.getBoolean(KEY_ANDROID_SHOW_NOTIFICATION_BADGE, false)
        androidNotificationClickStartsActivity = preferences.getBoolean(
            KEY_ANDROID_NOTIFICATION_CLICK_STARTS_ACTIVITY, true
        )
        androidNotificationOngoing = preferences.getBoolean(KEY_ANDROID_NOTIFICATION_ONGOING, false)
        androidStopForegroundOnPause =
            preferences.getBoolean(KEY_ANDROID_STOP_FOREGROUND_ON_PAUSE, true)
        artDownscaleWidth = preferences.getInt(KEY_ART_DOWNSCALE_WIDTH, -1)
        artDownscaleHeight = preferences.getInt(KEY_ART_DOWNSCALE_HEIGHT, -1)
        activityClassName = preferences.getString(KEY_ACTIVITY_CLASS_NAME, null)
        browsableRootExtras = preferences.getString(KEY_BROWSABLE_ROOT_EXTRAS, null)
    }

    fun setBrowsableRootExtras(map: Map<*, *>?) {
        browsableRootExtras = if (map != null) {
            val json = JSONObject(map)
            json.toString()
        } else {
            null
        }
    }

    fun getBrowsableRootExtras(): Bundle? {
        return if (browsableRootExtras == null) null else try {
            val json = browsableRootExtras?.let { JSONObject(it) }
            val extras = Bundle()
            val it = json?.keys()
            if (it != null) {
                while (it.hasNext()) {
                    val key = it.next()
                    try {
                        extras.putInt(key, json.getInt(key))
                    } catch (e1: Exception) {
                        try {
                            extras.putBoolean(key, json.getBoolean(key))
                        } catch (e2: Exception) {
                            try {
                                extras.putDouble(key, json.getDouble(key))
                            } catch (e3: Exception) {
                                try {
                                    extras.putString(key, json.getString(key))
                                } catch (e4: Exception) {
                                    println("Unsupported extras value for key $key")
                                }
                            }
                        }
                    }
                }
            }
            extras
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun save() {
        preferences.edit()
            .putBoolean(KEY_ANDROID_RESUME_ON_CLICK, androidResumeOnClick)
            .putString(KEY_ANDROID_NOTIFICATION_CHANNEL_ID, androidNotificationChannelId)
            .putString(KEY_ANDROID_NOTIFICATION_CHANNEL_NAME, androidNotificationChannelName)
            .putString(
                KEY_ANDROID_NOTIFICATION_CHANNEL_DESCRIPTION,
                androidNotificationChannelDescription
            )
            .putInt(KEY_NOTIFICATION_COLOR, notificationColor)
            .putString(KEY_ANDROID_NOTIFICATION_ICON, androidNotificationIcon)
            .putBoolean(KEY_ANDROID_SHOW_NOTIFICATION_BADGE, androidShowNotificationBadge)
            .putBoolean(
                KEY_ANDROID_NOTIFICATION_CLICK_STARTS_ACTIVITY,
                androidNotificationClickStartsActivity
            )
            .putBoolean(KEY_ANDROID_NOTIFICATION_ONGOING, androidNotificationOngoing)
            .putBoolean(KEY_ANDROID_STOP_FOREGROUND_ON_PAUSE, androidStopForegroundOnPause)
            .putInt(KEY_ART_DOWNSCALE_WIDTH, artDownscaleWidth)
            .putInt(KEY_ART_DOWNSCALE_HEIGHT, artDownscaleHeight)
            .putString(KEY_ACTIVITY_CLASS_NAME, activityClassName)
            .putString(KEY_BROWSABLE_ROOT_EXTRAS, browsableRootExtras)
            .apply()
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "audio_service_preferences"
        private const val KEY_ANDROID_RESUME_ON_CLICK = "androidResumeOnClick"
        private const val KEY_ANDROID_NOTIFICATION_CHANNEL_ID = "androidNotificationChannelId"
        private const val KEY_ANDROID_NOTIFICATION_CHANNEL_NAME = "androidNotificationChannelName"
        private const val KEY_ANDROID_NOTIFICATION_CHANNEL_DESCRIPTION =
            "androidNotificationChannelDescription"
        private const val KEY_NOTIFICATION_COLOR = "notificationColor"
        private const val KEY_ANDROID_NOTIFICATION_ICON = "androidNotificationIcon"
        private const val KEY_ANDROID_SHOW_NOTIFICATION_BADGE = "androidShowNotificationBadge"
        private const val KEY_ANDROID_NOTIFICATION_CLICK_STARTS_ACTIVITY =
            "androidNotificationClickStartsActivity"
        private const val KEY_ANDROID_NOTIFICATION_ONGOING = "androidNotificationOngoing"
        private const val KEY_ANDROID_STOP_FOREGROUND_ON_PAUSE = "androidStopForegroundOnPause"
        private const val KEY_ART_DOWNSCALE_WIDTH = "artDownscaleWidth"
        private const val KEY_ART_DOWNSCALE_HEIGHT = "artDownscaleHeight"
        private const val KEY_ACTIVITY_CLASS_NAME = "activityClassName"
        private const val KEY_BROWSABLE_ROOT_EXTRAS = "androidBrowsableRootExtras"
    }
}