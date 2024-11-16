package com.yinlin.rachel

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*---------    JSON序列化器    --------*/

class ByteArrayAdapter : TypeAdapter<ByteArray>() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun write(writer: JsonWriter, value: ByteArray?) {
        if (value == null) writer.nullValue()
        else writer.value(value.toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun read(reader: JsonReader): ByteArray? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        else return reader.nextString().hexToByteArray()
    }
}

val gson: Gson = GsonBuilder().serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss")
    .registerTypeAdapter(ByteArray::class.java, ByteArrayAdapter())
    .create()

inline fun <reified T> JsonElement?.fetch(): T = gson.fromJson(this, object : TypeToken<T>(){}.type)
val String?.parseJson: JsonElement get() = if (this == null) JsonNull.INSTANCE else gson.fromJson(this, JsonElement::class.java)
inline fun <reified T> String.parseJsonFetch(): T = gson.fromJson(this, object : TypeToken<T>(){}.type)
inline val <reified T> T?.jsonString: String get() = gson.toJson(this, object : TypeToken<T>(){}.type)
fun jsonMap(vararg pairs: Pair<String, Any?>): JsonObject = gson.toJsonTree(mapOf(*pairs)).asJsonObject

val JsonElement.asIntOrNull: Int? get() = if (this.isJsonNull) null else asInt
val JsonElement.asStringOrNull: String? get() = if (this.isJsonNull) null else asString
val JsonElement.asObjectDefaultEmpty: JsonObject get() = if (this.isJsonNull) JsonObject() else asJsonObject
@OptIn(ExperimentalStdlibApi::class)
val JsonElement.asByteArray: ByteArray get() = this.asString.hexToByteArray()

/*---------    反射    --------*/

fun <T> Any.reflect(name: String): T? = try {
    val field: Field = this.javaClass.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    field.get(this) as T
}
catch (ignored: Exception) { null }

/*---------    File    --------*/

lateinit var basePath: String
val pathAPP: File
    get() = File(basePath)
val pathMusic: File
    get() = File(basePath, "music")

operator fun File.div(child: String) = File(this, child)

fun File.read() : ByteArray {
    var data = byteArrayOf()
    try {
        FileInputStream(this).use {
            val size = it.available()
            if (size > 0) {
                data = ByteArray(size)
                it.read(data)
            }
        }
    } catch (ignored: Exception) { }
    return data
}

fun File.readText() : String = read().toString(StandardCharsets.UTF_8)

fun File.write(data: ByteArray) {
    try {
        FileOutputStream(this, false).use { it.write(data) }
    }
    catch (ignored: Exception) { }
}

fun File.writeText(data: String) = write(data.toByteArray(StandardCharsets.UTF_8))

inline fun <reified T> File.readJson(): T = readText().parseJsonFetch()

fun File.writeJson(obj: Any) = writeText(obj.jsonString)

fun File.create(data: ByteArray) {
    if (!exists()) write(data)
}

fun File.createAll() = mkdirs()

fun File.deleteFilter(delName: String) {
    listFiles { file ->
        val filename: String = file.getName()
        var pos = filename.lastIndexOf('.')
        var name = if (pos == -1) filename else filename.substring(0, pos)
        pos = name.lastIndexOf('_')
        if (pos != -1) name = name.substring(0, pos)
        file.isFile() && delName.equals(name, ignoreCase = true)
    }?.let {
        for (file in it) file.delete()
    }
}


/*---------    ValueGetter    --------*/

val Long.timeString: String get() {
    val second = (this / 1000).toInt()
    return String.format(Locale.ENGLISH,"%02d:%02d", second / 60, second % 60)
}

val currentDateInteger: Int get() = try {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val formattedDate = currentDate.format(formatter)
    formattedDate.toInt()
}
catch (err: Exception) { System.currentTimeMillis().toInt() }

val String.md5: String get() = try {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray(StandardCharsets.UTF_8))
    val hexString = StringBuilder()
    for (b in md.digest()) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    hexString.toString()
}
catch (ignored: Exception) { "" }

/*---------    Container    --------*/

fun <E> MutableCollection<E>.clearAddAll(element: Collection<E>) {
    clear()
    addAll(element)
}

fun <K, V> MutableMap<K, V>.clearAddAll(element: Map<out K, V>) {
    clear()
    putAll(element)
}

/*---------    Third-Party Intent    --------*/

fun gotoAPP(context: Context, name: String, uri: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
    catch (ignored: Exception) { }
}

fun gotoQQ(context: Context, id: String) = gotoAPP(context, "QQ",
    "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=${id}&card_type=person&source=qrcode")

fun gotoQQGroup(context: Context, id: String) = gotoAPP(context, "QQ",
    "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=${id}&card_type=group&source=qrcode")

fun gotoTaobaoShop(context: Context, shopId: String) = gotoAPP(context, "淘宝",
    "taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=${shopId}")