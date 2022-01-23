package com.kiylx.download_module.file_platform.system

import java.io.FileDescriptor
import java.io.IOException

// 平台依赖的文件系统借口，用于系统调用
interface SysCall {
    fun checkConnectivity(): Boolean

    @Throws(IOException::class, UnsupportedOperationException::class)
    fun lseek(fd: FileDescriptor, offset: Long)

    @Throws(IOException::class)
    fun fallocate(fd: FileDescriptor, length: Long)

    @Throws(IOException::class)
    fun availableBytes(fd: FileDescriptor): Long
}