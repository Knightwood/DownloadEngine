package com.kiylx.download_module.interfaces;

import com.kiylx.download_module.model.PieceInfo;
import com.kiylx.download_module.model.PieceResult;
import io.reactivex.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.Callable;

import static com.kiylx.download_module.model.StatusCode.*;

public abstract class PieceThread implements Callable<PieceResult> {
    public static final int BUFFER_SIZE = 8192; //8kib
    /* 在进度条更新之前必须完成的最小进度量 */
    public static final int MIN_PROGRESS_STEP = 65536;//64kib
    /* 更新进度条之前必须经过的最短时间, 单位：ms */
    public static final long MIN_PROGRESS_TIME = 1500;
    public static final long INFO_MIN_PROGRESS_TIME = 800;

    public UUID infoId;
    public int blockId = 0;

    public boolean isRunning = false;
    protected PieceInfo pieceInfo;
    protected PieceResult pieceResult;

    protected Long currentTimeMillis= System.currentTimeMillis();
    public PieceThread(@NonNull PieceInfo pieceInfo) {
        this.pieceInfo = pieceInfo;
        this.infoId = pieceInfo.getId();
        this.blockId = pieceInfo.getBlockId();
    }

    /**
     *
     * @param finalCode {@link pieceCode}
     * @param msg message
     * @return {@link PieceResult}
     */
    public PieceResult generatePieceResult(int finalCode, String msg) {
        return generatePieceResult(finalCode, msg, null);
    }

    public PieceResult generatePieceResult(int finalCode, String msg, Throwable throwable) {
        pieceInfo.setFinalCode(finalCode);
        pieceInfo.setMsg(msg);
        pieceResult = new PieceResult(infoId, blockId, finalCode, msg, pieceInfo.getCurBytes(), pieceInfo.getTotalBytes(), throwable);
        return pieceResult;
    }

    public long getStart() {
        return pieceInfo.getStart();
    }

    public void setStart(long start) {
        pieceInfo.setStart(start);
    }

    /**
     * 将delta累加到分块的开始
     * @param delta 比起上次下载了多少数据
     */
    public void startPlus(long delta) {
        pieceInfo.startPlus(delta);
        pieceInfo.curBytesPlus(delta);
    }

    public long getEnd() {
        return pieceInfo.getEnd();
    }

    public void setEnd(long end) {
        pieceInfo.setEnd(end);
    }

    //当前下载了多少
    public long getCurBytes() {
        return pieceInfo.getCurBytes();
    }

    public void setCurBytes(long curBytes) {
        pieceInfo.setCurBytes(curBytes);
    }

    //这个分块的大小
    public long getTotalBytes() {
        return pieceInfo.getTotalBytes();
    }

    public void setTotalBytes(long totalBytes) {
        pieceInfo.setTotalBytes(totalBytes);
    }

    /**
     * 描述piece执行的结果或者当前的状态
     */
    public static class pieceCode {
        public static final int init_ = STATUS_INIT; //初始状态
        public static final int wait = STATUS_WAITING;
        public static final int active = STATUS_ACTIVE;
        public static final int stop = STATUS_STOPPED;
        public static final int success = STATUS_SUCCESS;
        public static final int error = STATUS_ERROR;
    }

}
