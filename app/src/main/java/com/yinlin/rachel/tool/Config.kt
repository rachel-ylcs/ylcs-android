package com.yinlin.rachel.tool

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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Config {
    lateinit var kv: MMKV

    abstract class Meta<T>(private val version: String? = null) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(key(property))
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(key(property), value)
        abstract fun get(key: String): T
        abstract fun set(key: String, value: T)
        private fun key(property: KProperty<*>) = if (version != null) "${property.name}_${version}" else property.name
    }

    abstract class PrimitiveMeta<T>(protected val default: T, version: String? = null) : Meta<T>(version)

    open class BooleanMeta(defValue: Boolean, version: String? = null) : PrimitiveMeta<Boolean>(defValue, version) {
        override fun get(key: String) = kv.decodeBool(key, default)
        override fun set(key: String, value: Boolean) { kv.encode(key, value) }
    }

    open class IntMeta(defValue: Int, version: String? = null) : PrimitiveMeta<Int>(defValue, version) {
        override fun get(key: String) = kv.decodeInt(key, default)
        override fun set(key: String, value: Int) { kv.encode(key, value) }
    }

    open class LongMeta(defValue: Long, version: String? = null) : PrimitiveMeta<Long>(defValue, version) {
        override fun get(key: String) = kv.decodeLong(key, default)
        override fun set(key: String, value: Long) { kv.encode(key, value) }
    }

    open class EnumMeta<U : RachelEnum>(defValue: U, private val cls: Class<U>, version: String? = null) : PrimitiveMeta<U>(defValue, version) {
        override fun get(key: String) = cls.getDeclaredConstructor(Int::class.java).newInstance(kv.decodeInt(key, default.ordinal)) as U
        override fun set(key: String, value: U) { kv.encode(key, value.ordinal) }
    }

    open class StringMeta(defValue: String, version: String? = null) : PrimitiveMeta<String>(defValue, version) {
        override fun get(key: String) = kv.decodeString(key) ?: default
        override fun set(key: String, value: String) { kv.encode(key, value) }
    }

    class JsonMeta<U>(private val defValueGetter: () -> U, private val type: Type, version: String? = null) : Meta<U>(version) {
        override fun get(key: String) = try { kv.decodeString(key)?.let { gson.fromJson(it, type) } ?: defValueGetter() } catch (_: Exception) { defValueGetter() }
        override fun set(key: String, value: U) { kv.encode(key, if (value != null) gson.toJson(value) else "null") }
    }

    class DailyCacheKeyMeta : IntMeta(-1) {
        override fun set(key: String, value: Int) = super.set(key, currentDateInteger)
    }

    class CacheKeyMeta : LongMeta(-1) {
        override fun set(key: String, value: Long) = super.set(key, System.currentTimeMillis())
    }

    inline fun <reified T> ignore(): T = when (T::class) {
        Boolean::class -> false as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        String::class -> "" as T
        else -> throw Error()
    }

    /* ------------------  配置  ------------------ */

    // 用户 Token
    var token: String by StringMeta("", "20241115")
    // 更新 Token
    var token_daily: Int by DailyCacheKeyMeta()
    // 用户信息
    var user: User? by JsonMeta({ null }, object : TypeToken<User?>(){}.type, "20241115")
    val isLogin: Boolean get() = token.isNotEmpty() && user != null
    val loginUser: User? get() = if (token.isNotEmpty()) user else null

    // 上一次播放列表
    var music_last_playlist: String by StringMeta("", "20241128")
    // 上一次播放歌曲
    var music_last_music: String by StringMeta("", "20241128")
    // 播放模式
    var music_play_mode: MusicPlayMode by EnumMeta(MusicPlayMode.ORDER, MusicPlayMode::class.java, "20241115")
    // 禁止音频焦点
    var music_focus: Boolean by BooleanMeta(false, "20241115")
    // 歌词设置
    var music_lyrics_settings: LyricsSettings by JsonMeta({ LyricsSettings() }, object : TypeToken<LyricsSettings>(){}.type, "20241130")
    // 歌单
    var playlist: PlaylistMap by JsonMeta({ mutableMapOf() }, object : TypeToken<HashMap<String, Playlist>>(){}.type)
    // 微博用户
    var weibo_users: WeiboUserStorageList by JsonMeta({ WeiboUserStorage.defaultWeiboUsers }, object : TypeToken<MutableList<WeiboUserStorage>>(){}.type)

    // 图片缓存
    var cache_daily_pic: Int by DailyCacheKeyMeta()
    // 头像缓存
    var cache_key_avatar: Long by CacheKeyMeta()
    // 背景墙缓存
    var cache_key_wall: Long by CacheKeyMeta()
}