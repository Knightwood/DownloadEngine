package com.kiylx.download_module.interfaces

import com.kiylx.download_module.taskhandler.ListKind
import com.kiylx.download_module.view.ViewSources
import java.util.UUID

/**
 * 由下载任务管理器实现，提供给外界使用的关于下载任务的方法
 */
interface ITaskHandler {
    /**
     * 注册一个视图侦听器，下载产生的信息更新将通过此向外传递
     */
    fun registerViewSources(viewSources: ViewSources)
    fun unRegisterViewSources(viewSources: ViewSources)

    /**
     * 注册一个下载任务结束后的处理器
     */
    fun registerHandle(taskHandler: ATaskHandler.IBackHandler)
    fun unRegisterHandle()

    /**
     * 添加下载任务
     */
    fun addDownloadTask(task: DownloadTask): Boolean

    /**
     * 重试下载
     */
    fun reTry(id: UUID, kind: ListKind)
    fun requestPauseTask(id: UUID)

    /**
     * 限定在正在下载，暂停等队列
     */
    fun requestCancelTask(id: UUID, kind: ListKind=ListKind.ActiveKind)
    fun resumeTask(id: UUID)
}