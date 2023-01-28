package com.kiylx.download_module.interfaces

import com.kiylx.download_module.file.file_platform.FakeFile
import com.kiylx.download_module.file.fileskit.FileKit
import com.kiylx.download_module.getContext
import com.kiylx.download_module.interfaces.Repo.SyncAction
import com.kiylx.download_module.model.*
import com.kiylx.download_module.taskhandler.ATaskHandler
import com.kiylx.download_module.view.IViewSources
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class DownloadTask(@JvmField val info: DownloadInfo) : Callable<TaskResult?> {
    var moveState = 0

    @JvmField
    var viewSources: IViewSources? = null

    @JvmField
    internal var fs: FileKit<*>

    @JvmField
    internal var fakeFile: FakeFile<*>? = null
    var lifecycleCollection: LifecycleCollection
        private set
    var isRecoveryFromDisk = false

    val taskId: UUID
        get() = info.uuid!!
    var backHandler: ATaskHandler.IBackHandler? = null //任务执行完成后的处理，可以没有

    init {
        lifecycleCollection = LifecycleCollection()
        lifecycleCollection.initState(TaskLifecycle.OH, TaskLifecycle.OH)
        fs = getContext().fileKit
    }

    protected abstract fun initTask(): DownloadTask?

    /**
     * 停止下载
     * 完全停止下载，不接受时间或其他条件推迟下载
     * 但方法本身只是更改状态信息，需要外部实现真正的取消下载逻辑
     */
    abstract fun requestCancel()

    /**
     * 暂停下载，或者推迟下载任务
     * 此状态后，任务被暂停，待时机合适（比如延迟一定时间）或手动，重启下载任务
     * 但方法本身只是更改状态信息，需要外部实现真正的停止下载逻辑
     */
    abstract fun requestStop()

    /**
     * 方法本身只是更改状态信息，需要外部实现真正的恢复下载逻辑
     */
    abstract fun requestResume()
    val isRunning: Boolean
        get() = lifecycleCollection.nowState == TaskLifecycle.RUNNING

    abstract fun getInfo(): DownloadInfo
    fun setLifecycleState(state: TaskLifecycle) {
        lifecycleCollection.setLifecycleState(state)
        getInfo().lifeCycle = state
        syncInfo(SyncAction.UPDATE_STATE)
    }

    val isStop: Boolean
        get() = lifecycleCollection.nowState == TaskLifecycle.STOP

    fun stateStopped() {
        lifecycleCollection.setLifecycleState(TaskLifecycle.STOP)
    }

    val isCancel: Boolean
        get() = lifecycleCollection.nowState == TaskLifecycle.CANCEL

    fun stateCancel() {
        lifecycleCollection.setLifecycleState(TaskLifecycle.CANCEL)
    }

    abstract fun syncInfo(action: SyncAction?)

    /**
     * 与pieceTask之间的接口
     */
    interface TaskCallback {
        /**
         * 分块线程通过此向上层更新下载任务信息
         *
         * @param pieceInfo
         * @param isRunning
         * @throws NullPointerException
         */
        @Throws(NullPointerException::class)
        fun update(pieceInfo: PieceInfo?, isRunning: Boolean) //pieceTask另task通知repo更新信息

        /**
         * 给分块线程提供文件，以存储下载内容
         *
         * @return FakeFile
         */
        fun getFile(): FakeFile<*>?

        /**
         * 给分块线程提供任务的信息
         *
         * @return DownloadTaskInfo
         */
        fun getDownloadInfo(): DownloadInfo
    }

    /**
     * 包含分块信息和执行分块后拿到的结果
     */
    @Deprecated("")
    class ExecPieceResult(//执行分块之前或之后校验得到的结论，比如错误信息
        var globeTaskResult: BasicConclusion,
        //分块下载结果列表
        var pieceResultList: List<Future<PieceResult>>
    )

    /**
     * 下载任务的运行状态
     */
    class LifecycleCollection {
        //指示downloadTask的状态，还会将状态写入downloadInfo存储，便于下载加载downloadInfo识别任务状态
        var oldState : TaskLifecycle//上一个状态
        var nowState : TaskLifecycle //当前状态

        init {
            oldState=TaskLifecycle.OH
            nowState=TaskLifecycle.OH
        }

        fun setLifecycleState(nowState: TaskLifecycle): TaskLifecycle {
            oldState=this.nowState
            this.nowState=nowState
            return this.nowState
        }

        fun initState(nowState: TaskLifecycle, oldState: TaskLifecycle): LifecycleCollection {
            this.oldState=nowState
            this.nowState=oldState
            return this
        }

        fun reSetState() {
            oldState=TaskLifecycle.OH
            nowState=TaskLifecycle.OH
        }

        fun `is`(taskLifecycle: TaskLifecycle): Boolean {
            return nowState == taskLifecycle
        }
    }


}