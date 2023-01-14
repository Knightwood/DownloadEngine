package com.kiylx.download_module.file.file_platform.system

import com.kiylx.download_module.file.file_platform.system.SysCall
import java.io.FileDescriptor

/**
 * 默认实现，可以被外界实现替换
 */
class SysCallImpl: SysCall {
    override fun checkConnectivity(): Boolean {
        return true
    }

    override fun lseek(fd: FileDescriptor, offset: Long) {
        TODO("Not yet implemented")
    }

    override fun fallocate(fd: FileDescriptor, length: Long) {
        TODO("Not yet implemented")
    }

    override fun availableBytes(fd: FileDescriptor): Long {
        TODO("Not yet implemented")
    }
}