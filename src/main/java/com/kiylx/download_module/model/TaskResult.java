package com.kiylx.download_module.model;

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

    public TaskResult(UUID infoId, TaskResponse taskResponse) {
        this.infoId = infoId;
        this.taskResultCode = taskResponse.getTaskResultCode();
        this.message = taskResponse.getMessage();
        this.finalCode = taskResponse.getFinalCode();
    }

    public enum TaskResultCode {
        OH,//初始化
        DOWNLOAD_COMPLETE, //下载任务完成，文件下载成功
        FAILED,//下载失败
        PAUSED, //暂停下载，等待恢复
        CANCELED,//取消
        ERROR //出错
    }

}
