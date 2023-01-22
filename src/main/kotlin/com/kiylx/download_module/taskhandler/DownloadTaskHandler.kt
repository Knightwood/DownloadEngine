package com.kiylx.download_module.taskhandler

import com.kiylx.download_module.interfaces.ATaskHandler
import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.model.StatusCode
import com.kiylx.download_module.model.TaskResult
import com.kiylx.download_module.model.TaskResult.TaskResultCode
import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier

class DownloadTaskHandler private constructor() : ATaskHandler() {
    private val logger = JavaLogUtil.setLoggerHandler()
    private var config: TaskHandlerConfig = defaultConfig()
    private val taskList = TaskList()
    private val executorService: ExecutorService? = Executors.newFixedThreadPool(config.downloadLimit * 2)

    companion object {
        val downloadTaskHandler: DownloadTaskHandler
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

    private fun runTask(task: DownloadTask) {
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
                onCompleted(task, result)
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.SucceedKind)
                handleInfoStatus(task, ListKind.SucceedKind)
            }
            TaskResultCode.CANCELED, TaskResultCode.FAILED, TaskResultCode.ERROR -> {
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.SucceedKind)
                handleInfoStatus(task, ListKind.SucceedKind)
            }
            TaskResultCode.PAUSED -> {
                taskList.move(task.taskId, ListKind.ActiveKind, ListKind.Stopped)
                handleInfoStatus(task, ListKind.Stopped)
            }
        }
        scheduleDownload()
    }

    /**
     * @param downloadTask null: 将等待下载的任务执行下载并加入active;
     *                     传入task： 直接运行task
     */
    private fun scheduleDownload(task: DownloadTask? = null) {
        if (taskList.waitKindSize()>0){

        }
    }

    /**
     * 下载完成，处理一些情况
     */
    private fun onCompleted(task: DownloadTask, result: TaskResult) {
        val info = task.getInfo()
        if (info != null) {
            handleInfoStatus(task)
            val b: Boolean = verifyChecksum(task) //todo 校验完成需要通知
        }
    }

    /**
     * @param task 任务
     */
    private fun handleInfoStatus(task: DownloadTask, listKind: ListKind = ListKind.None) {
        val info = task.getInfo() ?: return
        when (info.finalCode) {
            StatusCode.STATUS_SUCCESS -> {
                checkMoveAfterDownload(task.getInfo())
            }
            StatusCode.STATUS_CANCELLED -> {}
            StatusCode.STATUS_WAITING_TO_RETRY, StatusCode.STATUS_WAITING_FOR_NETWORK -> {
                reTry(task.taskId, listKind)
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {}
            HttpURLConnection.HTTP_PROXY_AUTH -> {}
        }
        task.syncInfo(Repo.SyncAction.UPDATE)
        if (iBackHandler != null) {
            //后处理，比如交给app重命名或是移动文件等
            iBackHandler!!.handle(info)
        }
    }

    /**
     * 下载完成后移动文件
     *
     * @param info download info
     */
    private fun checkMoveAfterDownload(info: DownloadInfo) {
        //todo 移动文件
    }

    /**
     * 添加下载任务
     *
     * @param task download task
     * @return 顺利放入active开始下载, 返回true ；
     * 任务需要继续等待，返回false ；
     */
    override fun addDownloadTask(task: DownloadTask): Boolean {
        val isFull = taskList.downloadingIsFull()
        if (!isFull) {
            taskList.add(task, ListKind.ActiveKind)
            runTask(task)
        } else {
            taskList.add(task, ListKind.WaitKind)
        }
        return isFull
    }


    override fun reTry(id: UUID, kind: ListKind) {
        val task = taskList.find(id, kind)
        task?.let {
            it.isRecoveryFromDisk = true
            it.getInfo().plusRetryCount()
            taskList.move(id, kind, ListKind.ActiveKind)
        }
    }

    override fun requestPauseTask(id: UUID) {
        taskList.find(id, ListKind.ActiveKind)?.let {
            it.requestStop()
            taskList.move(id, ListKind.ActiveKind, ListKind.Stopped)
        }
    }

    override fun requestCancelTask(id: UUID, kind: ListKind) {
        taskList.find(id, kind)?.let {
            it.requestCancel()
            taskList.move(id, ListKind.ActiveKind, ListKind.SucceedKind)
        }
    }

    override fun resumeTask(id: UUID) {
        taskList.find(id, ListKind.Stopped)?.let {
            it.requestResume()
            taskList.move(id, ListKind.SucceedKind, ListKind.ActiveKind)
        }
    }
}