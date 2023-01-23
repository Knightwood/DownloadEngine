package com.kiylx.download_module.utils

import com.kiylx.download_module.utils.TextUtils.convertSpeed

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

    /**
     * 传入bytes,返回人性化的字符串
     */
    @JvmStatic
    fun convertSpeed(num: Long): String {
        if (num/1024 <1) {
            return "$num b"
        }
        val kb = num / 1024.0
        return if (kb.toInt() in 1..1023) {
            "${String.format("%.2f",kb)} Kb"
        } else {
            val mb = kb / 1024.0
            if (mb.toInt() in 1..1023) {
                "${String.format("%.2f",mb)} Mb"
            } else {
                val gb = mb / 1024.0
                "${String.format("%.2f",gb)} g"
            }
        }
    }

    /**
     * @param num bytes
     * @param timeInterval 时间间隔 时间单位：秒
     *
     * bytes除以timeInterval并进行转换，得到易于阅读的速度字符串
     */
    @JvmStatic
    fun convertBytesNumber(num: Long,timeInterval:Int=1):String {
        if (num/1024 <1) {
            return "$num/$timeInterval b/s"
        }
        val kb = num / 1024.0
        return if (kb.toInt() in 1..1023) {
            "${String.format("%.2f",kb/timeInterval)} Kb/s"
        } else {
            val mb = kb / 1024.0
            if (mb.toInt() in 1..1023) {
                "${String.format("%.2f",mb/timeInterval)} Mb/s"
            } else {
                val gb = mb / 1024.0
                "${String.format("%.2f",gb/timeInterval)} g/s"
            }
        }
    }

}
fun main() {
    print(convertSpeed(100) + "\n")
    print(convertSpeed(1024) + "\n")
    print(convertSpeed(10240) + "\n")
    print(convertSpeed(204800) + "\n")
    print(convertSpeed(2048000) + "\n")
}