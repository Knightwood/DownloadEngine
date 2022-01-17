package com.kiylx.download_module.view

import com.kiylx.download_module.lib_core.model.DownloadInfo
import com.kiylx.download_module.lib_core.model.StatusCode
import java.util.*

data class SimpleDownloadInfo(
    val id: UUID? = null,
    val name: String,
    val filePath: String,
    val url: String,
    var contentLength: Long = -1,
    var currentLength: Long = 0,
    var state: Int = StatusCode.STATUS_INIT,
)

fun genSimpleDownloadInfo(info: DownloadInfo): SimpleDownloadInfo {
    return SimpleDownloadInfo(id = info.uuid,
        name = info.fileName,
        filePath = info.path,
        url = info.url,
        contentLength = info.totalBytes,
        currentLength = info.currentLength,
        state = info.statusCode)
}

class ViewsAction {
    companion object {
        const val none: Int = 0;//初始化
        const val generate: Int = 1;//生成数据
        const val update: Int = 2;//更新数据
        const val pull: Int = 3;//获得已有数据
    }
}
