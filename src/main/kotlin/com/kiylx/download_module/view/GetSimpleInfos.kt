package com.kiylx.download_module.view

import com.kiylx.download_module.DownloadsListKind
import com.kiylx.download_module.lib_core.engine.TaskHandler
import java.util.ArrayList

fun TaskHandler.getActiveList(viewsAction: Int =ViewsAction.generate): MutableList<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.active_kind).covert(viewsAction))
    return list
}

fun TaskHandler.getWaitingList(viewsAction: Int =ViewsAction.generate): MutableList<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.wait_kind).covert(viewsAction))
    list.addAll(getDownloadTaskList(DownloadsListKind.frozen_kind).covert(viewsAction))
    return list
}

fun TaskHandler.getFinishList(viewsAction: Int =ViewsAction.generate): MutableList<SimpleDownloadInfo> {
    val list: MutableList<SimpleDownloadInfo> = ArrayList()
    list.addAll(getDownloadTaskList(DownloadsListKind.finish_kind).covert(viewsAction))
    return list
}