package com.kiylx.download_module.taskhandler

import com.kiylx.download_module.file.FileUtils
import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.interfaces.ITaskHandler
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.utils.DigestUtils
import com.kiylx.download_module.view.IViewSources
import com.kiylx.download_module.view.ViewUpdateAction
import java.io.IOException
import java.util.*

/**
 * 一个下载任务管理器应该有的方法
 */
abstract class ATaskHandler : ITaskHandler {
    //外界把接口实现注册到这里，以此实现在下载完成后，另外界自动处理后续操作，比如重命名文件并移动到某一个特定目录
    var iBackHandler: IBackHandler? = null
    var viewSources: IViewSources? = null
    val taskList: TaskList by lazy { TaskList() }

    override fun registerViewSources(viewSources: IViewSources) {
        if (this.viewSources == null) this.viewSources = viewSources
    }

    override fun unRegisterViewSources(viewSources: IViewSources) {
        //移除接口
        this.viewSources = null
    }

    override fun registerHandle(taskHandler: IBackHandler) {
        if (this.iBackHandler == null) this.iBackHandler = taskHandler
    }

    override fun unRegisterHandle() {
        //移除接口
        iBackHandler = null
    }

    /**
     * 验证完整性
     */
    fun verifyChecksum(task: DownloadTask): Boolean {
        val info = task.getInfo()
        var hash = info.checkSum
        if (hash != null && hash.isNotEmpty()) {
            //todo 校验文件 sha256 md5
            hash = try {
                if (DigestUtils.isMd5Hash(info.checkSum)) {
                    FileUtils.calcHashSum(info, false)

                } else if (DigestUtils.isSha256Hash(info.checkSum)) {
                    FileUtils.calcHashSum(info, true)

                } else {
                    throw IllegalArgumentException("Unknown checksum type:" + info.checkSum!!)
                }

            } catch (e: IOException) {
                return false
            }

            return (hash != null && hash.equals(info.checkSum!!, ignoreCase = true))
        }
        return false
    }

    /**
     * 执行下载之后的处理
     */
    fun backHandle(task: DownloadTask) {
        if (task.backHandler != null) {
            task.backHandler!!.process(task.info)
        } else {
            //后处理，比如交给app重命名或是移动文件等
            iBackHandler?.process(task.info)
        }
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
        viewSources?.notifyViewsChanged(task.info, ViewUpdateAction.ADD, task.lifecycleCollection)
        return isFull
    }


    override fun reTry(id: UUID, kind: ListKind) {
        val task = taskList.find(id, kind)
        task?.let {
            it.isRecoveryFromDisk = true
            it.getInfo().plusRetryCount()
            taskList.move(id, kind, ListKind.ActiveKind)
            viewSources?.notifyViewsChanged(it.info, ViewUpdateAction.RETRY, it.lifecycleCollection)
        }
    }

    override fun requestPauseTask(id: UUID) {
        taskList.find(id, ListKind.ActiveKind)?.let {
            it.requestStop()
            taskList.move(id, ListKind.ActiveKind, ListKind.Stopped)
            viewSources?.notifyViewsChanged(it.info, ViewUpdateAction.STOP, it.lifecycleCollection)
        }
    }

    override fun requestCancelTask(id: UUID, kind: ListKind) {
        taskList.find(id, kind)?.let {
            it.requestCancel()
            taskList.move(id, ListKind.ActiveKind, ListKind.SucceedKind)
            viewSources?.notifyViewsChanged(it.info, ViewUpdateAction.DELETE, it.lifecycleCollection)
        }
    }

    override fun resumeTask(id: UUID) {
        taskList.find(id, ListKind.Stopped)?.let {
            it.requestResume()
            taskList.move(id, ListKind.SucceedKind, ListKind.ActiveKind)
            viewSources?.notifyViewsChanged(it.info, ViewUpdateAction.RESUME, it.lifecycleCollection)
        }
    }

    /**
     * 下载任务完成后，执行其他处理,可以组织成链式处理
     * 这里的处理优先级不如任务本身的处理*
     */
    abstract class IBackHandler {
        var nextProcess: IBackHandler? = null
        var id: String = ""//标识
        fun process(info: DownloadInfo) {
            handle(info)
            nextProcess?.process(info)
        }

        abstract fun handle(info: DownloadInfo)
    }

    abstract fun runTask(task: DownloadTask)
    abstract fun generateNewTask(info: DownloadInfo): DownloadTask
}