package com.kiylx.download_module.utils

import com.kiylx.download_module.Context
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 1.如果替代类(substituteClazz)不存在，使用defaultCreator创建实例并返回
 *
 * 2.如果替代类(substituteClazz)存在，使用替代类(substituteClazz)创建实例并返回
 *
 * 要求类必须有空参数的构造函数
 * @param substituteClazz 如果不为null,则使用此类取代默认类
 * @param defaultCreator 创建默认类
 * T : 替代类和默认类分别是T的两个子类
 */
//<out T, in A>(creator: (A) -> T)
class CreateClassInstance<T>(substituteClazz: Class<out T>?, defaultCreator: () -> T) : ReadOnlyProperty<Context, T> {
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

//方法版，作用同上
fun <T> createClassInstance(substituteClazz: Class<out T>?, defaultCreator: () -> T): T {
    return if (substituteClazz == null) {
        val tmp = defaultCreator()
        tmp
    } else {
        val tmp2 = substituteClazz.getDeclaredConstructor().newInstance()
        tmp2 as T
    }
}

//可赋值版
class CreateClassInstance2<T>(substituteClazz: Class<out T>?, defaultCreator: () -> T) : ReadWriteProperty<Context, T> {
    private var creator: (() -> T) = defaultCreator//生成默认类实例
    private var clazz = substituteClazz//取代默认类的新类
    private var instance: T? = null//创建的类实例

    override fun getValue(thisRef: Context, property: KProperty<*>): T {
        val i = instance
        if (i != null) {
            return i
        }
        if (clazz == null) {
            val tmp = creator()
            instance = tmp
            return tmp
        } else {
            val tmp2 = clazz!!.getDeclaredConstructor().newInstance()
            instance = tmp2
            return tmp2 as T
        }
    }

    override fun setValue(thisRef: Context, property: KProperty<*>, value: T) {
        instance = value
    }
}