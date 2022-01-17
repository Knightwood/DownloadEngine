package com.kiylx.download_module.utils;

public class TextUtils {
    // 1 d. 1 h. 1 m
    public static final String elapsed_time_format_d_h_mm = "%1$d d. %2$d h. %3$d m";
    //1h . 1m . 1s
    public static final String elapsed_time_format_h_mm_ss ="%1$d h. %2$d m. %3$d s";
    //1m . 1s
    public static final String elapsed_time_format_mm_ss="%1$d m. %2$d s";
    //1s
    public static final String elapsed_time_format_ss ="%1$d s";
    public static boolean isEmpty(String s) {
        return (s == null || s.isEmpty());
    }
}
