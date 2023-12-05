package com.pnt.flutter_audio.types

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pnt.flutter_audio.utils.CursorProjection.artistProjection
import com.pnt.flutter_audio.utils.CursorProjection.genreProjection
import com.pnt.flutter_audio.utils.CursorProjection.playlistProjection
import com.pnt.flutter_audio.utils.CursorProjection.songProjection

object WithFiltersType {

    fun checkWithFiltersType(sortType: Int): Uri {
        return when (sortType) {
            0 -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
            }

            3 -> MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
            4 -> MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            else -> throw Exception("[checkWithFiltersType] value don't exist!")
        }
    }

    fun checkProjection(withType: Uri): Array<String>? {
        return when (withType) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> songProjection()
            // [Album] projection is null because we need all items.
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI -> null
            @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI -> playlistProjection

            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> playlistProjection
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI -> artistProjection
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI -> genreProjection
            else -> songProjection()
        }
    }

    @SuppressLint("InlinedApi")
    fun checkSongsArgs(args: Int): String {
        return when (args) {
            0 -> MediaStore.Audio.Media.TITLE + " like ?"
            1 -> MediaStore.Audio.Media.DISPLAY_NAME + " like ?"
            2 -> MediaStore.Audio.Media.ALBUM + " like ?"
            3 -> MediaStore.Audio.Media.ARTIST + " like ?"
            else -> throw Exception("[checkSongsArgs] value don't exist!")
        }
    }

    fun checkAlbumsArgs(args: Int): String {
        return when (args) {
            0 -> MediaStore.Audio.Albums.ALBUM + " like ?"
            1 -> MediaStore.Audio.Albums.ARTIST + " like ?"
            else -> throw Exception("[checkAlbumsArgs] value don't exist!")
        }
    }

    fun checkPlaylistsArgs(args: Int): String {
        return when (args) {
            0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.DISPLAY_NAME + " like ?"
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Audio.Playlists.NAME + " like ?"
            }

            else -> throw Exception("[checkPlaylistsArgs] value don't exist!")
        }
    }

    fun checkArtistsArgs(args: Int): String {
        return when (args) {
            0 -> MediaStore.Audio.Artists.ARTIST + " like ?"
            else -> throw Exception("[checkArtistsArgs] value don't exist!")
        }
    }

    fun checkGenresArgs(args: Int): String {
        return when (args) {
            0 -> MediaStore.Audio.Genres.NAME + " like ?"
            else -> throw Exception("[checkGenresArgs] value don't exist!")
        }
    }
}