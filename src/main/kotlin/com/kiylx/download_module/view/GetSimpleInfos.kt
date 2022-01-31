package com.kiylx.download_module.view

import com.kiylx.download_module.DownloadsListKind
import com.kiylx.download_module.lib_core.engine.TaskHandler
import java.util.ArrayList

fun TaskHandler.getActiveList(): List<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.active_kind).covert(ViewsAction.generate))
    return list
}

fun TaskHandler.getWaitingList(): List<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.wait_kind).covert(ViewsAction.generate))
    list.addAll(getDownloadTaskList(DownloadsListKind.frozen_kind).covert(ViewsAction.generate))
    return list
}

fun TaskHandler.getFinishList(): List<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.finish_kind).covert(ViewsAction.generate))
    return list
}