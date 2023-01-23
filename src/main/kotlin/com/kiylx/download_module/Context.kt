package com.kiylx.download_module

import com.kiylx.download_module.file.fileskit.FileKit
import com.kiylx.download_module.file.fileskit.FileKitImpl
import com.kiylx.download_module.file.file_platform.system.SysCall
import com.kiylx.download_module.file.file_platform.system.SysCallImpl
import com.kiylx.download_module.interfaces.ATaskHandler
import com.kiylx.download_module.interfaces.VerifyFactory
import com.kiylx.download_module.model.VerifyFactoryImpl
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.network.HttpManager
import com.kiylx.download_module.lib_core.repository.RepoImpl
import com.kiylx.download_module.taskhandler.DownloadTaskHandler
import com.kiylx.download_module.utils.CDelegate
import com.kiylx.download_module.view.ViewSources

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
        ::FileKitImpl
    )
//不要提前初始化！！！
//    val taskHandler: TaskHandler by lazy { TaskHandler.getInstance(limit) }
    val taskHandler: ATaskHandler by lazy { DownloadTaskHandler.instance }
    val verifyFactory: VerifyFactory by lazy { VerifyFactoryImpl() }
    val httpManager: HttpManager by lazy { HttpManager.getInstance() }

    /**
     * 外界将接口实现通过此方法注册到TaskHandler中。
     * 以此实现在下载完成后，外界自动处理下载文件，比如重命名文件并移动到某一个特定目录
     */
    fun setDownloadFinishHandle(downloadFinishHandler: ATaskHandler.IBackHandler){
            taskHandler.registerHandle(downloadFinishHandler)
    }

    fun setViewObserver(viewSources: ViewSources) {
        taskHandler.registerViewSources(viewSources)
    }

    companion object {
        const val updateViewInterval: Long = 1000L//更新下载进度时间间隔
        const val defaultUserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36"
        const val defaultDownloadLimit = 3 //默认下载任务数量限制
        const val defaultDownloadThreadNum = 8 //默认下载线程数

        var singleton: Context? = null
        fun getContextSingleton(configs: ContextConfigs): Context {
            if (singleton == null) {
                synchronized(Context::class.java) {
                    if (singleton == null) {
                        singleton = Context(configs)
                    }
                }
            }
            return singleton!!
        }
    }

    /**
     * 通过改变下面字段以传入不同的接口实现提供不同的功能。
     * 默认字段为null将使用默认接口实现。
     */
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