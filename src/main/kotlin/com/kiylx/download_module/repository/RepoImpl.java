package com.kiylx.download_module.repository;

import com.kiylx.download_module.interfaces.Repo;
import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.model.HeaderStore;
import com.kiylx.download_module.model.PieceInfo;
import com.kiylx.download_module.view.SimpleDownloadInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 每一个下载任务都有一个文件存储下载信息
 * 文件以下载信息中的文件名为名称，以“session”为后缀，存储于某个位置，位置由context指定,操作系统不同，位置不同
 */
public class RepoImpl implements Repo {
    private static volatile Repo mRepo;

    public static Repo getInstance() {
        if (mRepo == null) {
            synchronized (Repo.class) {
                if (mRepo == null) {
                    mRepo = new RepoImpl();
                }
            }
        }
        return mRepo;
    }

    private RepoImpl() {
    }


    @Override
    public void saveInfo(DownloadInfo info) {

    }

    @Override
    public DownloadInfo queryInfo(DownloadInfo info) {
        return null;
    }

    @Override
    public void deleteInfo(UUID id) {

    }

    @Override
    public DownloadInfo queryInfoById(UUID id) {
        return null;
    }

    /**
     * 以uuid查找到此id下的header,再从结果中找到kind的值
     * 例如：
     * uuid：365651
     * headerName：ETag
     * 就是查找365651名下所有header中，header名称为ETag的值
     * 找到值，并更新
     */
    @Override
    public void updateHeader(UUID uuid, String headerName, String value) {
//todo
    }

    @Override
    public HeaderStore[] getHeadersById(UUID uuid, String... exclude) {
        //todo
        return new HeaderStore[0];
    }

    @Override
    public HeaderStore[] getHeadersByName(UUID uuid, String... include) {
        //todo
        return new HeaderStore[0];
    }

    @Override
    public List<SimpleDownloadInfo> queryList(int kind) {
        //todo
        return null;
    }

    @Override
    public void syncInfoToDisk(DownloadInfo info, SyncAction action) {
        //todo
        //更新存储库信息
        boolean isExist = queryInfo(info) != null;
        if (!isExist && (action == SyncAction.UPDATE || action == SyncAction.ADD)) {
            saveInfo(info);
        } else {

        }
    }

    @NotNull
    @Override
    public List<PieceInfo> queryPieceInfo(UUID uuid) {
        return Collections.emptyList();
    }

    @Override
    public boolean deletePieceInfo(UUID uuid) {
        //todo
        return false;
    }

    @Override
    public void syncPieceInfoToDisk(PieceInfo info, SyncAction action) {
    //todo
    }

}
