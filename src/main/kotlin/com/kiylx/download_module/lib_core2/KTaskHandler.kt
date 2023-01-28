package com.kiylx.download_module.lib_core2

import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.taskhandler.ATaskHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class KTaskHandler : ATaskHandler(), CoroutineScope {
    private val job: Job by lazy { Job() }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun runTask(task: DownloadTask) {
        TODO("Not yet implemented")
    }

    override fun generateNewTask(info: DownloadInfo): DownloadTask {
        TODO("Not yet implemented")
    }

}