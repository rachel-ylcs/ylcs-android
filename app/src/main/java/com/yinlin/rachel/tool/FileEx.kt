package com.yinlin.rachel.tool

import android.content.Context
import androidx.annotation.RawRes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

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
    } catch (_: Exception) { }
    return data
}

fun File.readText() : String = read().toString(StandardCharsets.UTF_8)

fun File.write(data: ByteArray) {
    try {
        FileOutputStream(this, false).use { it.write(data) }
    }
    catch (_: Exception) { }
}

fun File.writeText(data: String) = write(data.toByteArray(StandardCharsets.UTF_8))

inline fun <reified T> File.readJson(): T = readText().parseJsonFetch()

fun File.writeJson(obj: Any) = writeText(obj.jsonString)

fun File.writeRes(context: Context, @RawRes id: Int) = try {
    context.resources.openRawResource(id).use { inputStream ->
        FileOutputStream(this).use { outputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while ((inputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE).also { read = it }) >= 0)
                outputStream.write(buffer, 0, read)
            true
        }
    }
}
catch (_: Exception) { false }

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

val File.fileSizeString: String get() = try {
    val fileSize = this.length()
    if (fileSize < 1024) "${fileSize}B"
    else if (fileSize < 1024 * 1024) "${fileSize / 1024}KB"
    else if (fileSize < 1024 * 1024 * 1024) "${fileSize / (1024 * 1024)}MB"
    else "${fileSize / (1024 * 1024 * 1024)}GB"
}
catch (_: Exception) { "0 B" }