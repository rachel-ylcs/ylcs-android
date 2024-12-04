package com.yinlin.rachel.data

enum class RachelMessage {
    // FragmentMusic
    MUSIC_START_PLAYER,  // Playlist
    MUSIC_PAUSE_PLAYER,
    MUSIC_STOP_PLAYER,
    MUSIC_GET_PLAYLIST_NAMES, // -> List<String>
    MUSIC_GET_PLAYLIST_PREVIEW, // String -> PlaylistPreview
    MUSIC_FIND_PLAYLIST, // String -> Playlist?
    MUSIC_CREATE_PLAYLIST,  // String -> Boolean
    MUSIC_RENAME_PLAYLIST,  // String, String -> Boolean
    MUSIC_DELETE_PLAYLIST,  // String
    MUSIC_RELOAD_PLAYLIST,
    MUSIC_ADD_MUSIC_INTO_PLAYLIST,  // String, MusicInfoPreviewList -> Int
    MUSIC_DELETE_MUSIC_FROM_PLAYLIST,  // String, String
    MUSIC_MOVE_MUSIC_IN_PLAYLIST, // String, Int, Int
    MUSIC_GET_MUSIC_INFO, // String -> MusicInfo
    MUSIC_SEARCH_MUSIC_INFO_PREVIEW, // String? -> MusicInfoPreviewList
    MUSIC_DELETE_MUSIC,  // MusicInfoPreviewList
    MUSIC_GOTO_MUSIC,  // String
    MUSIC_NOTIFY_ADD_MUSIC,  // List<String>

    MUSIC_USE_LYRICS_ENGINE,  // String, String

    MUSIC_UPDATE_LYRICS_SETTINGS,


    // FragmentLibrary
    LIBRARY_UPDATE_MUSIC_INFO, // MusicInfo

    // FragmentMe
    ME_UPDATE_USER_INFO, // User?
    ME_REQUEST_USER_INFO,
    ME_ADD_ACTIVITY, // Calendar, ShowActivity

    // FragmentDiscovery
    DISCOVERY_ADD_TOPIC, // TopicPreview
    DISCOVERY_DELETE_TOPIC, // Int

    // FragmentProfile
    PROFILE_DELETE_TOPIC, // Int
    PROFILE_UPDATE_TOPIC_TOP, // Int, Int
}