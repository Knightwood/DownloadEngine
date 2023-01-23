package com.kiylx.download_module.model


/**
 * CREATE->START->DOWNLOADING->STOP->
 * RESTART->START->DOWNLOADING...->COMPLETE
 * 这里没有下载完成的状态，下载完成时，是RUNNING
 * finish或者说complete（任务结束）理应包括SUCCESS和FAILED两种状态
 */
enum class TaskLifecycle(  //任务执行过程中出错，无法成功完成任务
    private val code: Int
) {
    OH(0),  //无状态
    CREATE(1),  //创建任务
    RESTART(2),  //恢复下载 ->START
    START(3),  //等待
    RUNNING(4),  //运行中
    SUCCESS(5),  //task运行结束
    STOP(6),  //终止任务
    CANCEL(7),  //task未完成，被终止丢弃（取消下载操作）
    FAILED(8);



    companion object {
        /**
         * @param i -1 < i < 7
         * @return state
         */
        fun parse(i: Int): TaskLifecycle {
            return values()[i]
        }

        fun dec(lifecycle: TaskLifecycle): Int {
            return lifecycle.code
        }
    }
}
