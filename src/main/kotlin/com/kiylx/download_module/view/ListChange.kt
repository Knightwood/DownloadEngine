package com.kiylx.download_module.view

interface ListChange {
    fun add(info: SimpleDownloadInfo)
    fun remove(info: SimpleDownloadInfo)
}