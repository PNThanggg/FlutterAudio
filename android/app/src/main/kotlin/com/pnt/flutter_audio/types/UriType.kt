package com.pnt.flutter_audio.types


import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object UriType {
    fun checkAudiosUriType(uriType: Int): Uri {
        return when (uriType) {
            0 -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Audio.Media.INTERNAL_CONTENT_URI
            else -> throw Exception("[checkAudiosUriType] value don't exist!")
        }
    }

    fun checkAlbumsUriType(uriType: Int): Uri {
        return when (uriType) {
            0 -> MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Audio.Albums.INTERNAL_CONTENT_URI
            else -> throw Exception("[checkAlbumsUriType] value don't exist!")
        }
    }

    fun checkPlaylistsUriType(uriType: Int): Uri {
        return when (uriType) {
            0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
            }

            1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_INTERNAL
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI
            }

            else -> throw Exception("[checkPlaylistsUriType] value don't exist!")
        }
    }

    fun checkArtistsUriType(uriType: Int): Uri {
        return when (uriType) {
            0 -> MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Audio.Artists.INTERNAL_CONTENT_URI
            else -> throw Exception("[checkArtistsUriType] value don't exist!")
        }
    }

    fun checkGenresUriType(uriType: Int): Uri {
        return when (uriType) {
            0 -> MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Audio.Genres.INTERNAL_CONTENT_URI
            else -> throw Exception("[checkGenresUriType] value don't exist!")
        }
    }
}