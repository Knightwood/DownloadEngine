package com.kiylx.download_module.view

import com.kiylx.download_module.getContext
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 周期性的获取所有的simpledownloadinfo
 */
@Deprecated("outDated")
class ViewsCenter {

    private val downloadResultListeners = ConcurrentLinkedQueue<DownloadResultListener>()
    private var tasks: WeakReference<TaskHandler>? = null
    private var infosList_wait: MutableList<SimpleDownloadInfo>? = null
    private var infosList_active: MutableList<SimpleDownloadInfo>? = null
    private var infosList_finish: MutableList<SimpleDownloadInfo>? = null
    private var b: AtomicBoolean = AtomicBoolean(false)

    init {
        tasks = WeakReference(getContext().taskHandler)
        infosList_active = tasks?.get()?.getActiveList();
        infosList_wait = tasks?.get()?.getWaitingList();
        infosList_finish = tasks?.get()?.getFinishList();
    }

    // SimpleDownloadInfo本身会在下载过程中被更新，所以这里只需要不停的用rxjava周期性的推送即可
    private val activeObservable: Observable<MutableList<SimpleDownloadInfo>?> =
        Observable.just(infosList_active).repeatWhen {
            Observable.timer(1, TimeUnit.SECONDS)
        }
    private val waitObservable: Observable<MutableList<SimpleDownloadInfo>?> =
        Observable.just(infosList_wait).repeatWhen {
            Observable.timer(1, TimeUnit.SECONDS)
        }
    private val finishObservable: Observable<MutableList<SimpleDownloadInfo>?> =
        Observable.just(infosList_finish).repeatWhen {
            Observable.timer(3, TimeUnit.SECONDS)
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

    //因为有些下载被删除或是下载完成，他们会被移动到另一个列表，所以要重新获取列表更新进度
    fun updateList() {
        infosList_active?.apply {
            clear()
            tasks?.get()?.getActiveList(ViewsAction.update)?.let { addAll(it) }
        }
        infosList_wait?.apply {
            clear()
            tasks?.get()?.getWaitingList(ViewsAction.update)?.let { addAll(it) }
        }
        infosList_finish?.apply {
            clear()
            tasks?.get()?.getFinishList(ViewsAction.update)?.let { addAll(it) }
        }
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