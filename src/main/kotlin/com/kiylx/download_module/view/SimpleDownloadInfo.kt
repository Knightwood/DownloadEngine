package com.kiylx.download_module.view

import com.kiylx.download_module.lib_core.model.DownloadInfo
import com.kiylx.download_module.lib_core.model.StatusCode
import com.kiylx.download_module.lib_core.model.TaskLifecycle
import com.sun.org.apache.xpath.internal.operations.Bool
import java.util.*

data class SimpleDownloadInfo(
    val id: UUID? = null,
    val name: String,
    val filePath: String,
    val url: String,
    var fileSize: Long = -1,
    var currentLength: Long = 0L,
    var speed:Long=0L,// bytes/s
    var finalCode: Int = StatusCode.STATUS_INIT,//结果
    var finalMsg: String? = "null",//结果相关的信息
    var state:TaskLifecycle,//当前任务状态
    var isRunning:Boolean=false//是否正在下载
)

fun genSimpleDownloadInfo(info: DownloadInfo): SimpleDownloadInfo {
    return SimpleDownloadInfo(
        id = info.uuid,
        name = info.fileName,
        filePath = info.path,
        url = info.url,
        fileSize = info.totalBytes,
        currentLength = info.getDownloadedSize(),
        speed=0L,
        finalCode = info.finalCode,
        finalMsg = info.finalMsg,
        state = info.lifeCycle,
        isRunning = info.isRunning
    )
}

class ViewsAction {
    companion object {
        const val none: Int = 0;//初始化
        const val generate: Int = 1;//生成数据
        const val update: Int = 2;//更新数据
        const val pull: Int = 3;//获得已有数据
    }
}
