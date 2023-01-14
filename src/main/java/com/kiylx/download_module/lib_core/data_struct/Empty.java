package com.kiylx.download_module.lib_core.data_struct;

import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.lib_core.interfaces.TasksCollection;
import com.kiylx.download_module.model.TaskResult;
import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Empty implements TasksCollection {
    @Override
    public DownloadTask findTask(UUID id) {
        return null;
    }

    @Override
    public void add(DownloadTask task) {

    }

    @Override
    public void remove(TaskResult taskResult) {

    }

    @Override
    public void remove(DownloadTask task) {

    }

    @Override
    public DownloadTask insert(DownloadTask task) {
        return null;
    }

    @Override
    public DownloadTask delete(UUID infoId) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public List<SimpleDownloadInfo> covert(int viewsAction) {
        return new ArrayList<>();
    }
}
