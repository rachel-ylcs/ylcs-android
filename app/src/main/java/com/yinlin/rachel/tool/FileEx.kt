package com.yinlin.rachel.tool

import android.content.Context
import androidx.annotation.RawRes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

lateinit var pathApp: File
lateinit var pathCache: File
val pathMusic: File get() = File(pathApp, "music")

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

fun File.copySafely(other: File, overwrite: Boolean = true): Boolean = try {
    this.copyTo(other, overwrite)
    true
} catch (_: Exception) { false }

fun File.deleteSafely(): Boolean = try {
    this.delete()
} catch (_: Exception) { false }

fun File.deleteFilterSafely(delName: String): Boolean = try {
    val files = listFiles { file ->
        val filename: String = file.getName()
        var pos = filename.lastIndexOf('.')
        var name = if (pos == -1) filename else filename.substring(0, pos)
        pos = name.lastIndexOf('_')
        if (pos != -1) name = name.substring(0, pos)
        file.isFile() && delName.equals(name, ignoreCase = true)
    }!!
    for (file in files) file.delete()
    true
} catch (_: Exception) { false }

private val Long.fileSizeString: String get() = if (this < 1024) "${this}B"
    else if (this < 1024 * 1024) "${this / 1024}KB"
    else if (this < 1024 * 1024 * 1024) "${this / (1024 * 1024)}MB"
    else "${this / (1024 * 1024 * 1024)}GB"

val File.fileSizeString: String get() = try { this.length().fileSizeString } catch (_: Exception) { "0B" }

val File.folderSizeString: String get() = try {
    var size = 0L
    Files.walk(this.toPath()).use {
        for (file in it) {
            if (Files.isRegularFile(file)) size += Files.size(file)
        }
    }
    size.fileSizeString
} catch (_: Exception) { "0B" }