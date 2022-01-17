package com.kiylx.download_module.utils.kotlin.singleton

/**
使用：
class SomeSingleton private constructor(context: Context) {
init {
// Init using context argument
context.getString(R.string.app_name)
}

companion object : SingletonHolder<SomeSingleton, Context>(::SomeSingleton)
}
获取单例
SomeSingleton.getInstance(context)
 */

open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
    //对上述方法的一种更简洁的写法
    fun getInstance2(arg: A): T =
        instance ?: synchronized(this) {
            instance ?: creator!!(arg).apply {
                instance = this
            }
        }
}
