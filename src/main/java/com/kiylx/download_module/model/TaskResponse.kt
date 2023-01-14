package com.kiylx.download_module.model

import com.kiylx.download_module.model.TaskResult.TaskResultCode

/**
 * downloadTask在下载之前或之后验证某些内容后，将信息记录在此类
 * downloadTask在成功执行分块下载之后，综合每个分块线程返回的PieceResult，得出一个分块下载结果信息，并记录在此类中
 */
data class TaskResponse(
    var finalCode: Int = StatusCode.STATUS_INIT,
    var message: String = "",
    var taskResultCode: TaskResultCode=TaskResultCode.OH,
)