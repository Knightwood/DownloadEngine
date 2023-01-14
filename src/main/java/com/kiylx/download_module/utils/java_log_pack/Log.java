package com.kiylx.download_module.utils.java_log_pack;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 此类，使得在日志打印中无法正确打印调用日志打印的类名和方法名
 * 因此弃用。
 * 但若无打印类名与方法名的需要，可以使用此类
 */
@Deprecated
public class Log {
    static Logger logger = JavaLogUtil.setLoggerHandler(Level.INFO);

    public static void i(String msg, Object... args) {
        logger.info(String.format(msg, args));
    }

    public static void w(String msg, Object... args) {
        logger.warning(String.format(msg, args));
    }

    public static void e(String msg, Object... args) {
        logger.severe(String.format(msg, args));
    }

    public static String getStackTraceString(Throwable t) {
        return getThrowableStackMsg(t);
    }

    public static String getStackTraceString(Exception e) {
        return getExceptionStackMsg(e);
    }

    private static String getExceptionStackMsg(Exception e) {

        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackArray = e.getStackTrace();
        for (StackTraceElement element : stackArray) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private static String getThrowableStackMsg(Throwable e) {

        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackArray = e.getStackTrace();
        for (StackTraceElement element : stackArray) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
