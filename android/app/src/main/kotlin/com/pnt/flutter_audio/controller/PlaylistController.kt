package com.pnt.flutter_audio.controller

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.pnt.flutter_audio.PluginProvider
import io.flutter.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class PlaylistController {
    private val context: Context = PluginProvider.context()
    private val call: MethodCall = PluginProvider.call()
    private val result: MethodChannel.Result = PluginProvider.result()

    // Main parameters
    private val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
    } else {
        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
    }
    private val contentValues = ContentValues()
    private val channelError = "on_audio_error"
    private lateinit var resolver: ContentResolver

    // Query projection
    private val columns = arrayOf(
        "count(*)"
    )

    fun createPlaylist() {
        this.resolver = context.contentResolver
        val playlistName = call.argument<String>("playlistName")!!

        @Suppress("DEPRECATION")
        //For create we don't check if name already exist
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, playlistName)
            contentValues.put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis())
        } else {

            contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName)
            contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())
        }

        resolver.insert(uri, contentValues)
        result.success(true)
    }

    fun removePlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            val delUri = ContentUris.withAppendedId(uri, playlistId.toLong())
            resolver.delete(delUri, null, null)
            result.success(true)
        }
    }

    @Suppress("DEPRECATION")
    fun addToPlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!

        // Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            val uri =
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
            //If Android is Q/10 or above "count(*)" don't count, so, we use other method.
            val columnsBasedOnVersion = if (Build.VERSION.SDK_INT < 29) columns else null
            val cursor = resolver.query(uri, columnsBasedOnVersion, null, null, null)
            var count = -1
            while (cursor != null && cursor.moveToNext()) {
                count += if (Build.VERSION.SDK_INT < 29) cursor.count else cursor.getInt(0)
            }
            cursor?.close()

            try {
                // For create we don't check if name already exist
                contentValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count + 1)
                contentValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId.toLong())
                resolver.insert(uri, contentValues)
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
            }
        }
    }

    fun removeFromPlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!

        // Check if Playlist exists based on Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            @Suppress("DEPRECATION") try {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaStore.Audio.Media.getContentUri(
                        "external", playlistId.toLong()
                    )
                } else {
                    MediaStore.Audio.Playlists.Members.getContentUri(
                        "external", playlistId.toLong()
                    )
                }

                val where = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaStore.Audio.Media.DISPLAY_NAME + "=?"
                } else {
                    MediaStore.Audio.Playlists.Members._ID + "=?"
                }

                resolver.delete(uri, where, arrayOf(audioId.toString()))
                result.success(true)
            } catch (e: Exception) {
                Log.i("on_audio_error: ", e.toString())
                result.success(false)
            }
        }
    }

    fun moveItemTo() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val from = call.argument<Int>("from")!!
        val to = call.argument<Int>("to")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) {
            result.success(false)
        } else {
            @Suppress("DEPRECATION") val value: Boolean =
                MediaStore.Audio.Playlists.Members.moveItem(resolver, playlistId.toLong(), from, to)
            result.success(value)
        }
    }

    fun renamePlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val newPlaylistName = call.argument<String>("newPlName")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            @Suppress("DEPRECATION")
            //For create we don't check if name already exist
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, newPlaylistName)
                contentValues.put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis())
            } else {

                contentValues.put(MediaStore.Audio.Playlists.NAME, newPlaylistName)
                contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())
            }
            resolver.update(uri, contentValues, "_id=${playlistId.toLong()}", null)
            result.success(true)
        }
    }

    private fun checkPlaylistId(plId: Int): Boolean {
        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media._ID),
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION") resolver.query(
                uri,
                arrayOf(MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists._ID),
                null,
                null,
                null
            )
        }

        while (cursor != null && cursor.moveToNext()) {
            val playListId = cursor.getInt(1) //Id
            if (playListId == plId) return true
        }

        cursor?.close()
        return false
    }
}