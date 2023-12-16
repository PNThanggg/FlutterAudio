package com.pnt.flutter_audio.interfaces

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import androidx.media.MediaBrowserServiceCompat
import com.pnt.flutter_audio.models.MediaButton

interface ServiceListener {
    // BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints);
    fun onLoadChildren(
        parentMediaId: String,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>,
        options: Bundle
    )

    fun onLoadItem(itemId: String, result: MediaBrowserServiceCompat.Result<MediaBrowserCompat.MediaItem>)
    
    fun onSearch(
        query: String,
        extras: Bundle,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
    )

    fun onClick(mediaButton: MediaButton)
    fun onPrepare()
    fun onPrepareFromMediaId(mediaId: String, extras: Bundle)
    fun onPrepareFromSearch(query: String, extras: Bundle)
    fun onPrepareFromUri(uri: Uri, extras: Bundle)
    fun onPlay()
    fun onPlayFromMediaId(mediaId: String, extras: Bundle)
    fun onPlayFromSearch(query: String, extras: Bundle)
    fun onPlayFromUri(uri: Uri, extras: Bundle)
    fun onSkipToQueueItem(id: Long)
    fun onPause()
    fun onSkipToNext()
    fun onSkipToPrevious()
    fun onFastForward()
    fun onRewind()
    fun onStop()
    fun onSeekTo(pos: Long)
    fun onSetRating(rating: RatingCompat)
    fun onSetRating(rating: RatingCompat, extras: Bundle)
    fun onSetRepeatMode(repeatMode: Int)
    fun onSetShuffleMode(shuffleMode: Int)
    fun onCustomAction(action: String, extras: Bundle)
    fun onAddQueueItem(metadata: MediaMetadataCompat)
    fun onAddQueueItemAt(metadata: MediaMetadataCompat, index: Int)
    fun onRemoveQueueItem(metadata: MediaMetadataCompat)
    fun onRemoveQueueItemAt(index: Int)
    fun onSetPlaybackSpeed(speed: Float)
    fun onSetCaptioningEnabled(enabled: Boolean)
    fun onSetVolumeTo(volumeIndex: Int)
    fun onAdjustVolume(direction: Int)

    //
    // NON-STANDARD METHODS
    //

    fun onPlayMediaItem(metadata: MediaMetadataCompat)
    fun onTaskRemoved()
    fun onClose()
    fun onDestroy()
}