package com.kiylx.download_module.lib_core.model;

import java.util.UUID;

/**
 * 下载任务完成的状态
 * 完成是指下载完成，可能成功，也可能失败
 */
public class TaskResult {
    public UUID infoId;//downloadInfo 的UUID
    public TaskResultCode taskResultCode;
    //public TaskLifecycle taskState;
    public int finalCode = 0;
    public String message = null;

    public TaskResult(UUID infoId, TaskResultCode taskResultCode) {
        this(infoId, taskResultCode, null);
    }

    public TaskResult(UUID infoId, BasicConclusion conclusion) {
        this(infoId, null, conclusion);
    }

    public TaskResult(UUID infoId, TaskResultCode taskResultCode, BasicConclusion conclusion) {
        this.infoId = infoId;
        this.taskResultCode = taskResultCode;
        if (conclusion != null) {
            this.finalCode = conclusion.getFinalCode();
            this.message = conclusion.getMessage();
            this.taskResultCode = conclusion.getData(TaskResultCode.class);
        }
    }

    /**
     *
     * @param infoId uuid
     * @param taskResultCode {@link TaskResultCode}
     * @param msg message
     * @param finalCode {@link StatusCode}
     */
    public TaskResult(UUID infoId, TaskResultCode taskResultCode, String msg, int finalCode) {
        this.infoId = infoId;
        this.taskResultCode = taskResultCode;
        this.message = msg;
        this.finalCode = finalCode;
    }

    public TaskResult(UUID infoId, VerifyResult verifyResult) {
        this.infoId = infoId;
        this.taskResultCode = verifyResult.getTaskResultCode();
        this.message = verifyResult.getMessage();
        this.finalCode = verifyResult.getFinalCode();
    }

    public enum TaskResultCode {
        OH,//初始化
        DOWNLOAD_COMPLETE, //任务完成，文件下载成功
        FAILED,//失败
        PAUSED, //暂停下载，等待恢复
        CANCELED,//取消
        ERROR //出错
    }

    //task目前在哪个阶段
    /*public static class Kind {
        public static final int init_ = 0; //初始状态
        public static final int wait = 1; //等待状态
        public static final int active = 2; //下载状态
        public static final int finish = 3; //完成状态
    }*/
}
