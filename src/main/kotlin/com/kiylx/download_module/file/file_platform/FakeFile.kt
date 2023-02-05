package com.kiylx.download_module.file.file_platform

import com.kiylx.download_module.file.file_platform.system.SysCall
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * 一些关于平台和读写方式的描述
 */
class Platform {
    class OS{
        companion object{
            const val linux = 1
            const val windows = 2
            const val android = 3
            const val other = 4
        }
    }
    class RDWay{
        companion object{
            const val fdImpl = 5 //基于文件描述符
            const val pathImpl = 6//基于Path类
        }
    }
}


/**
 * 对于不同平台，包含不同的文件使用方式。
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
        const val linux = Platform.OS.linux
        const val windows = Platform.OS.windows
        const val android = Platform.OS.android
        const val other = Platform.OS.other
        const val fdImpl = Platform.RDWay.fdImpl //基于文件描述符
        const val pathImpl = Platform.RDWay.pathImpl//基于Path类

        const val MIN_PROGRESS_STEP = 65536 //64kib
        const val MIN_PROGRESS_TIME: Long = 1500
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


    override fun get(): Path = path

    private val randomAccessFile: RandomAccessFile by lazy { RandomAccessFile(path.toFile(), "rw") }

    override fun seek(pos: Long) = randomAccessFile.seek(pos)

    @Throws(IOException::class)
    override fun close() = randomAccessFile.close()

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        randomAccessFile.write(b, off, len)
    }

    override fun newOutputStream(): OutputStream = Files.newOutputStream(path)

    override fun newInputStream(): InputStream = Files.newInputStream(path)

}
inline fun Path.newInputStream()=Files.newInputStream(this)
inline fun Path.newOutputStream()=Files.newOutputStream(this)

