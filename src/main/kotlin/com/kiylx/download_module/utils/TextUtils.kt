package com.kiylx.download_module.utils

object TextUtils {
    // 1 d. 1 h. 1 m
    const val elapsed_time_format_d_h_mm = "%1\$d d. %2\$d h. %3\$d m"

    //1h . 1m . 1s
    const val elapsed_time_format_h_mm_ss = "%1\$d h. %2\$d m. %3\$d s"

    //1m . 1s
    const val elapsed_time_format_mm_ss = "%1\$d m. %2\$d s"

    //1s
    const val elapsed_time_format_ss = "%1\$d s"

    @JvmStatic
    fun isEmpty(s: String?): Boolean {
        return s.isNullOrEmpty() || s.isBlank()
    }
}