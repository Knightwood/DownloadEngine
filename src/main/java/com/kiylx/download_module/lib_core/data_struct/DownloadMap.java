package com.kiylx.download_module.lib_core.data_struct;

import com.kiylx.download_module.lib_core.interfaces.DownloadTask;
import com.kiylx.download_module.lib_core.interfaces.TasksCollection;
import com.kiylx.download_module.lib_core.model.TaskResult;
import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.view.ViewsAction;

import java.util.*;

import static com.kiylx.download_module.view.SimpleDownloadInfoKt.genSimpleDownloadInfo;

public class DownloadMap implements TasksCollection {
    private final Map<UUID, DownloadTask> map = new HashMap<>();
    public List<SimpleDownloadInfo> views = Collections.emptyList();
    private final ArrayList<DownloadTask> cache = new ArrayList<>();
    private boolean canAdd = true;

    @Override
    public DownloadTask findTask(UUID id) {
        for (Map.Entry<UUID, DownloadTask> entry : map.entrySet()) {
            if (entry.getKey() == id) {
                DownloadTask task = entry.getValue();
                if (task != null)
                    return task;
            }
        }
        return null;
    }

    @Override
    public void add(DownloadTask task) {
        map.put(task.getInfo().getUuid(), task);
    }

    @Override
    public void remove(TaskResult taskResult) {
        map.remove(taskResult.infoId);
    }

    @Override
    public void remove(DownloadTask task) {
        map.remove(task.getInfo().getUuid());
    }

    @Override
    public DownloadTask insert(DownloadTask task) {
        return map.put(task.getInfo().getUuid(), task);
    }

    @Override
    public DownloadTask delete(UUID infoId) {
        return map.remove(infoId);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public List<SimpleDownloadInfo> covert(int viewsAction) {
        switch (viewsAction) {
            case ViewsAction.generate:
                views = new ArrayList<>();
            case ViewsAction.update:
                map.values().forEach(downloadTask -> views.add(genSimpleDownloadInfo(downloadTask.getInfo())));
                return views;
            case ViewsAction.pull:
            default:
                return views;
        }
    }
}
