package com.yinlin.rachel.model

import com.yinlin.rachel.data.mod.MusicItem
import com.yinlin.rachel.data.mod.Metadata
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicRes
import com.yinlin.rachel.tool.div
import com.yinlin.rachel.tool.jsonString
import com.yinlin.rachel.tool.parseJsonFetch
import com.yinlin.rachel.tool.readJson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random


// | R | A | C | H | E | L |    ------ 6字节 Rachel标识
// | <META> |                   ------ ?字节 JSON元数据
// | <Items> |                  ------ ?字节 资源

object RachelMod {
    const val MOD_VERSION = 3
    val MOD_MAGIC = "RACHEL".toByteArray()
    const val MOD_BUFFER_SIZE = 1024 * 64

    fun interface MetaListener {
        fun onError(id: String, res: String)
    }

    fun interface Listener {
        fun onProcess(id: String, res: String, index: Int)
    }

    class Releaser(rawStream: InputStream) {
        private val stream = DataInputStream(rawStream)

        fun getMetadata(): Metadata {
            var metadata = Metadata()
            try {
                val magic = ByteArray(MOD_MAGIC.size)
                stream.read(magic)
                if (!magic.contentEquals(MOD_MAGIC)) return metadata
                metadata = stream.readUTF().parseJsonFetch()
            }
            catch (_: Exception) { }
            return metadata
        }

        fun run(folder: File, listener: Listener?): Boolean {
            try {
                val count = stream.readInt()
                val buffer = ByteArray(MOD_BUFFER_SIZE)
                var index = 0
                var countRead: Int
                for (i in 0 until count) {
                    val id = stream.readUTF()
                    val resCount = stream.readInt()
                    for (j in 0 until resCount) {
                        val resName = stream.readUTF()
                        var resSize = stream.readInt()
                        if (resSize <= 0) return false
                        FileOutputStream(folder / (id + resName)).use { writeStream ->
                            while (resSize >= MOD_BUFFER_SIZE) {
                                countRead = stream.read(buffer)
                                stream.readByte()
                                writeStream.write(buffer, 0, countRead)
                                resSize -= countRead
                            }
                            if (resSize != 0) {
                                countRead = stream.read(buffer, 0, resSize)
                                writeStream.write(buffer, 0, countRead)
                            }
                            listener?.onProcess(id, resName, index)
                        }
                        ++index
                    }
                }
            }
            catch (_: Exception) { return false }
            return true
        }

        fun close() {
            try { stream.close() }
            catch (_: Exception) { }
        }
    }

    class Merger(private val folder: File) {
        fun getMetadata(ids: List<String>, filter: List<String>, listener: MetaListener?): Metadata {
            val metadata = Metadata()
            if (!folder.isDirectory) return metadata
            // 统计
            for (id in ids) {
                val infoFile = folder / (id + MusicRes.INFO_NAME)
                if (!infoFile.exists()) continue
                val musicInfo: MusicInfo = infoFile.readJson()
                if (!musicInfo.isCorrect || musicInfo.id != id) {
                    listener?.onError(id, "NotCorrect")
                    continue
                }
                metadata.items[id] = MusicItem(musicInfo.name, musicInfo.version)
            }
            // 遍历
            val files = folder.listFiles { file: File -> file.isFile }
            if (files == null) {
                metadata.items.clear()
                return metadata
            }
            for (file in files) {
                val filename = file.name
                var pos = filename.lastIndexOf('.')
                var name = if (pos == -1) filename else filename.substring(0, pos)
                var ext = if (pos == -1) "" else filename.substring(pos)
                pos = name.lastIndexOf('_')
                if (pos != -1) {
                    ext = name.substring(pos) + ext
                    name = name.substring(0, pos)
                }
                val item = metadata.items[name]
                if (item == null || ext.isEmpty() || filter.contains(ext)) continue
                item.resList.add(ext)
            }
            // 至少存在一个音乐基础信息不足
            for (meta in metadata.items) {
                val resList: List<String> = meta.value.resList
                if (!resList.contains(MusicRes.INFO_NAME)) {
                    listener?.onError(meta.key, MusicRes.INFO_NAME)
                    metadata.items.clear()
                    break
                }
                if (!resList.contains(MusicRes.AUDIO_NAME)) {
                    listener?.onError(meta.key, MusicRes.AUDIO_NAME)
                    metadata.items.clear()
                    break
                }
                if (!resList.contains(MusicRes.RECORD_NAME)) {
                    listener?.onError(meta.key, MusicRes.RECORD_NAME)
                    metadata.items.clear()
                    break
                }
                if (!resList.contains(MusicRes.BGS_NAME)) {
                    listener?.onError(meta.key, MusicRes.BGS_NAME)
                    metadata.items.clear()
                    break
                }
                if (!resList.contains(MusicRes.DEFAULT_LRC_NAME)) {
                    listener?.onError(meta.key, MusicRes.DEFAULT_LRC_NAME)
                    metadata.items.clear()
                    break
                }
            }
            return metadata
        }

        fun run(rawStream: OutputStream, metadata: Metadata, listener: Listener?): Boolean {
            if (!folder.isDirectory) return false
            try {
                DataOutputStream(rawStream).use { stream ->
                    stream.write(MOD_MAGIC)
                    stream.writeUTF(metadata.jsonString)
                    stream.writeInt(metadata.items.size)
                    val buffer = ByteArray(MOD_BUFFER_SIZE)
                    var index = 0
                    var countRead: Int
                    for ((id, item) in metadata.items) {
                        stream.writeUTF(id)
                        stream.writeInt(item.resList.size)
                        for (resName in item.resList) {
                            stream.writeUTF(resName)
                            FileInputStream(folder / (id + resName)).use { readStream ->
                                val resSize = readStream.available()
                                if (resSize <= 0) return false
                                stream.writeInt(resSize)
                                while ((readStream.read(buffer).also { countRead = it }) != -1) {
                                    stream.write(buffer, 0, countRead)
                                    if (countRead == MOD_BUFFER_SIZE) stream.writeByte(Random.nextInt(127))
                                }
                                listener?.onProcess(id, resName, index)
                            }
                            ++index
                        }
                    }
                }
            }
            catch (e: Exception) { return false }
            return true
        }
    }
}