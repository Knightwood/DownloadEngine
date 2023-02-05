package com.kiylx.download_module.interfaces

import com.kiylx.download_module.model.DownloadInfo
import java.util.*

interface Repo {
    fun saveInfo(info: DownloadInfo)
    fun deleteInfo(uuid: UUID)
    fun queryInfo(uuid: UUID): DownloadInfo?
    fun update(info: DownloadInfo)

    /**
     * 将info同步到磁盘
     */
    fun syncInfoToDisk(info: DownloadInfo, action: SyncAction)

    /**
     * 同步数据库与通知视图更新所用
     */
    enum class SyncAction {
        ADD,  //用途：1.添加数据到数据库 2.通知界面更新
        UPDATE,  //用途：1.更新数据库信息 2.通知视图更新界面
        DELETE //用途：1.删除数据库中信息 2.通知视图更新界面
    }
}