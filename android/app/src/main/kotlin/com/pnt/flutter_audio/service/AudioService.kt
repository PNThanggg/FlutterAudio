package com.pnt.flutter_audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.LruCache
import android.util.Size
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.VolumeProviderCompat
import androidx.media.utils.MediaConstants
import com.pnt.flutter_audio.interfaces.ServiceListener
import com.pnt.flutter_audio.models.AudioProcessingState
import com.pnt.flutter_audio.models.MediaButton
import com.pnt.flutter_audio.models.MediaControl
import com.pnt.flutter_audio.receiver.MediaButtonReceiver
import com.pnt.flutter_audio.receiver.MediaButtonReceiver.Companion.ACTION_NOTIFICATION_DELETE
import com.pnt.flutter_audio.utils.AudioServiceConfig
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Arrays

class AudioService : MediaBrowserServiceCompat() {
    fun createMediaMetadata(
        mediaId: String?,
        title: String?,
        album: String?,
        artist: String?,
        genre: String?,
        duration: Long?,
        artUri: String?,
        playable: Boolean?,
        displayTitle: String?,
        displaySubtitle: String?,
        displayDescription: String?,
        rating: RatingCompat?,
        extras: Map<*, *>?
    ): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        if (album != null) builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        if (artist != null) builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        if (genre != null) builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
        if (duration != null) builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        if (artUri != null) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, artUri)
        }
        if (playable != null) builder.putLong("playable_long", (if (playable) 1 else 0).toLong())
        if (displayTitle != null) builder.putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
            displayTitle
        )
        if (displaySubtitle != null) builder.putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
            displaySubtitle
        )
        if (displayDescription != null) builder.putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
            displayDescription
        )
        if (rating != null) {
            builder.putRating(MediaMetadataCompat.METADATA_KEY_RATING, rating)
        }
        if (extras != null) {
            for (o in extras.keys) {
                val key = o as String?
                when (val value = extras[key]) {
                    is Long -> {
                        builder.putLong(key, (value as Long?)!!)
                    }

                    is Int -> {
                        builder.putLong(key, value.toLong())
                    }

                    is String -> {
                        builder.putString(key, value as String?)
                    }

                    is Boolean -> {
                        builder.putLong(key, (if (value) 1 else 0).toLong())
                    }

                    is Double -> {
                        builder.putString(key, value.toString())
                    }
                }
            }
        }
        val mediaMetadata = builder.build()
        mediaMetadataCache[mediaId] = mediaMetadata
        return mediaMetadata
    }

    private fun loadArtBitmap(artUriString: String, loadThumbnailUri: String?): Bitmap? {
        var bitmap = artBitmapCache!![artUriString]
        if (bitmap != null) return bitmap
        return try {
            // There are 3 cases handled by this function:
            //   1. content URI with openFileDescriptor
            //   2. content URI with loadThumbnail (when Android >= Q and specified by the config)
            //   3. not content URI - loading from the file, or cache file created by the Dart side
            val artUri = Uri.parse(artUriString)
            val usesContentScheme = "content" == artUri.scheme
            var fileDescriptor: FileDescriptor? = null
            if (usesContentScheme) {
                try {
                    if (loadThumbnailUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val defaultSize = Size(192, 192)
                        bitmap = contentResolver.loadThumbnail(
                            artUri,
                            Size(
                                if (audioServiceConfig.artDownscaleWidth == -1) defaultSize.width else audioServiceConfig.artDownscaleWidth,
                                if (audioServiceConfig.artDownscaleHeight == -1) defaultSize.height else audioServiceConfig.artDownscaleHeight
                            ),
                            null
                        )
                    } else {
                        val parcelFileDescriptor = contentResolver.openFileDescriptor(artUri, "r")
                        if (parcelFileDescriptor != null) {
                            fileDescriptor = parcelFileDescriptor.fileDescriptor
                        } else {
                            return null
                        }
                    }
                } catch (ex: FileNotFoundException) {
                    return null
                } catch (ex: IOException) {
                    return null
                }
            }
            // Decode the image ourselves for scenarios 1 and 3 (see the comment above).
            if (!usesContentScheme || fileDescriptor != null) {
                if (audioServiceConfig.artDownscaleWidth != -1) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    if (fileDescriptor != null) {
                        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
                    } else {
                        BitmapFactory.decodeFile(artUri.path, options)
                    }
                    options.inSampleSize = calculateInSampleSize(
                        options,
                        audioServiceConfig.artDownscaleWidth,
                        audioServiceConfig.artDownscaleHeight
                    )
                    options.inJustDecodeBounds = false
                    bitmap = if (fileDescriptor != null) {
                        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
                    } else {
                        BitmapFactory.decodeFile(artUri.path, options)
                    }
                } else {
                    bitmap = if (fileDescriptor != null) {
                        BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    } else {
                        BitmapFactory.decodeFile(artUri.path)
                    }
                }
            }
            artBitmapCache!!.put(artUriString, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private lateinit var audioServiceConfig: AudioServiceConfig
    private var wakeLock: WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionCallback: MediaSessionCallback? = null
    private var controls: MutableList<MediaControl> = ArrayList()
    private val nativeActions: MutableList<NotificationCompat.Action> = ArrayList()
    private val customActions: MutableList<PlaybackStateCompat.CustomAction> = ArrayList()
    private var compactActionIndices: IntArray? = null
    private var mediaMetadata: MediaMetadataCompat? = null
    private var artBitmap: Bitmap? = null
    private var notificationChannelId: String? = null
    private var artBitmapCache: LruCache<String, Bitmap?>? = null

    var isPlaying = false
        private set
    var processingState: AudioProcessingState = AudioProcessingState.IDLE
        private set
    var repeatMode = 0
        private set
    var shuffleMode = 0
        private set

    private var notificationCreated = false
    private val handler = Handler(Looper.getMainLooper())
    private var volumeProvider: VolumeProviderCompat? = null

    override fun onCreate() {
        super.onCreate()

        configure(AudioServiceConfig(applicationContext))

        instance = this

        repeatMode = 0
        shuffleMode = 0
        notificationCreated = false
        isPlaying = false
        processingState = AudioProcessingState.IDLE
        mediaSession = MediaSessionCompat(this, "media-session")
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(AUTO_ENABLED_ACTIONS)
        mediaSession!!.setPlaybackState(stateBuilder.build())
        mediaSession!!.setCallback(MediaSessionCallback().also {
            mediaSessionCallback = it
        })
        setSessionToken(mediaSession!!.sessionToken)
        mediaSession!!.setQueue(queue)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AudioService::class.java.name)

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

        // Use 1/8th of the available memory for this memory cache.
        val cacheSize = maxMemory / 8
        artBitmapCache = object : LruCache<String, Bitmap?>(cacheSize) {
            override fun sizeOf(key: String?, bitmap: Bitmap?): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return (bitmap?.byteCount ?: 0) / 1024
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_NOT_STICKY
    }

    fun stop() {
        deactivateMediaSession()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (listener != null) {
            listener!!.onDestroy()
            listener = null
        }
        mediaMetadata = null
        artBitmap = null
        queue.clear()
        mediaMetadataCache.clear()
        controls.clear()
        artBitmapCache!!.evictAll()
        compactActionIndices = null
        releaseMediaSession()
        legacyStopForeground(audioServiceConfig?.androidResumeOnClick == false)
        // This still does not solve the Android 11 problem.
        // if (notificationCreated) {
        //     NotificationManager notificationManager = getNotificationManager();
        //     notificationManager.cancel(NOTIFICATION_ID);
        // }
        releaseWakeLock()
        instance = null
        notificationCreated = false
    }

    @Suppress("deprecation")
    private fun legacyStopForeground(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_LEGACY)
        } else {
            stopForeground(removeNotification)
        }
    }

    fun getConfig(): AudioServiceConfig {
        return audioServiceConfig
    }

    private fun configure(config: AudioServiceConfig) {
        this.audioServiceConfig = config

        notificationChannelId =
            if (config.androidNotificationChannelId != null) config.androidNotificationChannelId else application.packageName + ".channel"
        if (config.activityClassName != null) {
            val context = applicationContext
            val intent = Intent(null as String?)
            intent.component = ComponentName(context, config.activityClassName!!)
            //Intent intent = new Intent(context, config.activityClassName);
            intent.action = NOTIFICATION_CLICK_ACTION
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            flags = flags or PendingIntent.FLAG_IMMUTABLE
            contentIntent =
                PendingIntent.getActivity(context, REQUEST_CONTENT_INTENT, intent, flags)
        } else {
            contentIntent = null
        }
        if (!config.androidResumeOnClick) {
            mediaSession!!.setMediaButtonReceiver(null)
        }
    }

    private fun getResourceId(resource: String): Int {
        val parts = resource.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val resourceType = parts[0]
        val resourceName = parts[1]
        return resources.getIdentifier(resourceName, resourceType, applicationContext.packageName)
    }

    private fun createAction(
        resource: String,
        label: String?,
        actionCode: Long
    ): NotificationCompat.Action {
        val iconId = getResourceId(resource)
        return NotificationCompat.Action(
            iconId, label,
            buildMediaButtonPendingIntent(actionCode)
        )
    }

    private fun mapToBundle(map: Map<*, *>?): Bundle? {
        if (map == null) {
            return null
        }

        val bundle = Bundle()

        map.forEach {entry ->
            val key = entry.key as String

            when (val value = entry.value) {
                is Int -> {
                    bundle.putInt(key, value)
                }

                is Long -> {
                    bundle.putLong(key, value)
                }

                else -> {
                    bundle.putString(key, value.toString())
                }
            }
        }

        return bundle
    }

    private fun createCustomAction(control: MediaControl): PlaybackStateCompat.CustomAction? {
        val iconId = getResourceId(control.icon)
        return PlaybackStateCompat.CustomAction.Builder(
            control.customAction.name,
            control.label,
            iconId
        )
            .setExtras(mapToBundle(control.customAction.extras))
            .build()
    }

    private fun buildMediaButtonPendingIntent(action: Long): PendingIntent? {
        val keyCode = toKeyCode(action)
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return null
        val intent = Intent(this, MediaButtonReceiver::class.java)
        intent.action = Intent.ACTION_MEDIA_BUTTON
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        var flags = 0
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, keyCode, intent, flags)
    }

    private fun buildDeletePendingIntent(): PendingIntent {
        val intent = Intent(this, MediaButtonReceiver::class.java)
        intent.action = ACTION_NOTIFICATION_DELETE
        var flags = 0
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, 0, intent, flags)
    }

    fun setState(
        controls: MutableList<MediaControl>,
        actionBits: Long,
        compactActionIndices: IntArray?,
        processingState: AudioProcessingState,
        playing: Boolean,
        position: Long,
        bufferedPosition: Long,
        speed: Float,
        updateTime: Long,
        errorCode: Int?,
        errorMessage: String?,
        repeatMode: Int,
        shuffleMode: Int,
        captioningEnabled: Boolean,
        queueIndex: Long?
    ) {
        var notificationChanged = false
        if (!Arrays.equals(compactActionIndices, this.compactActionIndices)) {
            notificationChanged = true
        }
        if (controls != this.controls) {
            notificationChanged = true
        }
        this.controls = controls
        nativeActions.clear()
        customActions.clear()
        for (control in controls) {
            val customAction = createCustomAction(control)
            if (customAction != null) {
                customActions.add(customAction)
            } else {
                nativeActions.add(createAction(control.icon, control.label, control.actionCode))
            }
        }
        this.compactActionIndices = compactActionIndices
        val wasPlaying = isPlaying
        val oldProcessingState = this.processingState
        this.processingState = processingState
        isPlaying = playing
        this.repeatMode = repeatMode
        this.shuffleMode = shuffleMode
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(AUTO_ENABLED_ACTIONS or actionBits)
            .setState(playbackState, position, speed, updateTime)
            .setBufferedPosition(bufferedPosition)
        for (action in customActions) {
            stateBuilder.addCustomAction(action)
        }
        if (queueIndex != null) stateBuilder.setActiveQueueItemId(queueIndex)
        if (errorCode != null && errorMessage != null) stateBuilder.setErrorMessage(
            errorCode,
            errorMessage
        ) else if (errorMessage != null) stateBuilder.setErrorMessage(-987654, errorMessage)
        if (mediaMetadata != null) {
            // Update the progress bar in the browse view as content is playing as explained
            // here: https://developer.android.com/training/cars/media#browse-progress-bar
            val extras = Bundle()
            extras.putString(
                MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_MEDIA_ID,
                mediaMetadata!!.description.mediaId
            )
            stateBuilder.setExtras(extras)
        }
        mediaSession!!.setPlaybackState(stateBuilder.build())
        mediaSession!!.setRepeatMode(repeatMode)
        mediaSession!!.setShuffleMode(shuffleMode)
        mediaSession!!.setCaptioningEnabled(captioningEnabled)
        if (!wasPlaying && playing) {
            enterPlayingState()
        } else if (wasPlaying && !playing) {
            exitPlayingState()
        }

        if (oldProcessingState !== AudioProcessingState.IDLE && processingState === AudioProcessingState.IDLE) {
            stop()
        } else if (processingState !== AudioProcessingState.IDLE && notificationChanged) {
            updateNotification()
        }
    }

    fun setPlaybackInfo(playbackType: Int, volumeControlType: Int, maxVolume: Int, volume: Int?) {
        if (playbackType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL) {
            // We have to wait 'til media2 before we can use AudioAttributes.
            mediaSession!!.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
            volumeProvider = null
        } else if (playbackType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
            if (volumeProvider == null || volumeControlType != volumeProvider!!.volumeControl || maxVolume != volumeProvider!!.maxVolume) {
                volumeProvider = object : VolumeProviderCompat(
                    volumeControlType, maxVolume,
                    volume!!
                ) {
                    override fun onSetVolumeTo(volumeIndex: Int) {
                        if (listener == null) return
                        listener!!.onSetVolumeTo(volumeIndex)
                    }

                    override fun onAdjustVolume(direction: Int) {
                        if (listener == null) return
                        listener!!.onAdjustVolume(direction)
                    }
                }
            } else {
                volumeProvider!!.currentVolume = (volume)!!
            }
            mediaSession!!.setPlaybackToRemote(volumeProvider)
        } else {
            // silently ignore
        }
    }

    private val playbackState: Int
        get() = when (processingState) {
            AudioProcessingState.IDLE -> PlaybackStateCompat.STATE_NONE
            AudioProcessingState.LOADING -> PlaybackStateCompat.STATE_CONNECTING
            AudioProcessingState.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            AudioProcessingState.READY -> if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            AudioProcessingState.COMPLETED -> if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            AudioProcessingState.ERROR -> PlaybackStateCompat.STATE_ERROR
        }

    private fun buildNotification(): Notification {
        var compactActionIndices = compactActionIndices
        if (compactActionIndices == null) {
            compactActionIndices = IntArray(MAX_COMPACT_ACTIONS.coerceAtMost(nativeActions.size))
            for (i in compactActionIndices.indices) compactActionIndices[i] = i
        }
        val builder = notificationBuilder
        if (mediaMetadata != null) {
            val description = mediaMetadata!!.description
            if (description.title != null) builder.setContentTitle(description.title)
            if (description.subtitle != null) builder.setContentText(description.subtitle)
            if (description.description != null) builder.setSubText(description.description)
            synchronized(this) { if (artBitmap != null) builder.setLargeIcon(artBitmap) }
        }
        if (audioServiceConfig.androidNotificationClickStartsActivity) builder.setContentIntent(mediaSession!!.controller.sessionActivity)

        if (audioServiceConfig.notificationColor != -1) builder.color = audioServiceConfig.notificationColor
        for (action in nativeActions) {
            builder.addAction(action)
        }
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession!!.sessionToken)
        if (Build.VERSION.SDK_INT < 33) {
            style.setShowActionsInCompactView(*compactActionIndices)
        }
        if (audioServiceConfig.androidNotificationOngoing) {
            style.setShowCancelButton(true)
            style.setCancelButtonIntent(buildMediaButtonPendingIntent(PlaybackStateCompat.ACTION_STOP))
            builder.setOngoing(true)
        }
        builder.setStyle(style)
        return builder.build()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val notificationBuilder: NotificationCompat.Builder
        get() {
            // This local variable could be commented out and replaced by an
            // instance variable if we want to reuse the builder instance. However,
            // there doesn't turn out to be much benefit to this since we don't
            // actually reuse any of the previous notification values when setting
            // a new notification.
            var notificationBuilder: NotificationCompat.Builder? = null
            if (notificationBuilder == null) {
                createChannel()

                notificationBuilder = NotificationCompat.Builder(
                    this,
                    notificationChannelId!!
                )
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setShowWhen(false)
                    .setDeleteIntent(buildDeletePendingIntent())
            }
            val iconId = audioServiceConfig.androidNotificationIcon?.let { getResourceId(it) }
            if (iconId != null) {
                notificationBuilder.setSmallIcon(iconId)
            }
            return notificationBuilder
        }

    fun handleDeleteNotification() {
        if (listener == null) return
        listener!!.onClose()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            var channel = notificationManager.getNotificationChannel(notificationChannelId)
            if (channel == null) {
                channel = NotificationChannel(
                    notificationChannelId,
                    audioServiceConfig.androidNotificationChannelName,
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.setShowBadge(audioServiceConfig.androidShowNotificationBadge)
                if (audioServiceConfig.androidNotificationChannelDescription != null) channel.description =
                    audioServiceConfig.androidNotificationChannelDescription
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun updateNotification() {
        if (notificationCreated) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun enterPlayingState() {
        ContextCompat.startForegroundService(
            this, Intent(
                this@AudioService,
                AudioService::class.java
            )
        )
        if (!mediaSession!!.isActive) mediaSession!!.isActive = true
        acquireWakeLock()
        mediaSession!!.setSessionActivity(contentIntent)
        internalStartForeground()
    }

    private fun exitPlayingState() {
        if (audioServiceConfig.androidStopForegroundOnPause) {
            exitForegroundState()
        }
    }

    private fun exitForegroundState() {
        legacyStopForeground(false)
        releaseWakeLock()
    }

    private fun internalStartForeground() {
        startForeground(NOTIFICATION_ID, buildNotification())
        notificationCreated = true
    }

    private fun acquireWakeLock() {
        if (!wakeLock!!.isHeld) wakeLock!!.acquire(10*60*1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        if (wakeLock!!.isHeld) wakeLock!!.release()
    }

    private fun activateMediaSession() {
        if (!mediaSession!!.isActive) mediaSession!!.isActive = true
    }

    private fun deactivateMediaSession() {
        if (mediaSession!!.isActive) {
            mediaSession!!.isActive = false
        }
        // Force cancellation of the notification
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun releaseMediaSession() {
        if (mediaSession == null) return
        deactivateMediaSession()
        mediaSession!!.release()
        mediaSession = null
    }

    /**
     * Updates queue.
     * Gets called from background thread.
     */
    @Synchronized
    fun setQueue(queue: MutableList<MediaSessionCompat.QueueItem>) {
        Companion.queue = queue
        mediaSession!!.setQueue(queue)
    }

    fun playMediaItem(description: MediaDescriptionCompat) {
        mediaSessionCallback!!.onPlayMediaItem(description)
    }

    /**
     * Updates metadata, loads the art and updates the notification.
     * Gets called from background thread.
     *
     *
     * Also adds the loaded art bitmap to the MediaMetadata.
     * This is needed to display art in lock screen in versions
     * prior Android 11, in which this feature was removed.
     *
     *
     * See:
     * - https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#album_artwork
     * - https://9to5google.com/2020/08/02/android-11-lockscreen-art/
     */
    @Synchronized
    fun setMetadata(mediaMetadata: MediaMetadataCompat) {
        var fMediaMetadata = mediaMetadata

        val artCacheFilePath = fMediaMetadata.getString("artCacheFile")
        if (artCacheFilePath != null) {
            // Load local files and network images, cached in files
            artBitmap = loadArtBitmap(artCacheFilePath, null)
            fMediaMetadata = putArtToMetadata(mediaMetadata)
        } else {
            // Load content:// URIs
            val artUri = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI)
            if (artUri != null && artUri.startsWith("content:")) {
                val loadThumbnailUri = mediaMetadata.getString("loadThumbnailUri")
                artBitmap = loadArtBitmap(artUri, loadThumbnailUri)
                fMediaMetadata = putArtToMetadata(mediaMetadata)
            } else {
                artBitmap = null
            }
        }

        this.mediaMetadata = fMediaMetadata
        mediaSession!!.setMetadata(fMediaMetadata)
        handler.removeCallbacksAndMessages(null)
        handler.post { updateNotification() }
    }

    private fun putArtToMetadata(mediaMetadata: MediaMetadataCompat): MediaMetadataCompat {
        return MediaMetadataCompat.Builder(mediaMetadata)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artBitmap)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artBitmap)
            .build()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        var isRecentRequest =
            rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT)
        if (isRecentRequest == null) isRecentRequest = false
        val extras: Bundle? = audioServiceConfig.getBrowsableRootExtras()
        return BrowserRoot(if (isRecentRequest) RECENT_ROOT_ID else BROWSABLE_ROOT_ID, extras)
        // The response must be given synchronously, and we can't get a
        // synchronous response from the Dart layer. For now, we hardcode
        // the root to "root". This may improve in media2.
        //return listener.onGetRoot(clientPackageName, clientUid, rootHints);
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        onLoadChildren(parentMediaId, result)
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>,
        options: Bundle
    ) {
        if (listener == null) {
            result.sendResult(ArrayList())
            return
        }

        listener!!.onLoadChildren(parentMediaId, result, options)
    }

    override fun onLoadItem(itemId: String, result: Result<MediaBrowserCompat.MediaItem>) {
        if (listener == null) {
            result.sendResult(null)
            return
        }
        listener!!.onLoadItem(itemId, result)
    }

    override fun onSearch(
        query: String,
        extras: Bundle,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (listener == null) {
            result.sendResult(ArrayList())
            return
        }
        listener!!.onSearch(query, extras, result)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (listener != null) {
            listener!!.onTaskRemoved()
        }
        super.onTaskRemoved(rootIntent)
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onAddQueueItem(description: MediaDescriptionCompat) {
            if (listener == null) return
            getMediaMetadata(description.mediaId)?.let { listener!!.onAddQueueItem(it) }
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat, index: Int) {
            if (listener == null) return
            getMediaMetadata(description.mediaId)?.let { listener!!.onAddQueueItemAt(it, index) }
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
            if (listener == null) return
            getMediaMetadata(description.mediaId)?.let { listener!!.onRemoveQueueItem(it) }
        }

        override fun onPrepare() {
            if (listener == null) return
            if (!mediaSession!!.isActive) mediaSession!!.isActive = true
            listener!!.onPrepare()
        }

        override fun onPrepareFromMediaId(mediaId: String, extras: Bundle) {
            if (listener == null) return
            if (!mediaSession!!.isActive) mediaSession!!.isActive = true
            listener!!.onPrepareFromMediaId(mediaId, extras)
        }

        override fun onPrepareFromSearch(query: String, extras: Bundle) {
            if (listener == null) return
            if (!mediaSession!!.isActive) mediaSession!!.isActive = true
            listener!!.onPrepareFromSearch(query, extras)
        }

        override fun onPrepareFromUri(uri: Uri, extras: Bundle) {
            if (listener == null) return
            if (!mediaSession!!.isActive) mediaSession!!.isActive = true
            listener!!.onPrepareFromUri(uri, extras)
        }

        override fun onPlay() {
            if (listener == null) return
            listener!!.onPlay()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            if (listener == null) return
            listener!!.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlayFromSearch(query: String, extras: Bundle) {
            if (listener == null) return
            listener!!.onPlayFromSearch(query, extras)
        }

        override fun onPlayFromUri(uri: Uri, extras: Bundle) {
            if (listener == null) return
            listener!!.onPlayFromUri(uri, extras)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            if (listener == null) return false
            @Suppress("deprecation")
            val event = mediaButtonEvent.extras!!
                .getParcelable<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent?

            if (event!!.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KEYCODE_BYPASS_PLAY -> onPlay()
                    KEYCODE_BYPASS_PAUSE -> onPause()
                    KeyEvent.KEYCODE_MEDIA_STOP -> onStop()
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> onFastForward()
                    KeyEvent.KEYCODE_MEDIA_REWIND -> onRewind()
                    KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> listener!!.onClick(
                        eventToButton(event)
                    )
                }
            }

            return true
        }

        private fun eventToButton(event: KeyEvent?): MediaButton {
            return when (event!!.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> MediaButton.MEDIA
                KeyEvent.KEYCODE_MEDIA_NEXT -> MediaButton.NEXT
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaButton.PREVIOUS
                else -> MediaButton.MEDIA
            }
        }

        override fun onPause() {
            if (listener == null) return
            listener!!.onPause()
        }

        override fun onStop() {
            if (listener == null) return
            listener!!.onStop()
        }

        override fun onSkipToNext() {
            if (listener == null) return
            listener!!.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            if (listener == null) return
            listener!!.onSkipToPrevious()
        }

        override fun onFastForward() {
            if (listener == null) return
            listener!!.onFastForward()
        }

        override fun onRewind() {
            if (listener == null) return
            listener!!.onRewind()
        }

        override fun onSkipToQueueItem(id: Long) {
            if (listener == null) return
            listener!!.onSkipToQueueItem(id)
        }

        override fun onSeekTo(pos: Long) {
            if (listener == null) return
            listener!!.onSeekTo(pos)
        }

        override fun onSetRating(rating: RatingCompat) {
            if (listener == null) return
            listener!!.onSetRating(rating)
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            if (listener == null) return
            listener!!.onSetPlaybackSpeed(speed)
        }

        override fun onSetCaptioningEnabled(enabled: Boolean) {
            if (listener == null) return
            listener!!.onSetCaptioningEnabled(enabled)
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            if (listener == null) return
            listener!!.onSetRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            if (listener == null) return
            listener!!.onSetShuffleMode(shuffleMode)
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            if (listener == null) return
            if (CUSTOM_ACTION_STOP == action) {
                listener!!.onStop()
            } else if (CUSTOM_ACTION_FAST_FORWARD == action) {
                listener!!.onFastForward()
            } else if (CUSTOM_ACTION_REWIND == action) {
                listener!!.onRewind()
            } else {
                listener!!.onCustomAction(action, extras)
            }
        }

        override fun onSetRating(rating: RatingCompat, extras: Bundle) {
            if (listener == null) return
            listener!!.onSetRating(rating, extras)
        }

        //
        // NON-STANDARD METHODS
        //
        fun onPlayMediaItem(description: MediaDescriptionCompat) {
            if (listener == null) return
            getMediaMetadata(description.mediaId)?.let { listener!!.onPlayMediaItem(it) }
        }
    }

    companion object {
        const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
        const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2
        const val CONTENT_STYLE_CATEGORY_LIST_ITEM_HINT_VALUE = 3
        const val CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE = 4

        private const val SHARED_PREFERENCES_NAME = "audio_service_preferences"

        private const val NOTIFICATION_ID = 1124
        private const val REQUEST_CONTENT_INTENT = 1000
        const val NOTIFICATION_CLICK_ACTION = "com.pnt.audio_service.NOTIFICATION_CLICK"
        const val CUSTOM_ACTION_STOP = "com.pnt.audio_service.action.STOP"
        const val CUSTOM_ACTION_FAST_FORWARD = "com.pnt.audio_service.action.FAST_FORWARD"
        const val CUSTOM_ACTION_REWIND = "com.pnt.audio_service.action.REWIND"
        private const val BROWSABLE_ROOT_ID = "root"
        private const val RECENT_ROOT_ID = "recent"

        // See the comment in onMediaButtonEvent to understand how the BYPASS keycodes work.
        // We hijack KEYCODE_MUTE and KEYCODE_MEDIA_RECORD since the media session subsystem
        // considers these keycodes relevant to media playback and will pass them on to us.
        const val KEYCODE_BYPASS_PLAY = KeyEvent.KEYCODE_MUTE
        const val KEYCODE_BYPASS_PAUSE = KeyEvent.KEYCODE_MEDIA_RECORD
        const val MAX_COMPACT_ACTIONS = 3
        private const val AUTO_ENABLED_ACTIONS = (PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_REWIND // Auto-enabling these is bad for Android Auto since it forces the
                // previous/next buttons to always show.
                //| PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                //| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_FAST_FORWARD
                or PlaybackStateCompat.ACTION_SET_RATING // "seek" is the exception because it's the only action that
                // affects the appearance of the media notification, so we leave it
                // up to the plugin user whether to enable it (via systemActions).
                //| PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                or PlaybackStateCompat.ACTION_PLAY_FROM_URI
                or PlaybackStateCompat.ACTION_PREPARE
                or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                or PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED)

        var instance: AudioService? = null

        private var contentIntent: PendingIntent? = null
        private var listener: ServiceListener? = null
        private var queue: MutableList<MediaSessionCompat.QueueItem> = ArrayList()
        private val mediaMetadataCache: MutableMap<String?, MediaMetadataCompat> = HashMap()

        fun init(listener: ServiceListener?) {
            Companion.listener = listener
        }

        fun toKeyCode(action: Long): Int {
            return when (action) {
                PlaybackStateCompat.ACTION_PLAY -> {
                    KEYCODE_BYPASS_PLAY
                }

                PlaybackStateCompat.ACTION_PAUSE -> {
                    KEYCODE_BYPASS_PAUSE
                }

                else -> {
                    PlaybackStateCompat.toKeyCode(action)
                }
            }
        }

        fun getMediaMetadata(mediaId: String?): MediaMetadataCompat? {
            return mediaMetadataCache[mediaId]
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight
                    && halfWidth / inSampleSize >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}