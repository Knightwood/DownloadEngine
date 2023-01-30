package com.kiylx.download_module.interfaces

import com.kiylx.download_module.model.PieceInfo
import java.util.concurrent.Callable
import com.kiylx.download_module.model.PieceResult
import java.util.UUID
import kotlin.jvm.JvmOverloads
import com.kiylx.download_module.model.StatusCode

abstract class PieceThread(var pieceInfo: PieceInfo) : Callable<PieceResult?> {
    var infoId: UUID//downloadInfo的uuid
    var blockId = 0
    @JvmField
    var isRunning = false
    @JvmField
    protected var pieceResult: PieceResult? = null
    protected var currentTimeMillis = System.currentTimeMillis()

    init {
        infoId = pieceInfo.id
        blockId = pieceInfo.blockId
    }

    /**
     *
     * @param finalCode [PieceCode]
     * @param msg message
     * @return [PieceResult]
     */
    @JvmOverloads
    fun generatePieceResult(finalCode: Int, msg: String?, throwable: Throwable? = null): PieceResult {
        pieceInfo.finalCode = finalCode
        pieceInfo.msg = msg
        pieceResult =
            PieceResult(infoId, blockId, finalCode, msg!!, pieceInfo.curBytes, pieceInfo.totalBytes, throwable)
        return pieceResult!!
    }

    var start: Long
        get() = pieceInfo.start
        set(start) {
            pieceInfo.start = start
        }

    /**
     * 1,将delta累加到分块的开始
     * 因此，若想知道分块的起始位置，需要通过计算pieceInfo的start-curBytes或者 end-totalBytes
     * 2,累加delta
     * @param delta 比起上次下载了多少数据
     */
    fun curBytesPlus(delta: Long) {
        pieceInfo.startPlus(delta)
        pieceInfo.curBytesPlus(delta)
    }

    var end: Long
        get() = pieceInfo.end
        set(end) {
            pieceInfo.end = end
        }

    //当前下载了多少
    var curBytes: Long
        get() = pieceInfo.curBytes
        set(curBytes) {
            pieceInfo.curBytes = curBytes
        }

    /**
     *  这个分块的大小
     *  若分块没有长度，即start和end都是-1，这里会得出等于1的错误结论
     */
    val totalBytes: Long
        get() = pieceInfo.end - pieceInfo.start - pieceInfo.curBytes + 1

    fun hasLength():Boolean{
        return totalBytes>0 && start>-1 && end>-1
    }

    /**
     * 描述piece执行的结果或者当前的状态
     */
    object PieceCode {
        const val init_ = StatusCode.STATUS_INIT //初始状态
        const val wait = StatusCode.STATUS_WAITING
        const val active = StatusCode.STATUS_ACTIVE
        const val stop = StatusCode.STATUS_STOPPED
        const val success = StatusCode.STATUS_SUCCESS
        const val error = StatusCode.STATUS_ERROR
    }

    companion object {
        const val BUFFER_SIZE = 8192 //8kib

        /* 在进度条更新之前必须完成的最小进度量 */
        const val MIN_PROGRESS_STEP = 65536 //64kib

        /* 更新进度条之前必须经过的最短时间, 单位：ms */
        const val MIN_PROGRESS_TIME: Long = 1500
        const val INFO_MIN_PROGRESS_TIME: Long = 800
    }
}