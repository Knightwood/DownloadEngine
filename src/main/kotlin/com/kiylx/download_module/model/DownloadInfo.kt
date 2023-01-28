package com.kiylx.download_module.model

import com.kiylx.download_module.getContext
import java.util.*

data class DownloadInfo(var url: String, var fileFolder: String, var fileName: String = "") {
    var path: String = ""//文件路径
    var uuid: UUID? = null
        //标识唯一信息
        get() {
            if (field == null) {
                field = UUID.randomUUID()
            }
            return field
        }
    var mimeType: String? = null
    var extension: String = "unknow"//文件的扩展名

    //需要下载文件的总大小，其从response中获取，或者从调用它的方法中给予
    //如果提前知道确切大小，可以在生成下载信息时提前写入，那么在获取长度信息时将以此处为准
    var totalBytes: Long = -1L
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
    var referer: String = ""
    var lifeCycle: TaskLifecycle = TaskLifecycle.OH//这是task的生命周期状态

    var taskResult: TaskResult.TaskResultCode = TaskResult.TaskResultCode.OH//任务结果，成功或失败，暂停等

    //下载的详细结果
    //FinalCode 下载任务成功与否，是不是在等在网络重新下载，结果同步至此
    var finalCode = StatusCode.STATUS_INIT

    //FinalMsg  处理code表示的结果之外 额外的消息
    var finalMsg: String? = null

    var isPartialSupport = false//是否支持分块下载
    var threadCounts: Int = 0//下载这个文件而分配的线程数量。若当前任务正在下载，那设置中更改线程数量不会被应用到这个正在下载或正在暂停状态的任务

    var splitStart: Array<Long?> = emptyArray()
    var splitEnd: Array<Long?> = emptyArray()

    var checkSum: String? = null // MD5,SHA-1 SHA-256。 添加下载生成downloadinfo时添加，也可不添加 。若添加此值，在下载完成时，会校验此值
    var checkSumType = CheckSumType.md5//默认校验方式是md5

    var description: String? = null//下载描述信息

    //分块集合
    var piecesList: MutableList<PieceInfo> = mutableListOf()

    //=======================================以下是一些不需要序列化的内容
    var isRunning = false //下载任务是否正在进行

    fun getDownloadedSize(): Long {
        var size: Long = 0L
        for (i in piecesList) {
            size += i.curBytes
        }
        return size
    }

    var speed: Long = 0L// bytes/s 更新simpleDownloadInfo中的下载速度

    //返回已下载百分比
    fun getPercent(): Long {
        return (getDownloadedSize() / totalBytes)
    }

    val isDownloadSuccess: Boolean
        get() = finalCode == StatusCode.STATUS_SUCCESS

    //var pieceResultArray: Array<PieceResult>? = null//分块的结果，目前还没有使用

    fun reduceRetryCount() {
        if (retryCount > 0)
            retryCount--
    }

    fun plusRetryCount() {
        if (retryCount >= 0)
            retryCount++
    }

    fun cleanInfo() {
        finalMsg = ""
        finalCode = StatusCode.STATUS_INIT
        retryCount = 0
        fetchCount = 0
        isHasMetadata = false
        totalBytes = -1
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

                //建立数组准备存储分块信息
                info.splitStart = arrayOfNulls(threadNum)
                info.splitEnd = arrayOfNulls(threadNum)

                for (i in 0 until info.threadCounts) {
                    //为新任务，根据文件长度填写分块信息
                    info.splitStart[i] = i * blockSize
                    info.splitEnd[i] = (i + 1) * blockSize - 1
                }
                //文件结尾
                info.splitEnd[threadNum - 1] = info.totalBytes - 1
            } else {
                info.threadCounts = 1
                info.isPartialSupport = false

            }
        }

        @JvmStatic
        fun modifyMsg(info: DownloadInfo, taskResponse: TaskResponse) {
            info.taskResult = taskResponse.taskResultCode
            info.finalCode = taskResponse.finalCode
            info.finalMsg = taskResponse.message
        }

        @JvmStatic
        fun modifyMsg(info: DownloadInfo, finalCode: Int, msg: String) {
            info.finalCode = finalCode
            info.finalMsg = msg
        }


    }
}

/**
 * 1.检查线程数量本身是否合法
 *
 * 2.检查线程数量在"最小可用多线程下载尺寸"下是否合法
 *
 * 如果不合法，自动修改为合法的值；
 */
fun DownloadInfo.fixThreadNumBySize() {
    if (!isPartialSupport || totalBytes < getContext().config.minSizeUseMultiThread) {
        //不支持多线程下载或者文件尺寸太小不适合用多线程下载
        threadCounts = 1
    } else {
        if (threadCounts <= 0 || threadCounts > getContext().config.maxDownloadThreadNum) {
            //生成下载信息时，未指定线程数量.或者线程数量超出多线程下载阈值
            threadCounts = getContext().config.downloadThreadNum
        }
    }
}

/**
 * 描述分块下载
 * 比如分块大小是10,整个任务的大小是20，分两个线程下载
 * 那么，分块1的下载范围是0-9,分块2是10-20
 */
data class PieceInfo @JvmOverloads constructor(
    val id: UUID, //downloadInfo的uuid
    val blockId: Int = 0,//分块编号

    var start: Long = -1,
    var end: Long = -1,

    ) {

    var totalBytes: Long = end - start + 1        //此分块的完整大小//初始化分块大小信息

    var curBytes: Long = 0//当前分块已经下载了多少
    var finalCode: Int = StatusCode.STATUS_INIT
    var msg: String? = null

    //=======================================以下是一些不需要序列化的内容
    var speed: Long = 0L

    fun startPlus(delta: Long) {
        start += delta
    }

    fun curBytesPlus(delta: Long) {
        curBytes += delta
    }

    fun clean() {
        finalCode = StatusCode.STATUS_INIT
        msg = null
    }
}

class CheckSumType {
    companion object {
        const val md5 = 0
        const val sha1 = 1
        const val sha256 = 2
    }
}