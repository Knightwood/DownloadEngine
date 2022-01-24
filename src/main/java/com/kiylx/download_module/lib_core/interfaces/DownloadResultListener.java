package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.List;
import java.util.UUID;

public interface DownloadResultListener {
    void updated(List<SimpleDownloadInfo> list);
}
