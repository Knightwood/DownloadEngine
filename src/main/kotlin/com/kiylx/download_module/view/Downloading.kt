package com.kiylx.download_module.view

import com.kiylx.download_module.lib_core.data_struct.DownloadMap

class Downloading

val DownloadMap.viewsList
    get() = mutableListOf<SimpleDownloadInfo>()
val DownloadMap.observerList
    get() = mutableListOf<ListChange>()

fun DownloadMap.addListener(listChange: ListChange) {
    observerList.add(listChange)
}

fun DownloadMap.removeListener(listChange: ListChange) {
    observerList.remove(listChange)
}