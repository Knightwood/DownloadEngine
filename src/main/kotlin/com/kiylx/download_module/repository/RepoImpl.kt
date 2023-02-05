package com.kiylx.download_module.repository

import com.kiylx.download_module.file.file_platform.FakeFile
import com.kiylx.download_module.file.file_platform.PathFile
import com.kiylx.download_module.file.file_platform.newInputStream
import com.kiylx.download_module.getContext
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.model.DownloadInfo
import java.util.UUID
import com.kiylx.download_module.interfaces.Repo.SyncAction
import com.kiylx.download_module.interfaces.Repo.SyncAction.*
import com.kiylx.download_module.model.PieceInfo
import kotlin.jvm.Volatile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 每一个下载任务都有一个文件存储下载信息
 * 文件以下载信息中的文件名为名称，以“session”为后缀，存储于某个位置，位置由context指定,操作系统不同，位置不同
 */
class RepoImpl private constructor() : Repo {
    private val filesMap: HashMap<UUID, FakeFile<Path>> = hashMapOf()
    private val configFolder = Path.of(getContext().config.configFilePath)
        get() {
            if (field.notExists()) {
                field.createDirectories()
            }
            return field
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun saveInfo(info: DownloadInfo) {
        if (!filesMap.containsKey(info.uuid)) {
            filesMap[info.uuid] = PathFile(getFilePath(info.uuid))

        }
        val file = filesMap[info.uuid]
        file?.let { Json.encodeToStream(info, it.newOutputStream()) }
    }

    override fun queryInfo(uuid: UUID): DownloadInfo? {
        val file: Path = getFilePath(uuid)
        return if (file.exists()) Json.decodeFromStream(file.newInputStream()) else null
    }

    override fun update(info: DownloadInfo) =saveInfo(info)

    override fun deleteInfo(uuid: UUID) {
        val file: Path = getFilePath(uuid)
        file.deleteExisting()
    }

    override fun syncInfoToDisk(info: DownloadInfo, action: SyncAction) {
        when (action) {
            ADD -> saveInfo(info)
            UPDATE -> update(info)
            DELETE -> deleteInfo(info.uuid)
        }
    }

    /**
     * 生成一个以uuid.session为名称的path对象,用于保存文件下载信息
     */
    private fun getFilePath(uuid: UUID): Path {
        return Path.of("${configFolder.pathString}${File.separator}$uuid.session")
    }

    companion object {
        @Volatile
        private var mRepo: Repo? = null
        val instance: Repo?
            get() {
                if (mRepo == null) {
                    synchronized(Repo::class.java) {
                        if (mRepo == null) {
                            mRepo = RepoImpl()
                        }
                    }
                }
                return mRepo
            }
    }
}

fun main() {
    val path = Path.of("/home/kiylxf/下载/testFload/a/b/")
    path.createDirectories()
    print(path.name)

    val file = Path.of("/home/kiylxf/下载/testFload/a/b/测试.session")
    //写入
    val info = PieceInfo(UUID.randomUUID(), 2, 233333333, 11111111)
    Json.encodeToStream(info, Files.newOutputStream(file))
    val info2 = PieceInfo(UUID.randomUUID(), 3, 12, 1111)
    Json.encodeToStream(info2, Files.newOutputStream(file))
    //读取
    val piece = Json.decodeFromStream<PieceInfo>(Files.newInputStream(file))
    print(piece.toString())
    print(path.pathString)
}