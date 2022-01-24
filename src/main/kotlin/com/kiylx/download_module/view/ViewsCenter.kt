package com.kiylx.download_module.view

import com.kiylx.download_module.Context
import com.kiylx.download_module.getContext
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener
import com.kiylx.download_module.lib_core.interfaces.PieceThread
import io.reactivex.Observable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class ViewsCenter {
    private var time = Context.updateViewInterval;//以多久的间隔更新视图
    private var connected: Boolean = false
    private val downloadResultListeners = ConcurrentLinkedQueue<DownloadResultListener>()
    private var tasks: WeakReference<TaskHandler>? = null
    private var infosList: MutableList<SimpleDownloadInfo>? = null

    init {
        tasks = WeakReference(getContext().taskHandler)
        infosList = tasks!!.get()?.allSimpleDownloadsInfo;
    }

    //todo SimpleDownloadInfo本身会在下载过程中被更新，所以这里只需要不停的用rxjava周期性的推送即可
    val observable: Observable<MutableList<SimpleDownloadInfo>?> =
        Observable.just(infosList).repeatWhen {
            Observable.timer(2, TimeUnit.SECONDS)
        }

    fun addListener(listener: DownloadResultListener) {
        downloadResultListeners.add(listener)

    }

    fun removeListener(listener: DownloadResultListener) {
        downloadResultListeners.remove(listener)
    }

    fun isConnect(): Boolean = connected

    fun connect() {
        if (connected)
            return
        connected = true
    }

    fun disConnect() {
        clear()
        connected = false
    }

    //清理
    private fun clear() {
        tasks?.clear()
        downloadResultListeners.clear()
    }

}