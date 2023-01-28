package com.kiylx.download_module

import com.kiylx.download_module.file.file_platform.system.SysCall
import com.kiylx.download_module.file.file_platform.system.SysCallImpl
import com.kiylx.download_module.file.fileskit.FileKit
import com.kiylx.download_module.file.fileskit.FileKitImpl
import com.kiylx.download_module.interfaces.Repo
import com.kiylx.download_module.interfaces.VerifyFactory
import com.kiylx.download_module.lib_core1.DownloadTaskHandler
import com.kiylx.download_module.model.VerifyFactoryImpl
import com.kiylx.download_module.network.HttpManager
import com.kiylx.download_module.taskhandler.ATaskHandler
import com.kiylx.download_module.utils.CreateClassInstance
import com.kiylx.download_module.view.IViewSources
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * 如果组件依赖此类，此类的初始化要在其余类之前。
 * 因此，依赖此类的类，在这里要延迟初始化。
 */
class Context private constructor(configs: ContextConfigs) {
    var contextScope: CoroutineScope = CoroutineScope(CoroutineName("context-scope") + Dispatchers.Default)
    private var setting: ContextConfigs = configs
    val config = setting//对外暴露

    //=======================================类实例=======================================//
    val sysCallKit: SysCall by CreateClassInstance(setting.sysCallClazz) { SysCallImpl() }
    val fileKit: FileKit<*> by CreateClassInstance(setting.fileKitClazz) { FileKitImpl() }
    val repo: Repo by CreateClassInstance(setting.repoClazz) {
        setting.repoClazz!!.getDeclaredConstructor().newInstance()
    }

    //保持延迟初始化，不要提前初始化！！！
    val taskHandler: ATaskHandler by lazy { DownloadTaskHandler.instance }
    val verifyFactory: VerifyFactory by lazy { VerifyFactoryImpl() }
    val httpManager: HttpManager by lazy { HttpManager.getInstance() }

    //=======================================方法======================================//
    /**
     * 外界将接口实现通过此方法注册到TaskHandler中，是所有下载任务共同的处理功能
     * 以此实现在下载完成后，外界自动处理下载文件，比如重命名文件并移动到某一个特定目录
     * 但如果任务本身有注册处理接口，则这里注册给下载任务管理器的将不起作用
     */
    fun setDownloadFinishHandle(downloadFinishHandler: ATaskHandler.IBackHandler) {
        taskHandler.registerHandle(downloadFinishHandler)
    }

    /**
     * 将视图监听器注册到下载任务管理，以接收任务信息更新
     */
    fun setViewObserver(viewSources: IViewSources) {
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
        var downloadThreadNum = defaultDownloadThreadNum//下载时使用的线程数默认期待值，但受到实际情况的限制，在不支持多线程下载的情况下，不一定会使用此值
        var userAgent = defaultUserAgent
        var minSizeUseMultiThread: Int = 10485760 //达到10Mb时可以使用多线程下载
        var maxDownloadThreadNum: Int = 64 //一个下载任务的最大下载线程（分块）数量
        var autoRunStopTask = false//true：某个下载任务完成后，是否自动运行处于暂停状态的任务
    }

}

/**
 * 不应在得到Context单例之前调用，在得到单例之前调用，理应崩溃
 */
fun getContext(): Context {
    return Context.singleton!!
}