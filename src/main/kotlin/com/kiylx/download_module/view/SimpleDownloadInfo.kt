package com.kiylx.download_module.view

import com.kiylx.download_module.lib_core.model.DownloadInfo
import com.kiylx.download_module.lib_core.model.StatusCode
import com.kiylx.download_module.lib_core.model.TaskLifecycle
import com.kiylx.download_module.lib_core.model.TaskResult
import java.util.*

data class SimpleDownloadInfo(
    val uuid: UUID? = null,
    val fileName: String,
    val filePath: String,
    val url: String,
    var fileSize: Long = -1,
    var currentLength: Long = 0L,
    var speed: Long = 0L,// bytes/s

    var finalCode: Int = StatusCode.STATUS_INIT,//结果
    var finalMsg: String? = "null",//结果相关的信息
    var state: TaskLifecycle,//当前任务状态
    var isRunning: Boolean = false,//是否正在下载
    var taskResult:TaskResult.TaskResultCode
) {
    fun print() {
        print("uuid: $uuid \n" +
                " 名称: $fileName \n" +
                " 文件大小: $fileSize \n" +
                " 速度: $speed \n" +
                "state：$state \n"+
                "finalCode: $finalCode \n"+
                "finalMsg: $finalMsg \n"+
                "------------------- \n"
        )
    }
}

fun genSimpleDownloadInfo(info: DownloadInfo): SimpleDownloadInfo {
    return SimpleDownloadInfo(
        uuid = info.uuid,
        fileName = info.fileName,
        filePath = info.path,
        url = info.url,
        fileSize = info.totalBytes,
        currentLength = info.getDownloadedSize(),
        speed = 0L,
        finalCode = info.finalCode,
        finalMsg = info.finalMsg,
        state = info.lifeCycle,
        isRunning = info.isRunning,
        taskResult = info.taskResult
    )
}

/**
 * 综合各方面数据得出任务出于什么状态
 */
enum class TaskState(val code: Int) {
    UNKNOWN(0),
    WAITTING(1),
    DOWNLOADING(2),
    COMPLETED(3),
    CANCELED(4),
    DOWNLOAD_FAILED(5);
}

class ViewsAction {
    companion object {
        const val none: Int = 0;//初始化
        const val generate: Int = 1;//生成数据
        const val update: Int = 2;//更新数据
        const val pull: Int = 3;//获得已有数据
    }
}
