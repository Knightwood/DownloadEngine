package com.kiylx.download_module.utils.java_log_pack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.*;

/**
 * 默认将log文件输出到$LOG_FILE_PATH\AIOClient\xxxx年\x月\xxxx-xx-xx.log
 * 路径不存在的话会自动创建
 * 可通过修改getLogFilePath修改生成的log路径
 *
 * Created by pos on 2016/8/30.
 */
public class JavaLogUtil {
    private static final String LOG_FILE_PATH="C:\\Logs";
    private static Calendar now = Calendar.getInstance();

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static final int year = now.get(Calendar.YEAR);

    private static final int month = now.get(Calendar.MONTH) + 1;

    private static final String LOG_FOLDER_NAME = "LOG_FOLDER";

    private static final String LOG_FILE_SUFFIX = ".log";

    private static Logger logger = Logger.getLogger("MyLogger");

    //使用唯一的fileHandler，保证当天的所有日志写在同一个文件里
    private static FileHandler fileHandler = getFileHandler();

    private static MyLogFormatter myLogFormatter = new MyLogFormatter();

    private synchronized static String getLogFilePath() {
        StringBuilder logFilePath = new StringBuilder();
        logFilePath.append(System.getProperty("user.home"));
        //logFilePath.append(LOG_FILE_PATH);
        logFilePath.append(File.separatorChar);
        logFilePath.append(LOG_FOLDER_NAME);
        logFilePath.append(File.separatorChar);
        logFilePath.append(year);
        logFilePath.append(File.separatorChar);
        logFilePath.append(month);

        File dir = new File(logFilePath.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        logFilePath.append(File.separatorChar);
        logFilePath.append(sdf.format(new Date()));
        logFilePath.append(LOG_FILE_SUFFIX);

//        System.out.println(logFilePath.toString());
        return logFilePath.toString();
    }

    private static FileHandler getFileHandler() {
        FileHandler fileHandler = null;
        //boolean APPEND_MODE = true;
        try {
            //文件日志内容标记为可追加
            fileHandler = new FileHandler(getLogFilePath(), true);
            return fileHandler;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized static Logger setLoggerHandler() {
        return setLoggerHandler(Level.ALL);
    }

    //    SEVERE > WARNING > INFO > CONFIG > FINE > FINER > FINEST
    public synchronized static Logger setLoggerHandler(Level level) {

        try {
            //以文本的形式输出
//            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setFormatter(myLogFormatter);

            logger.addHandler(fileHandler);
            logger.setLevel(level);
        } catch (SecurityException e) {
            logger.severe(populateExceptionStackTrace(e));
        }
        return logger;
    }

    private synchronized static String populateExceptionStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement elem : e.getStackTrace()) {
            sb.append("\tat ").append(elem).append("\n");
        }
        return sb.toString();
    }



    public static void main(String [] args) {
        Logger logger = JavaLogUtil.setLoggerHandler(Level.INFO);
        logger.info("Hello, world!");
        logger.severe("What are you doing?");
        logger.warning("Warning !");
//
//        for(Handler h : logger.getHandlers()) {
//            h.close();   //must call h.close or a .LCK file will remain.
//        }
    }

}
