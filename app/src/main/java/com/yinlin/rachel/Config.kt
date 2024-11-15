package com.yinlin.rachel

import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.data.weibo.WeiboUserStorageMap
import java.lang.reflect.Type

object Config {
    abstract class Meta<T>(protected val name: String, protected val defValue: T?) {
        abstract fun set(value: T)
        abstract fun get(): T
        abstract fun setDefault()
    }

    interface CheckDefault {
        fun isDefault(): Boolean
    }

    open class IntMeta(name: String, defValue: Int) : Meta<Int>(name, defValue), CheckDefault {
        override fun set(value: Int) { kv.encode(name, value) }
        override fun get() = kv.decodeInt(name, defValue!!)
        override fun setDefault() { kv.encode(name, defValue!!) }
        override fun isDefault() = get() == defValue
    }

    open class StringMeta(name: String, defValue: String) : Meta<String>(name, defValue), CheckDefault {
        override fun set(value: String) { kv.encode(name, value) }
        override fun get() = kv.decodeString(name, defValue)!!
        override fun setDefault() { kv.encode(name, defValue) }
        override fun isDefault() = get() == defValue
    }

    class JsonMeta<U>(name: String, private val defJson: String, private val type: Type) : Meta<U>(name, null) {
        override fun set(value: U) { kv.encode(name, if (value != null) gson.toJson(value) else "null") }
        override fun get(): U = gson.fromJson(kv.decodeString(name, defJson), type)
        override fun setDefault() { kv.encode(name, defJson) }
    }

    class DailyCacheKeyMeta : IntMeta("daily_cache_key", currentDateInteger)

    class CacheKeyMeta(name: String) : Meta<Long>(name, System.currentTimeMillis()), CheckDefault {
        override fun set(value: Long) { kv.encode(name, value) }
        override fun get() = kv.decodeLong(name, defValue!!)
        override fun setDefault() { kv.encode(name, defValue!!) }
        override fun isDefault() = get() == defValue
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

    // 用户信息
    private val token_meta = StringMeta("token/20241115", "")
    var token: String
        get() = token_meta.get()
        set(value) { token_meta.set(value) }

    private val user_meta = JsonMeta<User?>("user/20241115", "null", object : TypeToken<User?>(){}.type)
    var user: User?
        get() = user_meta.get()
        set(value) { user_meta.set(value) }
    val isLogin: Boolean get() = !token_meta.isDefault() && user != null

    // 歌单
    private val playlist_meta = JsonMeta<PlaylistMap>("playlist", "{}",
        object : TypeToken<HashMap<String, Playlist>>(){}.type)
    var playlist: PlaylistMap
        get() = playlist_meta.get()
        set(value) { playlist_meta.set(value) }

    // 微博用户
    private val weibo_users_meta = JsonMeta<WeiboUserStorageMap>("weibo_users", WeiboUserStorage.defaultWeiboUsers,
        object : TypeToken<LinkedHashMap<String, WeiboUserStorage>>(){}.type)
    var weibo_users: WeiboUserStorageMap
        get() = weibo_users_meta.get()
        set(value) { weibo_users_meta.set(value) }

    // 图片缓存键
    private val cache_daily_pic_meta = DailyCacheKeyMeta()
    val cache_daily_pic get() = cache_daily_pic_meta.get()

    val cache_key_avatar_meta = CacheKeyMeta("cache_key_avatar")
    val cache_key_avatar get() = cache_key_avatar_meta.get()

    val cache_key_wall_meta = CacheKeyMeta("cache_key_wall")
    val cache_key_wall get() = cache_key_wall_meta.get()
}