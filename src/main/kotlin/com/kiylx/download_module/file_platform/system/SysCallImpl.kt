package com.kiylx.download_module.file_platform.system

import com.kiylx.download_module.file_platform.system.SysCall
import java.io.FileDescriptor

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