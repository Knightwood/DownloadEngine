package com.kiylx.download_module.interfaces;

import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.model.TaskResponse;

public interface VerifyFactory {

    /**
     * 这个任务是否只能用单线程下载
     * @return
     */
    public boolean singleThread();

    /**
     * 验证存储空间是否够用
     */
    boolean canSaveFile();

    TaskResponse verify(DownloadInfo info);
    boolean initFile(DownloadInfo info);

}
