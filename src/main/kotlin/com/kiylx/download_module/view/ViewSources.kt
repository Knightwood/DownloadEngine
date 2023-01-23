package com.kiylx.download_module.view

import com.kiylx.download_module.interfaces.DownloadTask
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.interfaces.Repo.SyncAction.*
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.model.MoveStatus
import com.kiylx.download_module.model.TaskLifecycle
import com.kiylx.download_module.model.TaskLifecycle.*

class ViewSources private constructor() {
    //  val activeInfos: MutableList<SimpleDownloadInfo> = mutableListOf()
    //  val waitInfos: MutableList<SimpleDownloadInfo> = mutableListOf()
    val mainInfos: MutableList<SimpleDownloadInfo> = mutableListOf()//包括正在下载和等待下载的条目
    val finishInfos: MutableList<SimpleDownloadInfo> = mutableListOf()
    var observer: ViewObserver? = null

    fun setViewObserver(viewObserver: ViewObserver) {
        if (observer == null) observer = viewObserver
    }

    fun removeViewObserver() {
        //移除接口
        observer = null
    }

    fun notifyViewsChanged(
        info: DownloadInfo,
        action: Repo.SyncAction,
        lifecycleCollection: DownloadTask.LifecycleCollection
    ) {
        when (action) {
            ADD -> insertTask(info) //新任务
            UPDATE -> observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.UPDATE_PROGRESS) //进度更新
            DELETE -> removeTask(info) //删除任务
            UPDATE_STATE -> { //状态改变
                val oldState = lifecycleCollection.oldState
                val nowState = lifecycleCollection.nowState
                when (MoveStatus.guessMoveState(oldState, nowState)) {//判断task转换到了哪个状态
                    MoveStatus.MOVE_TO_ACTIVE, MoveStatus.MOVE_TO_WAITING, MoveStatus.MOVE_TO_FROZEN -> {
                        if (guessQueue(oldState) == TaskQueue.FINISH) {//判断task之前是哪个状态
                            finishInfos.remove(info.simpleDownloadInfo)
                            mainInfos.add(info.simpleDownloadInfo)
                            observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.FINISH_TO_DOWNLOAD) //更新
                        } else {
                            //task没有从下载到完成的转换，仅是从等待到下载，或是下载到等待的转换。
                            observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.UPDATE_INFO_STATE) //更新
                        }
                    }
                    MoveStatus.MOVE_TO_FINISH -> {
                        if (guessQueue(oldState) == TaskQueue.WAIT_ACTIVE) {
                            mainInfos.remove(info.simpleDownloadInfo)
                            finishInfos.add(info.simpleDownloadInfo)
                        }
                        observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.DOWNLOADING_TO_FINISH) //更新
                    }
                }
            }
        }
    }

    private fun insertTask(info: DownloadInfo) {
        when (info.lifeCycle) {
            OH, CREATE, RESTART, START, RUNNING -> {
                mainInfos.add(info.simpleDownloadInfo)
            }
            else -> {}
        }
        observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.NEW_DOWNLOAD) //更新
    }

    private fun removeTask(info: DownloadInfo) {
        when (info.lifeCycle) {
            OH, CREATE, RESTART, START, RUNNING -> {
                mainInfos.remove(info.simpleDownloadInfo)
                observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.REMOVE_DOWNLOAD) //更新
            }
            SUCCESS, STOP, CANCEL, FAILED -> {
                finishInfos.remove(info.simpleDownloadInfo)
                observer?.notifyViewUpdate(info, info.lifeCycle,UpdateStatus.REMOVE_FINISHED) //更新
            }
        }

    }

    companion object {
        val viewSourcesInstance: ViewSources by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ViewSources() }
    }
}

enum class TaskQueue {
    WAIT_ACTIVE, FINISH
}

fun guessQueue(lifecycle: TaskLifecycle): TaskQueue {
    when (lifecycle) {
        OH, CREATE, START, STOP, RUNNING -> return TaskQueue.WAIT_ACTIVE
        SUCCESS, CANCEL, FAILED -> return TaskQueue.FINISH
        else -> {}
    }
    return TaskQueue.FINISH
}

enum class UpdateStatus {
    NEW_DOWNLOAD,//添加一个新任务到main
    REMOVE_FINISHED,//移除已完成任务
    REMOVE_DOWNLOAD,//移除赈灾运行的任务
    DOWNLOADING_TO_FINISH,//task移动队列main-》finish
    FINISH_TO_DOWNLOAD,//task移动队列finish-》main
    UPDATE_PROGRESS,
    UPDATE_INFO_STATE//更新状态 main中每个task状态变化
}

interface ViewObserver {
    fun notifyViewUpdate(info: DownloadInfo?, lifeCycle: TaskLifecycle?, updateStatus: UpdateStatus)
}