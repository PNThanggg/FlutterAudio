package com.pnt.flutter_audio.types.sorttypes

import android.os.Build
import android.provider.MediaStore

@Suppress("DEPRECATION")
object PlaylistSortType {
    fun checkPlaylistSortType(sortType: Int?, order: Int, ignoreCase: Boolean): String {
        //[ASC] = Ascending Order
        //[DESC] = Descending Order
        val orderAndCase: String = if (ignoreCase) {
            if (order == 0) " COLLATE NOCASE ASC" else " COLLATE NOCASE DESC"
        } else {
            if (order == 0) " ASC" else " DESC"
        }

        return when (sortType) {
            0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.DISPLAY_NAME + orderAndCase
            } else {
                MediaStore.Audio.Playlists.NAME + orderAndCase
            }

            1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.DATE_ADDED + orderAndCase
            } else {
                MediaStore.Audio.Playlists.DATE_ADDED + orderAndCase
            }

            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER + orderAndCase
            } else {
                MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER + orderAndCase
            }
        }
    }
}