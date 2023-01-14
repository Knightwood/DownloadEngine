package com.kiylx.download_module.utils.log

import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil
import java.util.logging.Level
import java.util.logging.Logger

fun getLogger(): Logger = JavaLogUtil.setLoggerHandler(Level.INFO)

fun Any.i(logger: Logger, msg: String?, vararg args: Any?) {
    val methodName = Thread.currentThread().stackTrace[1].methodName
    val m = "$methodName:-:$msg"
    logger.info(String.format(m, *args))
}

fun Logger.w(msg: String?, vararg args: Any?) {
    warning(String.format(msg!!, *args))
}

fun Logger.e(msg: String?, vararg args: Any?) {
    severe(String.format(msg!!, *args))
}

fun getStackTraceString(t: Throwable): String? {
    return getThrowableStackMsg(t)
}

fun getStackTraceString(e: Exception): String? {
    return getExceptionStackMsg(e)
}

private fun getExceptionStackMsg(e: Exception): String? {
    val sb = StringBuffer()
    val stackArray = e.stackTrace
    for (element in stackArray) {
        sb.append(element.toString()).append("\n")
    }
    return sb.toString()
}

private fun getThrowableStackMsg(e: Throwable): String? {
    val sb = StringBuffer()
    val stackArray = e.stackTrace
    for (element in stackArray) {
        sb.append(element.toString()).append("\n")
    }
    return sb.toString()
}