package com.kiylx.khandler

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class KLooper {
    private val msgQueue: KMessageQueue = KMessageQueue()
    private var sThread: Thread? = null
    private val lock: ReentrantLock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    fun prepareLooper(thread: Thread) {
        if (sThread != null) {
            throw RuntimeException(
                "Can't create handler inside thread " + Thread.currentThread()
                        + " 线程已经绑定过looper"
            )
        } else {
            sThread = thread
        }
    }

    fun loop() {
        try {
            lock.lock()
            while (true) {
                if (msgQueue.hasNext()) {
                    val msg = msgQueue.next()
                    // 处理消息
                } else {
                    condition.await()
                }
            }
        } finally {
            lock.unlock()
        }
    }

}