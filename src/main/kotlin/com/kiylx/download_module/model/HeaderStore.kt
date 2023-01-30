package com.kiylx.download_module.model

import kotlinx.serialization.Serializable

/**
 * 描述header
 */
@Serializable
data class HeaderStore(@JvmField val name: String, @JvmField var value: String) {
    companion object {
        fun useragent(s: String) = HeaderStore("User-Agent", s)
        fun referer(s: String) = HeaderStore("Referer", s)
        fun range(start: Long, end: Long) = HeaderStore("Range", "bytes=$start-$end")
    }
}

class HeaderName {
    companion object {
        const val UserAgent = "User-Agent"
        const val Referer = "Referer"
        const val Range = "Range"
        const val ETag = "ETag"
        const val AcceptRanges = "Accept-Ranges"

        @JvmStatic
        fun rangeStr(start: Long, end: Long) = "bytes=$start-$end"
    }
}

inline fun DownloadInfo.editCustomHeaders(block: HashMap<String, String>.() -> Unit) {
    this.customHeaders.block()
}

/**
 * 获取排除了指定名称的header
 *
 * @param exclude 从自定义header的hashmap种排除这些名称的header
 */
@JvmOverloads
fun DownloadInfo.filterCustomHeaders(
    vararg exclude: String = arrayOf(
        HeaderName.AcceptRanges,
        HeaderName.Range,
        HeaderName.ETag,
    )
): Map<String, String> {
    val result = this.customHeaders.filterKeys {
        !exclude.contains(it)
    }
    return result
}