package com.pnt.flutter_audio.constant

object Constant {
    // General methods
    const val PERMISSION_STATUS = "permissionsStatus"
    const val PERMISSION_REQUEST = "permissionsRequest"
    const val QUERY_DEVICE_INFO = "queryDeviceInfo"
    const val SCAN = "scan"
    const val SET_LOG_CONFIG = "setLogConfig"

    // Query methods
    const val QUERY_AUDIOS = "querySongs"
    const val QUERY_ALBUMS = "queryAlbums"
    const val QUERY_ARTISTS = "queryArtists"
    const val QUERY_GENRES = "queryGenres"
    const val QUERY_PLAYLISTS = "queryPlaylists"
    const val QUERY_ARTWORK = "queryArtwork"
    const val QUERY_AUDIOS_FROM = "queryAudiosFrom"
    const val QUERY_WITH_FILTERS = "queryWithFilters"
    const val QUERY_ALL_PATHS = "queryAllPath"

    // Playlist methods
    const val CREATE_PLAYLIST = "createPlaylist"
    const val REMOVE_PLAYLIST = "removePlaylist"
    const val ADD_TO_PLAYLIST = "addToPlaylist"
    const val REMOVE_FROM_PLAYLIST = "removeFromPlaylist"
    const val RENAME_PLAYLIST = "renamePlaylist"
    const val MOVE_ITEM_TO = "moveItemTo"

    const val ACTION_NOTIFICATION_DELETE =
        "com.pnt.intent.action.ACTION_NOTIFICATION_DELETE"
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
}