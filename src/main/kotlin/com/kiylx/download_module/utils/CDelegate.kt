package com.kiylx.download_module.utils

import com.kiylx.download_module.Context
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 如果实例存在，返回实例。
 * 如果替代类不存在，使用默认类创建实例并返回
 * 如果替代类存在，使用替代类创建实例并返回
 * 要求类必须有空参数的构造函数
 * @param substituteClazz 替代默认类的类
 * @param defaultCreator 默认类
 * T : 替代类和默认类分别是T的两个子类
 */
//<out T, in A>(creator: (A) -> T)
class CDelegate<T>(substituteClazz: Class<out T>?, defaultCreator: () -> T) : ReadOnlyProperty<Context, T> {
    private var creator2: (() -> T)? = defaultCreator
    private var clazz = substituteClazz
    private var instance: T? = null

    override fun getValue(thisRef: Context, property: KProperty<*>): T {
        val i = instance
        if (i != null) {
            return i
        }
        return if (clazz == null) {
            val tmp = creator2!!()
            creator2 = null
            instance = tmp
            tmp
        } else {
            val tmp2 = clazz!!.getDeclaredConstructor().newInstance()
            instance = tmp2
            clazz = null
            tmp2 as T
        }
    }

}