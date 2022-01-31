package com.kiylx.download_module

import com.kiylx.download_module.fileskit.FileKit
import com.kiylx.download_module.fileskit.FileKitImpl
import com.kiylx.download_module.file_platform.system.SysCall
import com.kiylx.download_module.file_platform.system.SysCallImpl
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.VerifyFactory
import com.kiylx.download_module.lib_core.model.VerifyFactoryImpl
import com.kiylx.download_module.lib_core.interfaces.Repo
import com.kiylx.download_module.lib_core.network.HttpManager
import com.kiylx.download_module.lib_core.repository.RepoImpl
import com.kiylx.download_module.utils.kotlin.CDelegate

class Context private constructor(configs: ContextConfigs) {
    private var setting: Context.ContextConfigs = configs
    var limit = setting.limit

    /**
     * 下载时使用的线程数默认期待值，但受到实际情况的限制，在不支持多线程下载的情况下，不一定会使用此值
     */
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
                    field = obj
                    field
                }
            return field
        }

    val SysCallKit: SysCall by CDelegate(setting.sysCallClazz, ::SysCallImpl)

    val fileKit: FileKit<*> by CDelegate(setting.fileKitClazz,
        ::FileKitImpl)

    val taskHandler: TaskHandler by lazy { TaskHandler.getInstance(limit) }
    val verifyFactory: VerifyFactory by lazy { VerifyFactoryImpl() }
    val httpManager: HttpManager by lazy { HttpManager.getInstance() }

    companion object {
        const val updateViewInterval: Long = 2000L
        const val defaultUserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36"
        const val defaultDownloadLimit = 3 //默认下载任务数量限制
        const val defaultDownloadThreadNum = 8 //默认下载线程数

        var singleton:Context?=null
        fun getContextSingleton(configs: ContextConfigs):Context{
            if (singleton==null){
                synchronized(Context::class.java){
                    if (singleton==null){
                        singleton= Context(configs)
                    }
                }
            }
            return singleton!!
        }
    }


    class ContextConfigs() {
        var repoClazz: Class<out Repo>? = null
        var fileKitClazz: Class<out FileKit<*>>? = null
        var sysCallClazz: Class<out SysCall>? = null
        var limit = defaultDownloadLimit
        var threadNum = defaultDownloadThreadNum
        var userAgent = defaultUserAgent
    }

}

/**
 * 不应在得到Context单例之前调用，在得到单例之前调用，理应崩溃
 */
fun getContext(): Context {
    return Context.singleton!!
}