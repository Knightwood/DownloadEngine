package com.kiylx.download_module.utils.java_log_pack;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * 自定义日志格式
 *
 * Created by pos on 2016/8/31.
 */
class MyLogFormatter extends Formatter {

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(LocalDateTime.now()).append(" - ");
        //下面两行打印调用日志打印的类名和方法名
        builder.append("[").append(record.getSourceClassName()).append(".");
        builder.append(record.getSourceMethodName()).append("] - \n");
        builder.append("\t[").append(record.getLevel()).append("] - ");
        builder.append(formatMessage(record));
        builder.append("\n\n");
        return builder.toString();
    }

    @Override
    public String formatMessage(LogRecord record) {
        return super.formatMessage(record);
    }

    public String getHead(Handler h) {
        return super.getHead(h);
    }

    public String getTail(Handler h) {
        return super.getTail(h);
    }
}
