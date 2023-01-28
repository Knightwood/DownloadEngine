package com.kiylx.download_module.lib_core1

import com.kiylx.download_module.getContext
import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.lib_core1.engine1.DownloadTaskImpl
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.model.StatusCode
import com.kiylx.download_module.model.TaskResult
import com.kiylx.download_module.model.TaskResult.TaskResultCode
import com.kiylx.download_module.taskhandler.ATaskHandler
import com.kiylx.download_module.taskhandler.ListKind
import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil
import com.kiylx.download_module.utils.make
import java.net.HttpURLConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier

class DownloadTaskHandler private constructor() : ATaskHandler() {
    private val logger = JavaLogUtil.setLoggerHandler()
    private var config: TaskHandlerConfig = defaultConfig()
    private val executorService: ExecutorService? = Executors.newFixedThreadPool(config.downloadLimit * 2)
    private val lock: ReentrantLock = ReentrantLock()

    companion object {
        val instance: DownloadTaskHandler
                by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
                    DownloadTaskHandler()
                }
    }

    data class TaskHandlerConfig(var downloadLimit: Int = 0)

    /**
     * 生成默认配置
     */
    private fun defaultConfig(): TaskHandlerConfig = TaskHandlerConfig(3)


    fun config(configArgs: TaskHandlerConfig) {
        this.config = configArgs
    }

    override fun generateNewTask(info: DownloadInfo): DownloadTask {
        return DownloadTaskImpl(info)
    }

    override fun runTask(task: DownloadTask) {
        CompletableFuture.supplyAsync(Supplier {
            try {
                return@Supplier task.call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }, executorService).whenComplete { taskResult, throwable ->
            println("任务:${task.taskId} 下载结束")
            this@DownloadTaskHandler.finallyTaskFinish(task, taskResult)
        }.exceptionally { throwable: Throwable ->
            logger.severe(
                task.getInfo().uuid.toString() + " error: " +
                        throwable.message
            )
            null
        }
    }

    private fun finallyTaskFinish(task: DownloadTask, result: TaskResult?) {
        if (result == null) return
        when (result.taskResultCode) {
            TaskResultCode.DOWNLOAD_COMPLETE -> {
                processCompletedTask(task, result)
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.SucceedKind)
                backHandle(task)
            }
            TaskResultCode.CANCELED, TaskResultCode.FAILED, TaskResultCode.ERROR -> {
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.SucceedKind)
                handleErrorInfo(task)
            }
            TaskResultCode.PAUSED -> {
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.Stopped)
            }
            else -> {}
        }

        task.syncInfo(Repo.SyncAction.UPDATE)

        scheduleDownload()
    }

    /**
     * @param task null: 将等待下载的任务执行下载并加入active;
     *                     传入task： 直接运行task
     */
    private fun scheduleDownload(task: DownloadTask? = null) {
        try {
            lock.lock()
            make(taskList) {
                if (waitKindSize() > 0) {
                    //让等待下载的任务开始下载
                    firstWaitingTaskWrapper()?.let {
                        it.listKind = ListKind.ActiveKind
                        runTask(it.task)
                    }
                } else {
                    if (getContext().config.autoRunStopTask) {
                        move(ListKind.Stopped, ListKind.ActiveKind) {
                            runTask(it)
                        }
                    }
                }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * 对于下载完成的情况，做处理
     */
    private fun processCompletedTask(task: DownloadTask, result: TaskResult) {
        val info = task.getInfo()
        val b: Boolean = verifyChecksum(task) //todo 校验完成需要通知
    }

    /**
     * 处理下载得到的错误信息
     * @param task 任务
     */
    private fun handleErrorInfo(task: DownloadTask, listKind: ListKind = ListKind.ErrorKind) {
        val info = task.getInfo()
        when (info.finalCode) {
            StatusCode.STATUS_CANCELLED -> {}
            StatusCode.STATUS_WAITING_TO_RETRY, StatusCode.STATUS_WAITING_FOR_NETWORK -> {
                reTry(task.taskId, listKind)
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {}
            HttpURLConnection.HTTP_PROXY_AUTH -> {}
        }
    }

}