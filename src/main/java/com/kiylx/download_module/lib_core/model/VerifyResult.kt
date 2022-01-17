package com.kiylx.download_module.lib_core.model

import com.kiylx.download_module.lib_core.model.TaskResult.TaskResultCode

/**
 * task验证某些内容后，承载结果
 */
data class VerifyResult(
    var finalCode: Int = StatusCode.STATUS_INIT,
    var message: String = "",
    var taskResultCode: TaskResultCode? = null,
)