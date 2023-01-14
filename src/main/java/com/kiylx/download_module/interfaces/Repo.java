package com.kiylx.download_module.interfaces;

import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.model.HeaderStore;
import com.kiylx.download_module.model.PieceInfo;
import org.jetbrains.annotations.NotNull;


import java.util.List;
import java.util.UUID;

public interface Repo {
//downloadInfo

    void saveInfo(DownloadInfo info);

    void deleteInfo(UUID id);

    DownloadInfo queryInfoById(UUID id);

    DownloadInfo queryInfo(DownloadInfo info);

    /**
     * 将info同步到磁盘
     */
    public void syncInfoToDisk(DownloadInfo info, SyncAction action);


//headers
    void updateHeader(UUID uuid, String kind, String value);

    /**
     * @param uuid
     * @param exclude 排除这些名称的header
     * @return
     */
    HeaderStore[] getHeadersById(UUID uuid, String... exclude);

    /**
     * @param uuid
     * @param include 找到特定名称的header
     * @return
     */
    HeaderStore[] getHeadersByName(UUID uuid, String... include);


//pieceInfo
    /**
     * @param uuid downloadInfo 's uuid
     * @return list of pieceInfo
     */
    @NotNull
    List<PieceInfo> queryPieceInfo(UUID uuid);

    /**
     * @param uuid downloadInfo 's uuid
     * @return if delete pieceInfo succeed, return true,else return false
     */
    boolean deletePieceInfo(UUID uuid);

    /**
     * 将pieceInfo同步到磁盘
     */
    public void syncPieceInfoToDisk(PieceInfo info, SyncAction action);


    /**
     * @param kind DownloadsListKind类中定义
     * @return 返回kind类型的下载信息集合
     */
    List<SimpleDownloadInfo> queryList(int kind);

    //OTHER

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
