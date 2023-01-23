package com.kiylx.download_module.model
import com.kiylx.download_module.model.TaskLifecycle.*
/**
 * 描述task放在那个队列
 * 与ViewSources相关，需要修改
 */
class MoveStatus {
    companion object{
        /**
         * wait有两个队列，一个是waiting等待下载，一个是frozen暂停状态。
         * 当下载队列有空闲位置，waiting中的会自动开始下载，frozen中的要手动操作执行下载
         * 但是，两者是同一个队列，所以，定义为相同数值
         */
        //waiting/frozen->active
        const val MOVE_TO_ACTIVE = 0

        //ACTIVE->FINISH
        const val MOVE_TO_FINISH = 1

        //active空间不够->waiting
        const val MOVE_TO_WAITING = 2

        //active或waiting->frozen
        const val MOVE_TO_FROZEN = 3

        //队列没有变化
        const val NO_CHANGED = 4

        @JvmStatic
        fun guessMoveState(old: TaskLifecycle, now: TaskLifecycle): Int {
            when (old) {
                OH, CREATE, RESTART, START -> {
                    return when (now) {
                        OH, CREATE, RESTART, START, -> {
                            NO_CHANGED
                        }
                        RUNNING -> {
                            MOVE_TO_ACTIVE
                        }
                        STOP -> {
                            MOVE_TO_WAITING
                        }
                        SUCCESS, CANCEL, FAILED -> {
                            MOVE_TO_FINISH
                        }

                    }
                }
                RUNNING -> {
                    return when (now) {
                        OH, CREATE, RESTART, START, STOP -> {
                            MOVE_TO_WAITING
                        }
                        RUNNING -> {
                            NO_CHANGED
                        }
                        SUCCESS, CANCEL, FAILED -> {
                            MOVE_TO_FINISH
                        }
                    }
                }
                STOP -> {
                    return when (now) {
                        OH, CREATE, RESTART, START, -> {
                            MOVE_TO_WAITING
                        }
                        RUNNING -> {
                            MOVE_TO_ACTIVE
                        }
                        STOP -> {
                            NO_CHANGED
                        }
                        SUCCESS, CANCEL, FAILED -> {
                            MOVE_TO_FINISH
                        }

                    }
                }
                SUCCESS, CANCEL, FAILED -> {
                    return when (now) {
                        OH, CREATE, RESTART, START, STOP -> {
                            MOVE_TO_WAITING
                        }
                        RUNNING -> {
                            MOVE_TO_ACTIVE
                        }
                        SUCCESS, CANCEL, FAILED -> {
                            NO_CHANGED
                        }
                    }
                }
                else -> {
                    return NO_CHANGED
                }
            }
        }
    }
}