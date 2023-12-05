package com.pnt.flutter_audio.controller

import com.pnt.flutter_audio.PluginProvider
import com.pnt.flutter_audio.constant.Constant
import com.pnt.flutter_audio.queries.AlbumQuery
import com.pnt.flutter_audio.queries.AllPathQuery
import com.pnt.flutter_audio.queries.ArtistQuery
import com.pnt.flutter_audio.queries.ArtworkQuery
import com.pnt.flutter_audio.queries.AudioFromQuery
import com.pnt.flutter_audio.queries.AudioQuery
import com.pnt.flutter_audio.queries.GenreQuery
import com.pnt.flutter_audio.queries.PlaylistQuery
import com.pnt.flutter_audio.queries.WithFiltersQuery

class MethodController {
    private val playlistController = PlaylistController()

    fun find() {
        when (PluginProvider.call().method) {
            //Query methods
            Constant.QUERY_AUDIOS -> AudioQuery().querySongs()
            Constant.QUERY_ALBUMS -> AlbumQuery().queryAlbums()
            Constant.QUERY_ARTISTS -> ArtistQuery().queryArtists()
            Constant.QUERY_PLAYLISTS -> PlaylistQuery().queryPlaylists()
            Constant.QUERY_GENRES -> GenreQuery().queryGenres()
            Constant.QUERY_ARTWORK -> ArtworkQuery().queryArtwork()
            Constant.QUERY_AUDIOS_FROM -> AudioFromQuery().querySongsFrom()
            Constant.QUERY_WITH_FILTERS -> WithFiltersQuery().queryWithFilters()
            Constant.QUERY_ALL_PATHS -> AllPathQuery().queryAllPath()

            //Playlists methods
            Constant.CREATE_PLAYLIST -> playlistController.createPlaylist()
            Constant.REMOVE_PLAYLIST -> playlistController.removePlaylist()
            Constant.ADD_TO_PLAYLIST -> playlistController.addToPlaylist()
            Constant.REMOVE_FROM_PLAYLIST -> playlistController.removeFromPlaylist()
            Constant.RENAME_PLAYLIST -> playlistController.renamePlaylist()
            Constant.MOVE_ITEM_TO -> playlistController.moveItemTo()

            else -> PluginProvider.result().notImplemented()
        }
    }
}