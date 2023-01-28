package com.kiylx.download_module.lib_core1.interfaces;

import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.List;

public interface DownloadResultListener {
    void updatedActive(List<SimpleDownloadInfo> list);

    void updatedWait(List<SimpleDownloadInfo> list);

    void updatedFinish(List<SimpleDownloadInfo> list);
}
