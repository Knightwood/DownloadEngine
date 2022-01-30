package com.kiylx.download_module.view

import com.kiylx.download_module.Context
import com.kiylx.download_module.getContext
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ViewsCenter {
    private var time = Context.updateViewInterval;//以多久的间隔更新视图
    private var connected: Boolean = false
    private val downloadResultListeners = ConcurrentLinkedQueue<DownloadResultListener>()
    private var tasks: WeakReference<TaskHandler>? = null
    private var infosList: MutableList<SimpleDownloadInfo>? = null
    private var b: AtomicBoolean = AtomicBoolean(false)

    init {
        tasks = WeakReference(getContext().taskHandler)
        infosList = tasks!!.get()?.allSimpleDownloadsInfo;
    }

    // SimpleDownloadInfo本身会在下载过程中被更新，所以这里只需要不停的用rxjava周期性的推送即可
    private val observable: Observable<MutableList<SimpleDownloadInfo>?> =
        Observable.just(infosList).repeatWhen {
            Observable.timer(2, TimeUnit.SECONDS)
        }

    private fun doSomeThing() {
        observable.subscribe(object : Observer<MutableList<SimpleDownloadInfo>?> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: MutableList<SimpleDownloadInfo>) {
                downloadResultListeners.forEach {
                    it.updated(t)
                }
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

    fun listen() {
        if (b.compareAndSet(false, true))
            doSomeThing()
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