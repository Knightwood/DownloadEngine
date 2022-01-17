package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.lib_core.model.PieceInfo;
import com.kiylx.download_module.lib_core.model.PieceResult;
import io.reactivex.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.Callable;

import static com.kiylx.download_module.lib_core.model.StatusCode.*;

public abstract class PieceThread implements Callable<PieceResult> {
    public UUID infoId;
    public int blockId = 0;

    public boolean isRunning = false;
    protected PieceInfo pieceInfo;
    protected PieceResult pieceResult;

    public PieceThread(@NonNull PieceInfo pieceInfo) {
        this.pieceInfo = pieceInfo;
        this.infoId = pieceInfo.getId();
        this.blockId = pieceInfo.getBlockId();
    }

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

    public void startPlus(long delta) {
        pieceInfo.startPlus(delta);
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

    public void curBytesPlus(long delta) {
        pieceInfo.curBytesPlus(delta);
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
