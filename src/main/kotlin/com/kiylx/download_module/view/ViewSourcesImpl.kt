package com.kiylx.download_module.view

import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil

class ViewSourcesImpl private constructor() : IViewSources {
    var logger = JavaLogUtil.setLoggerHandler()

    override fun notifyViewsChanged(
        info: DownloadInfo,
        action: ViewUpdateAction,
        lifecycleCollection: DownloadTask.LifecycleCollection
    ) {
        logger.info("进度：" + info.getPercent())
        logger.info("bytes/s : " + info.speed)
    }

    companion object {
        val viewSourcesInstance: IViewSources by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ViewSourcesImpl() }
    }
}
