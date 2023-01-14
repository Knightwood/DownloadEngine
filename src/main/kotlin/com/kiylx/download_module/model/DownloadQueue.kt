package com.kiylx.download_module.model

import com.kiylx.download_module.interfaces.DownloadTask

/**
 * 完成DownloadTask的检索，添加，删除，移动等
 */
class DownloadQueueNode {

    var nodeKind=Kind.SimpleNode
    var task :DownloadTask?=null
    var next:DownloadQueueNode?=null
    var front:DownloadQueueNode?=null


    enum class Kind{
        SimpleNode,HeadNode
    }
}
typealias DownloadQueue=DownloadQueueNode


