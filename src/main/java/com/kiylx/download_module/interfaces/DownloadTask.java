package com.kiylx.download_module.interfaces;

import com.kiylx.download_module.DownloadsListKind;
import com.kiylx.download_module.file.file_platform.FakeFile;
import com.kiylx.download_module.file.fileskit.FileKit;
import com.kiylx.download_module.model.*;
import com.kiylx.download_module.view.ViewSources;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.kiylx.download_module.ContextKt.getContext;

public abstract class DownloadTask implements Callable<TaskResult> {
    public final DownloadInfo info;
    public int moveState = 0;
    public ViewSources viewSources;
    protected FileKit fs;
    protected FakeFile fakeFile = null;
    private LifecycleCollection lifecycleCollection;
    private boolean recoveryFromDisk = false;

    public DownloadTask() {
        this.info = null;
    }

    public DownloadTask(DownloadInfo info) {
        lifecycleCollection = new LifecycleCollection();
        this.info = info;
        getLifecycleCollection().initState(TaskLifecycle.OH, TaskLifecycle.OH);
        fs = getContext().getFileKit();
    }

    public UUID getTaskId() {
        return info == null ? null : info.getUuid();
    }

    protected abstract DownloadTask initTask();

    /**
     * 停止下载
     * 完全停止下载，不接受时间或其他条件推迟下载
     * 但方法本身只是更改状态信息，需要外部实现真正的取消下载逻辑
     */
    public abstract void requestCancel();

    /**
     * 暂停下载，或者推迟下载任务
     * 此状态后，任务被暂停，待时机合适（比如延迟一定时间）或手动，重启下载任务
     * 但方法本身只是更改状态信息，需要外部实现真正的停止下载逻辑
     */
    public abstract void requestStop();

    /**
     * 方法本身只是更改状态信息，需要外部实现真正的恢复下载逻辑
     */
    public abstract void requestResume();

    public boolean isRunning() {
        return lifecycleCollection.getNowState() == TaskLifecycle.RUNNING;
    }

    public abstract DownloadInfo getInfo();


    public void setMoveState(int moveStatus) {
        this.moveState = moveStatus;
    }

    public int getMoveState() {
        return this.moveState;
    }

    public LifecycleCollection getLifecycleCollection() {
        return lifecycleCollection;
    }

    public void setLifecycleState(TaskLifecycle state) {
        this.lifecycleCollection.setLifecycleState(state);
        getInfo().setLifeCycle(state);
        syncInfo(Repo.SyncAction.UPDATE_STATE);
    }

    public boolean isStop() {
        return lifecycleCollection.getNowState() == TaskLifecycle.STOP;
    }

    public void stateStopped() {
        this.lifecycleCollection.setLifecycleState(TaskLifecycle.STOP);
    }

    public boolean isCancel() {
        return lifecycleCollection.getNowState() == TaskLifecycle.CANCEL;
    }

    public void stateCancel() {
        this.lifecycleCollection.setLifecycleState(TaskLifecycle.CANCEL);
    }

    public boolean isRecoveryFromDisk() {
        return recoveryFromDisk;
    }

    public void setRecoveryFromDisk(boolean recoveryFromDisk) {
        this.recoveryFromDisk = recoveryFromDisk;
    }

    public abstract void syncInfo(Repo.SyncAction action);

    /**
     * 与pieceTask之间的接口
     */
    public interface TaskCallback {
        /**
         * 分块线程通过此向上层更新下载任务信息
         *
         * @param pieceInfo
         * @param isRunning
         * @throws NullPointerException
         */
        void update(PieceInfo pieceInfo, boolean isRunning) throws NullPointerException;//pieceTask另task通知repo更新信息

        /**
         * 给分块线程提供文件，以存储下载内容
         *
         * @return FakeFile
         */
        FakeFile getFile();

        /**
         * 给分块线程提供任务的信息
         *
         * @return DownloadTaskInfo
         */
        DownloadInfo getDownloadInfo();
    }

    /**
     * 包含分块信息和执行分块后拿到的结果
     */
    @Deprecated
    public static class ExecPieceResult {
        public BasicConclusion globeTaskResult;//执行分块之前或之后校验得到的结论，比如错误信息
        public List<Future<PieceResult>> pieceResultList;//分块下载结果列表

        public ExecPieceResult(BasicConclusion globeTaskResult,
                               List<Future<PieceResult>> pieceResultList) {
            this.globeTaskResult = globeTaskResult;
            this.pieceResultList = pieceResultList;
        }
    }


    /**
     * 下载任务的运行状态
     */
    public static class LifecycleCollection {
        //指示downloadTask的状态，还会将状态写入downloadInfo存储，便于下载加载downloadInfo识别任务状态
        final ThreadLocal<TaskLifecycle> oldState = new ThreadLocal<>();//上一个状态
        final ThreadLocal<TaskLifecycle> nowState = new ThreadLocal<>();//当前状态

        public LifecycleCollection() {
            this.oldState.set(TaskLifecycle.OH);
            this.nowState.set(TaskLifecycle.OH);
        }

        public TaskLifecycle setLifecycleState(TaskLifecycle nowState) {
            this.oldState.set(this.nowState.get());
            this.nowState.set(nowState);
            return this.nowState.get();
        }

        public TaskLifecycle getNowState() {
            return this.nowState.get();
        }

        public TaskLifecycle getOldState() {
            return this.oldState.get();
        }

        public LifecycleCollection initState(TaskLifecycle nowState, TaskLifecycle oldState) {
            this.oldState.set(nowState);
            this.nowState.set(oldState);
            return this;
        }

        public void reSetState() {
            this.oldState.set(TaskLifecycle.OH);
            this.nowState.set(TaskLifecycle.OH);
        }

        public boolean is(TaskLifecycle taskLifecycle) {
            return this.nowState.get() == taskLifecycle;
        }
    }

}
