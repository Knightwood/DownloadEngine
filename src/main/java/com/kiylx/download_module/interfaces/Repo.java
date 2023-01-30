package com.kiylx.download_module.interfaces;

import com.kiylx.download_module.model.DownloadInfo;

import java.util.UUID;

public interface Repo {
//downloadInfo

    void saveInfo(DownloadInfo info);

    void deleteInfo(UUID id);

    DownloadInfo queryInfo(UUID uuid);

    /**
     * 将info同步到磁盘
     */
    public void syncInfoToDisk(DownloadInfo info, SyncAction action);

    /**
     * @param uuid downloadInfo 's uuid
     * @return if delete pieceInfo succeed, return true,else return false
     */
    boolean deletePieceInfo(UUID uuid);

    /**
     * 同步数据库与通知视图更新所用
     */
    enum SyncAction {
        ADD,//用途：1.添加数据到数据库 2.通知界面更新
        UPDATE,//用途：1.更新数据库信息 2.通知视图更新界面
        UPDATE_STATE,//1.更新数据库信息 2.更新下载状态，等待-下载-停止-恢复-完成
        DELETE//用途：1.删除数据库中信息 2.通知视图更新界面
    }

}
