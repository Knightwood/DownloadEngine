package com.kiylx.download_module.view

import com.kiylx.download_module.Context
import com.kiylx.download_module.getContext
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.internal.disposables.DisposableContainer
import io.reactivex.observers.DisposableCompletableObserver
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ViewsCenter {

    private val downloadResultListeners = ConcurrentLinkedQueue<DownloadResultListener>()
    private var tasks: WeakReference<TaskHandler>? = null
    private var infosList_wait: List<SimpleDownloadInfo>? = null
    private var infosList_active: List<SimpleDownloadInfo>? = null
    private var infosList_finish: List<SimpleDownloadInfo>? = null
    private var b: AtomicBoolean = AtomicBoolean(false)

    init {
        tasks = WeakReference(getContext().taskHandler)
        infosList_active = tasks?.get()?.getActiveList();
        infosList_wait = tasks?.get()?.getWaitingList();
        infosList_finish = tasks?.get()?.getFinishList();
    }

    // SimpleDownloadInfo本身会在下载过程中被更新，所以这里只需要不停的用rxjava周期性的推送即可
    private val activeObservable: Observable<List<SimpleDownloadInfo>?> =
        Observable.just(infosList_active).repeatWhen {
            Observable.timer(2, TimeUnit.SECONDS)
        }
    private val waitObservable: Observable<List<SimpleDownloadInfo>?> =
        Observable.just(infosList_wait).repeatWhen {
            Observable.timer(2, TimeUnit.SECONDS)
        }
    private val finishObservable: Observable<List<SimpleDownloadInfo>?> =
        Observable.just(infosList_finish).repeatWhen {
            Observable.timer(2, TimeUnit.SECONDS)
        }
    var disposableContainer = CompositeDisposable()

    private fun doSomeThing() {
        val activeDisposable = activeObservable.subscribe(Consumer { list ->
            downloadResultListeners.forEach {
                it.updatedActive(list)
            }
        })
        val waitDisposable = waitObservable.subscribe(Consumer { list ->
            downloadResultListeners.forEach {
                it.updatedWait(list)
            }
        })
        val finishDisposable = finishObservable.subscribe(Consumer { list ->
            downloadResultListeners.forEach {
                it.updatedFinish(list)
            }
        })
        disposableContainer.addAll(activeDisposable, waitDisposable, finishDisposable)
    }

    fun listen() {
        if (b.compareAndSet(false, true))
            doSomeThing()
    }

    fun unListen() {
        if (b.compareAndSet(false, true))
            disposableContainer.dispose()
    }

    fun addListener(listener: DownloadResultListener) {
        downloadResultListeners.add(listener)
    }

    fun removeListener(listener: DownloadResultListener) {
        downloadResultListeners.remove(listener)
    }


    //清理
    private fun clear() {
        tasks?.clear()
        downloadResultListeners.clear()
    }

}