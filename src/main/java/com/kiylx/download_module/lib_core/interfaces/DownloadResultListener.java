package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.List;
import java.util.UUID;

public interface DownloadResultListener {
    void onDownloadsCompleted();

    void onSucceed(UUID infoId);

    void onFailed(UUID infoId);

    void onERROR(UUID infoId);

    void onCanceled(UUID infoId);

    void onCompleted(UUID infoId);

    void updated(int kind, List<SimpleDownloadInfo> list);
}
