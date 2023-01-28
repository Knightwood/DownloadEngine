package com.kiylx.download_module.utils

/**
 * 类似于apply的扩展函数。
 *
 * block内使用this而不是it，而且没有返回值
 */
@JvmName("kotlin_make")
inline fun <T> T.make(block: T.() -> Unit) {
    block()
}
@JvmName("kotlin_make_f")
inline fun <T> make(t: T, block: T.() -> Unit) {
    t.block()
}

