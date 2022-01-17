package com.kiylx.download_module.lib_core.model

import java.util.UUID
import java.io.File
import java.lang.IllegalArgumentException

class DownloadInfo(var url: String, var path: String, var fileName: String = "", threadNum: Int = 1) {
    var uuid: UUID? = null
        //标识唯一信息
        get() {
            if (field == null) {
                field = UUID.randomUUID()
            }
            return field
        }
    var mimeType: String? = null
    var totalBytes: Long = -1L//需要下载文件的总大小，其从response中获取，或者从调用它的方法中给予
    var isHasMetadata = false
    var fetchCount = 0//初始化后，计数是0,获取一次metadata计数加一  //MetaData
    var retryAfter = 0//多久后重新尝试下载
    var retryCount: Int = 0
        //重新尝试的次数
        set(value) {
            field = value
            if (field < 0)
                field = 0
        }
    var userAgent: String? = null

    var isRunning = false //下载任务是否正在进行
    var statusCode = StatusCode.STATUS_INIT//StatusCode 下载任务成功与否，是不是在等在网络重新下载，结果同步至此
    var statusMsg: String? = null//StatusMsg  处理code表示的结果之外 额外的消息

    var isPartialSupport = false//是否支持分块下载
    var threadCounts: Int = 1
        //下载这个文件而分配的线程数量。若当前任务正在下载，那设置中更改线程数量不会被应用到这个正在下载或正在暂停状态的任务
        set(value) {
            if (value <= 0)
                throw IllegalArgumentException("Piece number can't be less or equal zero")
            if (!isPartialSupport && value > 1)
                throw  IllegalStateException("The download doesn't support partial download")

            if ((totalBytes <= 0 && value != 1) || (totalBytes in 1 until threadCounts))
                throw  IllegalStateException("The number of pieces can't be more than the number of total bytes")
            field = value
        }
    var blockSize: Long = -1//下载时根据分配线程数量（threadNum）决定的文件分块大小。（最后一个分块可能会小于或大于其他分块大小）
    var splitStart: Array<Long> = emptyArray()
    var splitEnd: Array<Long> = emptyArray()

    /**
     * 当前文件已下载的大小,
     * 需要百分比的话，用contentLength和totalLength自行计算，结果要转成float类型
     */
    var currentLength: Long = 0
        get() {
            var unDownloadPart: Long = 0 //未下载的部分
            for (i in 0 until threadCounts) {
                unDownloadPart += splitEnd[i] - splitStart[i] + 1
            }
            //设置已下载的长度
            currentLength = totalBytes - unDownloadPart
            return field
        }
    val isDownloadSuccess: Boolean
        get() = statusCode == StatusCode.STATUS_SUCCESS

    /**
     * @return 返回下载进度, float类型
     * 文件的下载线程数就是文件分块的标号，
     * 那么分块的结束减去分块的开始就是未下载的部分
     */
    val percent: Float
        get() = currentLength.toFloat() / totalBytes.toFloat()//返回已下载百分比
    val intPercent: Int
        get() = (currentLength.toFloat() / totalBytes.toFloat() * 100).toInt()
    val uUIDString: String
        get() = uuid.toString()

    var pieceResultArray: Array<PieceResult>? = null//分块的结果，目前还没有使用

    var checkSum:String?=null // MD5, SHA-256。 添加下载生成downloadinfo时添加，也可不添加 。若添加此值，在下载完成时，会校验此值

    fun reduceRetryCount() {
        if (retryCount > 0)
            retryCount--
    }
    fun plusRetryCount() {
        if (retryCount >= 0)
            retryCount++
    }
    fun cleanInfo() {
        statusMsg = ""
        statusCode = StatusCode.STATUS_INIT
        retryCount = 0
        fetchCount = 0
        isHasMetadata = false
        totalBytes = -1
    }



    init {
        if (fileName == "") {
            this.fileName = url.substring(url.lastIndexOf(File.separator))  //斜杠不能丢，因为是路径加文件名，如果文件名不包含“/”，那路径会不正确会出错
        }
        this.threadCounts = threadNum
    }

    companion object {
        /*
     * The minimum amount of time that the download manager accepts for
     * a Retry-After response header with a parameter in delta-seconds
     */
        const val minRetryAfter = 30 /* 30 s */

        /*
       * The maximum amount of time that the download manager accepts for
       * a Retry-After response header with a parameter in delta-seconds
       */
        const val maxRetryAfter = 24 * 60 * 60 /* 24 h */
        const val defaultFileName = "downloadFile" //默认文件名称

        /**
         * 处理下载信息，处理分块大小
         */
        @JvmStatic
        fun allocPieceFileSize(info: DownloadInfo) {
            //新任务
            if (info.isPartialSupport && info.totalBytes != -1L) {
                val threadNum = info.threadCounts
                val blockSize = info.totalBytes / threadNum
                info.blockSize = blockSize
                //建立数组准备存储分块信息
                info.splitStart = arrayOf()
                info.splitEnd = arrayOf()
                for (i in 0 until info.threadCounts) {
                    //为新任务，根据文件长度填写分块信息
                    info.splitStart[i] = i * blockSize
                    info.splitEnd[i] = (i + 1) * blockSize - 1
                }
                //文件结尾
                info.splitEnd[threadNum - 1] = info.totalBytes
            } else {
                info.threadCounts = 1
                info.isPartialSupport = false
                info.blockSize = info.totalBytes
            }
        }

        @JvmStatic
        fun modifyMsg(info: DownloadInfo, verifyResult: VerifyResult) {
            info.statusCode = verifyResult.finalCode
            info.statusMsg = verifyResult.message
        }

        @JvmStatic
        fun modifyMsg(info: DownloadInfo, finalCode: Int, msg: String) {
            info.statusCode = finalCode
            info.statusMsg = msg
        }
    }
}

/**
 * 描述分块下载
 */
class PieceInfo @JvmOverloads constructor(
    val id: UUID,
    val blockId: Int = 0,

    var start: Long = -1,
    var end: Long = -1,
    var curBytes: Long = 0,//当前分块已经下载了多少
    var totalBytes: Long = -1,//此分块的完整大小

    var finalCode: Int= StatusCode.STATUS_INIT,
    var msg: String? = null,
) {
    fun startPlus(delta:Long){
        start+=delta
    }
    fun curBytesPlus(delta: Long){
        curBytes+=delta
    }
    fun clean() {
        finalCode = StatusCode.STATUS_INIT
        msg = null
    }
}