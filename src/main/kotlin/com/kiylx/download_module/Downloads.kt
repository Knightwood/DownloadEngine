package com.kiylx.download_module

import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.taskhandler.ATaskHandler
import java.util.*

class Downloads private constructor(configs: Context.ContextConfigs) {
    var mContext: Context
        private set
    private var mTaskHandler: ATaskHandler

    init {
        mContext = Context.getContextSingleton(configs)//确保最先初始化Context
        mTaskHandler = mContext.taskHandler
    }

    //=======================================下载任务相关方法======================================//
    @JvmOverloads
    fun execDownloadTask(
        url: String,
        path: String,
        fileName: String = "",
        totalSize: Long = -1,
        threadNum: Int = 0,
        backProcess: ATaskHandler.IBackHandler? = null
    ): DownloadInfo {
        val info = DownloadInfo(url, path, fileName,UUID.randomUUID()).apply {
            totalBytes = totalSize
            threadCounts = threadNum
        }
        execDownloadTask(info, false, backProcess)
        return info
    }

    fun execDownloadTask(
        info: DownloadInfo,
        fromDisk: Boolean = false,
        backProcess: ATaskHandler.IBackHandler? = null
    ) {
        //生成下载任务
        mTaskHandler.generateNewTask(info).run {
            if (fromDisk)
                isRecoveryFromDisk = true
            backHandler = backProcess
            runDownloadTask(this)
        }

    }

    fun pauseDownload(id: UUID) = mTaskHandler.requestPauseTask(id)
    fun resumeTask(id: UUID) = mTaskHandler.resumeTask(id)
    fun cancelTask(id: UUID) = mTaskHandler.requestCancelTask(id)

    //=======================================存储相关方法======================================//
    /**
     * @param kind :DownloadsListKind中定义
     * 返回存储库存储的下载信息
     */
    fun getDownloadsInfoList(kind: Int): MutableList<DownloadInfo> {
        TODO("Not yet implemented")

        //return mContext.repo?.queryList(kind)
    }

    //=======================================私有方法======================================//
    private fun runDownloadTask(task: DownloadTask) {
        mTaskHandler.addDownloadTask(task)
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
        const val none = -1
        const val wait_kind = 0 //wait和frozen同属于wait队列
        const val active_kind = 1
        const val frozen_kind = 2
        const val finish_kind = 3
    }
}
