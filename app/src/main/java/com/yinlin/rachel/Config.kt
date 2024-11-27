package com.yinlin.rachel

import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yinlin.rachel.data.music.LyricsSettings
import com.yinlin.rachel.data.music.MusicPlayMode
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.data.weibo.WeiboUserStorageList
import com.yinlin.rachel.model.RachelEnum
import java.lang.reflect.Type

object Config {
    abstract class Meta<T>(protected val name: String) {
        abstract fun set(value: T)
        abstract fun get(): T
        abstract fun setDefault()
        abstract fun getDefault(): T
        val isDefault: Boolean get() = get() == getDefault()
    }

    abstract class DefaultMeta<T>(name: String, protected val defValue: T) : Meta<T>(name) {
        override fun getDefault(): T = defValue
    }

    open class BooleanMeta(name: String, defValue: Boolean) : DefaultMeta<Boolean>(name, defValue) {
        override fun set(value: Boolean) { kv.encode(name, value) }
        override fun get(): Boolean = kv.decodeBool(name, defValue)
        override fun setDefault() { kv.encode(name, defValue) }
    }

    open class IntMeta(name: String, defValue: Int) : DefaultMeta<Int>(name, defValue) {
        override fun set(value: Int) { kv.encode(name, value) }
        override fun get() = kv.decodeInt(name, defValue)
        override fun setDefault() { kv.encode(name, defValue) }
    }

    open class EnumMeta(name: String, defValue: RachelEnum) : DefaultMeta<RachelEnum>(name, defValue) {
        override fun set(value: RachelEnum) { kv.encode(name, value.ordinal) }
        override fun get() = RachelEnum(kv.decodeInt(name, defValue.ordinal))
        override fun setDefault() { kv.encode(name, defValue.ordinal) }
    }

    open class StringMeta(name: String, defValue: String) : DefaultMeta<String>(name, defValue) {
        override fun set(value: String) { kv.encode(name, value) }
        override fun get() = kv.decodeString(name, defValue) ?: defValue
        override fun setDefault() { kv.encode(name, defValue) }
    }

    class JsonMeta<U>(name: String, private val defJson: String, private val type: Type) : Meta<U>(name) {
        private val defValue: U = gson.fromJson(defJson, type)
        override fun set(value: U) { kv.encode(name, if (value != null) gson.toJson(value) else "null") }
        override fun get(): U = try { gson.fromJson(kv.decodeString(name, defJson), type) } catch (_: Exception) { defValue }
        override fun setDefault() { kv.encode(name, defJson) }
        override fun getDefault(): U = defValue
    }

    class DailyCacheKeyMeta : IntMeta("daily_cache_key", currentDateInteger)

    class CacheKeyMeta(name: String) : DefaultMeta<Long>(name, System.currentTimeMillis()) {
        override fun set(value: Long) { kv.encode(name, value) }
        override fun get() = kv.decodeLong(name, defValue)
        override fun setDefault() { kv.encode(name, defValue) }
        fun update() = set(System.currentTimeMillis())
    }

    // 补丁
    inline fun injectPatch(name: String, patch: () -> Unit) {
        val patchName = "patch/${name}"
        if (!kv.containsKey(patchName)) {
            kv.encode(patchName, true)
            patch()
        }
    }

    lateinit var kv: MMKV

    /* ------------------  配置  ------------------ */


    // 用户信息
    private val token_meta = StringMeta("token/20241115", "")
    var token: String
        get() = token_meta.get()
        set(value) { token_meta.set(value) }

    //
    private val token_daily_meta = DailyCacheKeyMeta()
    var token_daily get() = token_daily_meta.get()
        set(value) { token_daily_meta.set(value) }

    private val user_meta = JsonMeta<User?>("user/20241115", "null", object : TypeToken<User?>(){}.type)
    var user: User?
        get() = user_meta.get()
        set(value) { user_meta.set(value) }
    val isLogin: Boolean get() = !token_meta.isDefault && user != null
    val loginUser: User? get() = if (!token_meta.isDefault) user else null

    // 音频相关
    private val music_play_mode_meta = EnumMeta("music_play_mode/20241115", MusicPlayMode.ORDER)
    var music_play_mode: MusicPlayMode
        get() = MusicPlayMode(music_play_mode_meta.get().ordinal)
        set(value) { music_play_mode_meta.set(value) }

    private val music_focus_meta = BooleanMeta("music_focus/20241115", false)
    var music_focus: Boolean
        get() = music_focus_meta.get()
        set(value) { music_focus_meta.set(value) }

    private val music_lyrics_settings_meta = JsonMeta<LyricsSettings>("music_lyrics_settings/20241128", LyricsSettings().jsonString,
        object : TypeToken<LyricsSettings>(){}.type)
    var music_lyrics_settings: LyricsSettings
        get() = music_lyrics_settings_meta.get()
        set(value) { music_lyrics_settings_meta.set(value) }

    // 歌单
    private val playlist_meta = JsonMeta<PlaylistMap>("playlist", "{}",
        object : TypeToken<HashMap<String, Playlist>>(){}.type)
    var playlist: PlaylistMap
        get() = playlist_meta.get()
        set(value) { playlist_meta.set(value) }

    // 微博用户
    private val weibo_users_meta = JsonMeta<WeiboUserStorageList>("weibo_users/20241115", WeiboUserStorage.defaultWeiboUsers,
        object : TypeToken<MutableList<WeiboUserStorage>>(){}.type)
    var weibo_users: WeiboUserStorageList
        get() = weibo_users_meta.get()
        set(value) { weibo_users_meta.set(value) }

    // 图片缓存键
    private val cache_daily_pic_meta = DailyCacheKeyMeta()
    var cache_daily_pic get() = cache_daily_pic_meta.get()
        set(value) { cache_daily_pic_meta.set(value) }

    val cache_key_avatar_meta = CacheKeyMeta("cache_key_avatar/20241115")
    val cache_key_avatar get() = cache_key_avatar_meta.get()

    val cache_key_wall_meta = CacheKeyMeta("cache_key_wall/20241115")
    val cache_key_wall get() = cache_key_wall_meta.get()
}