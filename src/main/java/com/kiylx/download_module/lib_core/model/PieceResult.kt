package com.kiylx.download_module.lib_core.model

import java.util.*

/**
 * 分块下载结果
 */
data class PieceResult @JvmOverloads constructor(
    val id: UUID,
    val blockId: Int = 0,

    val finalCode: Int,
    val msg: String = "",

    val curBytes: Long = 0,//当前分块已经下载了多少
    val totalBytes: Long = -1,//此分块的完整大小
    var throwable: Throwable? = null,
)