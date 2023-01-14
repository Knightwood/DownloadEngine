package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.List;
import java.util.UUID;

public interface FrozenCollection {

    //将任务放进list，使任务永远不会下载
    void frozenTask(UUID infoId);

    void frozenTask(DownloadTask task);

    //将任务放回queue，使任务重新等待下载
    void activeTask(UUID id);

    boolean removeFrozenTask(DownloadTask task);

    boolean removeFrozenTask(UUID uuid);

    void addTaskToFrozen(DownloadTask task);

    DownloadTask findTaskInFrozen(UUID id);

    /**
     * @param viewsAction ViewsAction中定义了数据转换行为
     * @return 将下载数据转换为视图显示所必需的数据
     */
    List<SimpleDownloadInfo> covert(int viewsAction);

}
