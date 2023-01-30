package com.kiylx.download_module.repository;

import com.kiylx.download_module.interfaces.Repo;
import com.kiylx.download_module.model.DownloadInfo;

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
    public DownloadInfo queryInfo(UUID uuid) {
        return null;
    }

    @Override
    public void deleteInfo(UUID id) {

    }


    @Override
    public void syncInfoToDisk(DownloadInfo info, SyncAction action) {
        //todo
        //更新存储库信息
    }

    @Override
    public boolean deletePieceInfo(UUID uuid) {
        //todo
        return false;
    }


}
