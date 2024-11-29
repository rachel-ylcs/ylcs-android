package com.yinlin.rachel.tool

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

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
val Any?.parseJson: JsonElement
    get() =
    if (this == null) JsonNull.INSTANCE
    else if (this is String) gson.fromJson(this, JsonElement::class.java)
    else gson.toJsonTree(this)
val Any.parseJsonObject: JsonObject get() = this.parseJson.asJsonObject
val Any.parseJsonArray: JsonArray get() = this.parseJson.asJsonArray
inline fun <reified T> String.parseJsonFetch(): T = gson.fromJson(this, object : TypeToken<T>(){}.type)
inline val <reified T> T?.jsonString: String get() = gson.toJson(this, object : TypeToken<T>(){}.type)
fun jsonMap(vararg pairs: Pair<String, Any?>): JsonObject = gson.toJsonTree(mapOf(*pairs)).asJsonObject

val JsonElement.asIntOrNull: Int? get() = if (this.isJsonNull) null else asInt
val JsonElement.asStringOrNull: String? get() = if (this.isJsonNull) null else asString
val JsonElement.asObjectDefaultEmpty: JsonObject get() = if (this.isJsonNull) JsonObject() else asJsonObject
@ExperimentalStdlibApi
val JsonElement.asByteArray: ByteArray get() = this.asString.hexToByteArray()