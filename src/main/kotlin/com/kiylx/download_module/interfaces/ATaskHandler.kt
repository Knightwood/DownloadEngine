package com.kiylx.download_module.interfaces

import com.kiylx.download_module.file.FileUtils
import com.kiylx.download_module.model.DownloadInfo
import com.kiylx.download_module.utils.DigestUtils
import com.kiylx.download_module.view.ViewSources
import java.io.IOException

/**
 * 一个下载任务管理器应该有的方法
 */
abstract class ATaskHandler : ITaskHandler {
    //外界把接口实现注册到这里，以此实现在下载完成后，另外界自动处理后续操作，比如重命名文件并移动到某一个特定目录
    var iBackHandler: IBackHandler? = null
    var viewSources: ViewSources? = null

    override fun registerViewSources(viewSources: ViewSources) {
        if (this.viewSources == null) this.viewSources = viewSources
    }

    override fun unRegisterViewSources(viewSources: ViewSources) {
        //移除接口
        this.viewSources = null
    }

    override fun registerHandle(taskHandler: IBackHandler) {
        if (this.iBackHandler == null) this.iBackHandler = taskHandler
    }

    override fun unRegisterHandle() {
        //移除接口
        iBackHandler = null
    }

    /**
     * 验证完整性
     */
    fun verifyChecksum(task: DownloadTask): Boolean {
        val info = task.getInfo()
        var hash = info.checkSum
        if (hash != null && !hash.isEmpty()) {
            //todo 校验文件 sha256 md5
            try {
                if (DigestUtils.isMd5Hash(info.checkSum)) {
                    hash = FileUtils.calcHashSum(info, false)

                } else if (DigestUtils.isSha256Hash(info.checkSum)) {
                    hash = FileUtils.calcHashSum(info, true)

                } else {
                    throw IllegalArgumentException("Unknown checksum type:" + info.checkSum!!)
                }

            } catch (e: IOException) {
                return false
            }

            return (hash != null && hash.equals(info.checkSum!!, ignoreCase = true))
        }
        return false
    }


    /**
     * 下载任务完成后，交给后处理
     */
    interface IBackHandler {
        fun handle(info: DownloadInfo?)
    }
}