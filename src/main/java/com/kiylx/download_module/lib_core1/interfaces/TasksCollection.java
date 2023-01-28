package com.kiylx.download_module.lib_core1.interfaces;

import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.model.TaskResult;
import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.view.ViewsAction;

import java.util.List;
import java.util.UUID;

public interface TasksCollection {

    DownloadTask findTask(UUID id);

    void add(DownloadTask task);

    void remove(TaskResult taskResult);

    void remove(DownloadTask task);

    /**
     * 添加task，并返回这个task，或者返回null
     */
    DownloadTask insert(DownloadTask task);

    /**
     * 移除特定id的task，并返回这个task，或者返回null
     *
     * @param infoId
     * @return
     */
    DownloadTask delete(UUID infoId);

    boolean isEmpty();

    int size();

    /**
     * @param viewsAction {@link ViewsAction} 定义了数据转换行为
     * @return 将下载数据转换为视图显示所必需的数据
     */
    List<SimpleDownloadInfo> covert(int viewsAction);
}
