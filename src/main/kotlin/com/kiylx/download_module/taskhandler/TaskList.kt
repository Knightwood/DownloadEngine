package com.kiylx.download_module.taskhandler

import com.kiylx.download_module.getContext
import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.taskhandler.ListKind.*
import java.util.*

/**
 * 存储下载任务（downloadTask），被DownloadTaskHandler所使用，并提供改变他们位置和状态的方法
 */
class TaskList {
    //同时下载任务限制
    var downloadLimit = getContext().limit

    /**
     * 包含正在下载和等待下载的任务
     */
    var active = hashMapOf<UUID, DownloadTaskWrapper>()

    /**
     * 暂停状态的任务
     */
    var paused = hashMapOf<UUID, DownloadTaskWrapper>()

    /**
     * 包含完成和失败的任务
     */
    var finished = hashMapOf<UUID, DownloadTaskWrapper>()

    /**
     * 如果下载队列已满（正在下载的数量等于下载数量限制），返回true
     */
    fun downloadingIsFull(): Boolean {
        return active.size > downloadLimit
    }

    /**
     * 返回等待下载的任务数量
     */
    fun waitKindSize(): Int {
        val num=active.size - downloadLimit
        return if (num <= 0) 0 else num
    }

    /**
     * 获取active队列中，pos位置处的任务
     */
    fun changeTaskKind(pos:Int){
        if (pos<0 || pos>active.size)
            return
        return
    }

    /**
     * 添加到特定TaskList
     */
    fun add(task: DownloadTask, kind: ListKind = ActiveKind) {
        when (kind) {
            None, Stopped -> {
                paused[task.taskId] = DownloadTaskWrapper(task).apply {
                    listKind = kind
                }
            }
            ActiveKind, WaitKind -> {
                active[task.taskId] = DownloadTaskWrapper(task).apply {
                    listKind = if (downloadingIsFull())
                        WaitKind
                    else
                        kind
                }
            }
            SucceedKind, ErrorKind -> {
                finished[task.taskId] = DownloadTaskWrapper(task).apply {
                    listKind = kind
                }
            }
        }
    }

    fun remove(task: DownloadTask, kind: ListKind = ActiveKind): DownloadTaskWrapper? = remove(task.taskId, kind)


    fun remove(taskId: UUID, kind: ListKind = ActiveKind): DownloadTaskWrapper? {
        return when (kind) {
            None, Stopped -> {
                paused.remove(taskId)
            }
            ActiveKind, WaitKind -> {
                active.remove(taskId)
            }
            SucceedKind, ErrorKind -> {
                finished.remove(taskId)
            }
        }
    }

    /**
     * 添加到特定TaskList
     */
    fun find(taskId: UUID, kind: ListKind = ActiveKind): DownloadTask? = when (kind) {
        None, Stopped -> {
            paused[taskId]?.task
        }
        ActiveKind, WaitKind -> {
            active[taskId]?.task
        }
        SucceedKind, ErrorKind -> {
            finished[taskId]?.task
        }
    }

    fun move(taskId: UUID, oldKind: ListKind = ActiveKind, newKind: ListKind) {
        remove(taskId, oldKind)?.let {
            add(it.task, newKind)
        }
    }
}

class DownloadTaskWrapper(val task: DownloadTask) {
    var listKind = ListKind.None
}

//ActiveKind, WaitKind属于一个队列(正在下载)
//SucceedKind,ErrorKind属于一个队列(下载结束)
enum class ListKind {
    None,//如果是此状态，归入暂停状态下
    WaitKind, //等待下载，而不是暂停状态
    ActiveKind,//正在下载
    Stopped,//暂停下载
    SucceedKind, //结束下载-下载成功/取消下载
    ErrorKind//结束下载-下载失败
}