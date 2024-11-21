package com.yinlin.rachel.data

enum class RachelMessage {
    // FragmentMusic
    PREPARE_PLAYER, // MediaController
    MUSIC_START_PLAYER,  // [Playlist | String]
    MUSIC_PAUSE_PLAYER,
    MUSIC_STOP_PLAYER,
    MUSIC_GET_PLAYLIST_INFO_PREVIEW, // String -> LoadMusicPreviewList
    MUSIC_CREATE_PLAYLIST,  // String -> Boolean
    MUSIC_RENAME_PLAYLIST,  // String, String -> Boolean
    MUSIC_DELETE_PLAYLIST,  // String
    MUSIC_UPDATE_PLAYLIST, // String, LoadMusicPreviewList
    MUSIC_RELOAD_PLAYLIST,
    MUSIC_DELETE_MUSIC_FROM_PLAYLIST,  // String, String
    MUSIC_ADD_MUSIC_INTO_PLAYLIST,  // Playlist, List<String> -> Int
    MUSIC_GET_MUSIC_INFO_PREVIEW, // String? -> MusicInfoPreviewList
    MUSIC_DELETE_MUSIC,  // MusicInfoPreviewList
    MUSIC_GOTO_MUSIC,  // String
    MUSIC_NOTIFY_ADD_MUSIC,  // List<String>

    MUSIC_USE_LYRICS_ENGINE,  // String, String


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