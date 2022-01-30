package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.lib_core.model.DownloadInfo;
import com.kiylx.download_module.lib_core.model.HeaderStore;
import com.kiylx.download_module.lib_core.model.PieceInfo;
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
    enum SyncAction {
        ADD, UPDATE, QUERY, DELETE, MODIFY
    }

}
