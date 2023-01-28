package com.kiylx.khandler

import kotlin.concurrent.thread

fun main() {
    mainHandler()
}

fun mainHandler() {
    val looper = KLooper()
    val thread = thread {
        looper.loop()
    }
    looper.prepareLooper(thread)
    thread.start()
}