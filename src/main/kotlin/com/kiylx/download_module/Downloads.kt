package com.kiylx.download_module

import com.kiylx.download_module.lib_core.engine.DownloadTaskImpl
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadTask
import com.kiylx.download_module.lib_core.interfaces.TasksCollection
import com.kiylx.download_module.lib_core.model.DownloadInfo
import com.kiylx.download_module.view.SimpleDownloadInfo
import java.util.*

class Downloads private constructor(configs: Context.ContextConfigs) {
    var mContext: Context
        private set
    private var mTaskHandler: TaskHandler

    init {
        mContext = Context.getContextSingleton(configs)//确保最先初始化Context
        mTaskHandler = mContext.taskHandler
    }

    fun execDownloadTask(url: String, path: String, fileName: String = ""): DownloadInfo {
        val info = DownloadInfo(url, path, fileName)
        execDownloadTask(info)
        return info
    }

    fun execDownloadTask(info: DownloadInfo, fromDisk: Boolean = false) {
        val task = DownloadTaskImpl.instance(info) //添加下载任务
        if (fromDisk)
            task.isRecoveryFromDisk = true;
        runDownloadTask(task)
    }

    fun pauseDownload(id: UUID) = mTaskHandler.requestPauseTask(id)
    fun resumeTask(id: UUID) = mTaskHandler.resumeTask(id)
    fun cancelTask(id: UUID?) = mTaskHandler.requestCancelTask(id)

    /**
     * @param kind :DownloadsListKind中定义
     * 返回taskhandler中的信息
     */
    @Deprecated("不该使用")
    fun getDownloadsList(kind: Int): TasksCollection? {
        return mTaskHandler.getDownloadTaskList(kind)
    }

    /**
     * @param kind :DownloadsListKind中定义
     * 返回存储库存储信息
     */
    @Deprecated("不该使用")
    fun getDownloadsInfoList(kind: Int): MutableList<SimpleDownloadInfo>? {
        return mContext.repo?.queryList(kind)
    }

    private fun runDownloadTask(task: DownloadTask) {
        mTaskHandler.addDownloadTask(task)
    }

    private fun pauseDownload(task: DownloadTask) {
        val id = task.info.uuid
        id?.let { pauseDownload(it) }
    }

    companion object {
        //@JvmStatic val downloadsInstance: Downloads by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Downloads() }
        @Volatile
        private var mInstance: Downloads? = null

        /**
         * @param configs 传入configs配置Context
         * 初始化downloads时必须传入config
         */
        fun downloadsInstance(configs: Context.ContextConfigs): Downloads? {
            if (mInstance == null) {
                synchronized(this) {
                    if (mInstance == null) {
                        mInstance = Downloads(configs)
                    }
                }
            }
            return mInstance
        }
    }

}

class DownloadsListKind {
    companion object {
        const val wait_kind = 0 //wait和frozen同属于wait队列
        const val active_kind = 1
        const val frozen_kind = 2
        const val finish_kind = 3
    }
}
