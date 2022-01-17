package com.kiylx.download_module.utils.java_log_pack;

import java.util.logging.Level;
import java.util.logging.Logger;

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
