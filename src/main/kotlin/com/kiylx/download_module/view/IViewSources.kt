package com.kiylx.download_module.view

import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.model.DownloadInfo

interface IViewSources {
    fun notifyViewsChanged(
        info: DownloadInfo,
        action: ViewUpdateAction,
        lifecycleCollection: DownloadTask.LifecycleCollection
    )

}

enum class ViewUpdateAction {
    ADD, UPDATE_PROGESS, STOP, RESUME, RETRY, DELETE
}