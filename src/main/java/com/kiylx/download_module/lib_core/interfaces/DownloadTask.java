package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.file_platform.FakeFile;
import com.kiylx.download_module.lib_core.model.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class DownloadTask implements Callable<TaskResult> {
    public int moveState = 0;
    private LifecycleCollection lifecycle;
    private boolean recoveryFromDisk=false;

    public DownloadTask() {
        lifecycle = new LifecycleCollection();
    }

    protected abstract DownloadTask initTask();

    /**
     * 停止下载
     * 完全停止下载，不接受时间或其他条件推迟下载
     */
    public abstract void requestCancel();

    /**
     * 暂停下载，或者推迟下载任务
     * 此状态后，任务被暂停，待时机合适（比如延迟一定时间）或手动，重启下载任务
     */
    public abstract void requestStop();

    public abstract void requestResume();

    public boolean isRunning() {
        return lifecycle.getNowState() == TaskLifecycle.RUNNING;
    }

    public abstract DownloadInfo getInfo();


    public void setMoveState(int moveStatus) {
        this.moveState = moveStatus;
    }

    public int getMoveState() {
        return this.moveState;
    }

    public LifecycleCollection getLifecycle() {
        return lifecycle;
    }

    public void setLifecycleState(TaskLifecycle state) {
        this.lifecycle.setLifecycleState(state);
        getInfo().setLifeCycle(state);
    }

    public boolean isStop() {
        return lifecycle.getNowState() == TaskLifecycle.STOP;
    }

    public void stateStopped() {
        this.lifecycle.setLifecycleState(TaskLifecycle.STOP);
    }

    public boolean isCancel() {
        return lifecycle.getNowState() == TaskLifecycle.DROP;
    }

    public void stateCancel() {
        this.lifecycle.setLifecycleState(TaskLifecycle.DROP);
    }

    public boolean isRecoveryFromDisk() {
        return recoveryFromDisk;
    }

    public void setRecoveryFromDisk(boolean recoveryFromDisk) {
        this.recoveryFromDisk = recoveryFromDisk;
    }

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
