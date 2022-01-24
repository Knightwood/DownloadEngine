package com.kiylx.download_module.file_platform

import com.kiylx.download_module.file_platform.system.SysCall
import com.kiylx.download_module.lib_core.interfaces.PieceThread.MIN_PROGRESS_STEP
import com.kiylx.download_module.lib_core.interfaces.PieceThread.MIN_PROGRESS_TIME
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * 对于不同平台，包含不同的文件使用方式。
 * linux平台下，使用文件描述符；windows平台下使用file对象
 */
sealed class FakeFile<T>(
    open val file: T,
    val platform: Int = linux,//平台
    val kind: Int = pathImpl,//辅助确认是基于什么类型的实现类
) {
    lateinit var sysCall: SysCall
    abstract fun seek(pos: Long)
    abstract fun get(): T
    abstract fun newOutputStream(): OutputStream
    abstract fun newInputStream(): InputStream

    @Throws(IOException::class)
    abstract fun write(b: ByteArray, off: Int, len: Int)

    @Throws(IOException::class)
    abstract fun close()

    companion object {
        const val linux = 1
        const val windows = 2
        const val android = 3
        const val other = 4
        const val fdImpl = 5 //基于文件描述符
        const val pathImpl = 6//基于Path类
    }
}

/**
 * 基于文件描述符的包装
 */
class FDFile(val fileDescriptor: FileDescriptor, platform: Int = android) :
    FakeFile<FileDescriptor>(fileDescriptor, platform, fdImpl) {
    var currentSize: Long = 0
    var currentTime: Long = -0
    val fout: FileOutputStream by lazy { FileOutputStream(fileDescriptor) }

    override fun seek(pos: Long) = sysCall.lseek(fileDescriptor, pos)

    override fun get(): FileDescriptor = fileDescriptor

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        fout.write(b, off, len)
        currentSize += b.size
        val flash = System.currentTimeMillis() - currentTime > MIN_PROGRESS_TIME
        if (currentSize > MIN_PROGRESS_STEP && flash) {
            fout.flush()
            fileDescriptor.sync()
            currentSize = 0
            currentTime = System.currentTimeMillis()
        }
    }

    override fun newOutputStream(): OutputStream = FileOutputStream(fileDescriptor)

    override fun newInputStream(): InputStream = FileInputStream(fileDescriptor)

    @Throws(IOException::class)
    override fun close() {
        fout.flush()
        fileDescriptor.sync()
        fout.close()
    }

}

/**
 * 基于path的包装
 */
class PathFile(val path: Path, platform: Int = linux) : FakeFile<Path>(path, platform, pathImpl) {

    private val randomAccessFile: RandomAccessFile by lazy { RandomAccessFile(path.toFile(), "rw") }

    override fun get(): Path = path

    override fun seek(pos: Long) = randomAccessFile.seek(pos)

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        randomAccessFile.write(b, off, len)
    }

    override fun newOutputStream(): OutputStream = Files.newOutputStream(path)

    override fun newInputStream(): InputStream = Files.newInputStream(path)

    @Throws(IOException::class)
    override fun close() = randomAccessFile.close()

}

