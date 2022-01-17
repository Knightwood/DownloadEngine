package com.kiylx.download_module.lib_core.interfaces;

import com.kiylx.download_module.lib_core.model.TaskResult;

import java.util.UUID;

public interface TaskFinishProcess {

    public void onCompleted(UUID id);

    public void onERROR(UUID infoId);

    public void onCanceled(UUID infoId);

    public void onFailed(UUID infoId);

    public void onPaused(TaskResult taskResult);
}
