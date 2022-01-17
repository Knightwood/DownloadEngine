package com.kiylx.download_module

import com.kiylx.download_module.fileio.filesystem.FileKit
import com.kiylx.download_module.fileio.filesystem.FileKitImpl
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.VerifyFactory
import com.kiylx.download_module.lib_core.model.VerifyFactoryImpl
import com.kiylx.download_module.lib_core.interfaces.Repo
import com.kiylx.download_module.lib_core.repository.RepoImpl
import com.kiylx.download_module.fileio.system.SysCall
import com.kiylx.download_module.fileio.system.SysCallImpl
import com.kiylx.download_module.utils.kotlin.CDelegate

class Context(configs: ContextConfigs) {
    private var setting: Context.ContextConfigs = configs
    var limit = setting.limit
    var defaultThreadNum = setting.threadNum//DownloadInfo中线程数不存在或不合法时使用这里的缺省值
    var userAgent = setting.userAgent
    var repo: Repo? = null
        get() {
            if (field == null)
                return if (setting.repoClazz == null) {
                    field = RepoImpl.getInstance()
                    field
                } else {
                    val obj = setting.repoClazz!!.getDeclaredConstructor().newInstance()
                    field = obj as Repo?
                    field
                }
            return field
        }

    val SysCallKit: SysCall by CDelegate(setting.sysCallClazz,::SysCallImpl)

    val fileKit: FileKit<*> by CDelegate(setting.fileKitClazz, ::FileKitImpl)

    val taskHandler: TaskHandler by lazy { TaskHandler.getInstance() }
    val verifyFactory: VerifyFactory by lazy { VerifyFactoryImpl() }


    companion object {
        const val updateViewInterval: Long = 2000L
        const val defaultUserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36"
        const val downloadLimit = 5 //默认下载任务数量限制
        const val downloadThreadNum = 8 //默认下载线程数
    }


    class ContextConfigs() {
        var repoClazz: Class<out Repo>? = null
        var fileKitClazz: Class<out FileKit<*>>? = null
        var sysCallClazz: Class<out SysCall>? = null
        var limit = downloadLimit
        var threadNum = downloadThreadNum
        var userAgent = defaultUserAgent
    }

}

/**
 * 不应在downloads初始化之前调用
 */
fun getContext(): Context {
    return Downloads.downloadsInstance().mContext
}