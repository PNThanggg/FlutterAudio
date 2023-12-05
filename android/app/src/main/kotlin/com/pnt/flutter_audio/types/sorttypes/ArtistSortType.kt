package com.pnt.flutter_audio.types.sorttypes

import android.provider.MediaStore

object ArtistSortType {
    fun checkArtistSortType(sortType: Int?, order: Int, ignoreCase: Boolean): String {
        //[ASC] = Ascending Order
        //[DESC] = Descending Order
        val orderAndCase: String = if (ignoreCase) {
            if (order == 0) " COLLATE NOCASE ASC" else " COLLATE NOCASE DESC"
        } else {
            if (order == 0) " ASC" else " DESC"
        }
        return when (sortType) {
            0 -> MediaStore.Audio.Artists.ARTIST + orderAndCase
            1 -> MediaStore.Audio.Artists.NUMBER_OF_TRACKS + orderAndCase
            2 -> MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + orderAndCase
            else -> MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + orderAndCase
        }
    }
}