package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.file_platform.FakeFile;
import com.kiylx.download_module.lib_core.model.*;
import com.kiylx.download_module.view.ViewSources;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class DownloadTask implements Callable<TaskResult>{
    public int moveState = 0;
    public ViewSources viewSources;
    private LifecycleCollection lifecycleCollection;
    private boolean recoveryFromDisk = false;

    public DownloadTask() {
        lifecycleCollection = new LifecycleCollection();
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
     * task与pieceTask之间的接口
     */
    public interface TaskCallback {
        void update(PieceInfo pieceInfo, boolean isRunning) throws NullPointerException;//pieceTask另task通知repo更新信息

        FakeFile getFile();

        DownloadInfo getDownloadInfo();
    }

    /**
     * 包含分块信息和执行分块后拿到的结果
     */
    public static class ExecPieceResult {
        public BasicConclusion globeTaskResult;//执行分块之前或之后校验得到的结论，比如错误信息
        public List<Future<PieceResult>> pieceResultList;//分块下载结果列表

        public ExecPieceResult(BasicConclusion globeTaskResult,
                               List<Future<PieceResult>> pieceResultList) {
            this.globeTaskResult = globeTaskResult;
            this.pieceResultList = pieceResultList;
        }
    }


    public static class LifecycleCollection {
        //指示downloadTask的状态，还会将状态写入downloadInfo存储，便于下载加载downloadInfo识别任务状态
        final ThreadLocal<TaskLifecycle> oldState = new ThreadLocal<>();
        final ThreadLocal<TaskLifecycle> nowState = new ThreadLocal<>();

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
