package com.kiylx.download_module.view

import com.kiylx.download_module.Context
import com.kiylx.download_module.getContext
import com.kiylx.download_module.lib_core.engine.TaskHandler
import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener
import okhttp3.internal.wait
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask

class ViewsCenter {
    private var time = Context.updateViewInterval;//以多久的间隔更新视图
    private var connected: Boolean = false
    private val downloadResultListeners = ConcurrentLinkedQueue<DownloadResultListener>()
    private var tasks: WeakReference<TaskHandler>? = null
    private val taskNotify: TaskNotify = TaskNotify()
    private val timer = Timer()
    private val bool = AtomicBoolean(false)

    inner class TaskNotify : TimerTask() {
        override fun run() {
            if (!downloadResultListeners.isEmpty()) {
                downloadResultListeners.forEach { listener ->
                    //listener.updated()
                }
            }

        }
    }

    fun activeNotify() {
        if (bool.compareAndSet(false, true)) {
            timer.schedule(taskNotify, 0L, time)
        }
    }

    fun stopNotify() {
        if (bool.compareAndSet(true, false)) {
            timer.cancel()
        }
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

    //获得taskhandler
    private fun connectTaskHandler() {
        tasks = WeakReference(getContext().taskHandler)
    }

    //清理
    private fun clear() {
        tasks?.clear()
        downloadResultListeners.clear()
    }

    private interface IMethod {
        operator fun invoke(listener: DownloadResultListener?)
    }

    private fun notifyListeners(l: IMethod, listeners: ConcurrentLinkedQueue<DownloadResultListener>) {
        for (tmp in listeners) {
            if (tmp != null) l.invoke(tmp)
        }
    }
}