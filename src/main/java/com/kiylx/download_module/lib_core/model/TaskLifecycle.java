package com.kiylx.download_module.lib_core.model;

/**
 * CREATE->START->DOWNLOADING->STOP->
 * RESTART->START->DOWNLOADING...->COMPLETE
 * 这里没有下载完成的状态，下载完成时，是RUNNING
 * finish或者说complete（任务结束）理应包括SUCCESS和FAILED两种状态
 */
public enum TaskLifecycle {
    OH(0),//无状态
    CREATE(1), //创建任务
    RESTART(2),//恢复下载 ->START
    START(3), //等待
    RUNNING(4), //运行中
    SUCCESS(5),//task运行结束
    STOP(6),//终止任务
    CANCEL(7),//task未完成，被终止丢弃（取消下载操作）
    FAILED(8);//任务执行过程中出错，无法成功完成任务

    private final int code;

    TaskLifecycle(int i) {
        this.code = i;
    }

    /**
     * @param i -1 < i < 7
     * @return state
     */
    public static TaskLifecycle parse(int i) {
        return values()[i];
    }

    public static int dec(TaskLifecycle lifecycle) {
        return lifecycle.code;
    }

    /**
     * 描述task放在那个队列
     */
    public static class MoveStatus {
        /**
         * wait有两个队列，一个是waiting等待下载，一个是frozen暂停状态。
         * 当下载队列有空闲位置，waiting中的会自动开始下载，frozen中的要手动操作执行下载
         * 但是，两者是同一个队列，所以，定义为相同数值
         */
        //waiting/frozen->active
        public static final int MOVE_TO_ACTIVE = 0;
        //ACTIVE->FINISH
        public static final int MOVE_TO_FINISH = 1;
        //active空间不够->waiting
        public static final int MOVE_TO_WAITING = 2;
        //active或waiting->frozen
        public static final int MOVE_TO_FROZEN = 3;
        //队列没有变化
        public static final int NO_CHANGED = 4;

        public static int guessMoveState(TaskLifecycle old, TaskLifecycle now) {
            switch (old) {
                case OH,CREATE,RESTART,START : {
                    switch (now) {
                        case OH, CREATE, RESTART, START : {
                            return NO_CHANGED;
                        }
                        case RUNNING : {
                            return MOVE_TO_ACTIVE;
                        }
                        case STOP : {
                            return MOVE_TO_WAITING;
                        }
                        case SUCCESS, CANCEL, FAILED : {
                            return MOVE_TO_FINISH;
                        }
                    }
                }
                case RUNNING : {
                    switch (now) {
                        case OH, CREATE, RESTART, START, STOP : {
                            return MOVE_TO_WAITING;
                        }
                        case RUNNING : {
                            return NO_CHANGED;
                        }
                        case SUCCESS, CANCEL, FAILED : {
                            return MOVE_TO_FINISH;
                        }
                    }
                }
                case STOP : {
                    switch (now) {
                        case OH, CREATE, RESTART, START : {
                            return MOVE_TO_WAITING;
                        }
                        case RUNNING : {
                            return MOVE_TO_ACTIVE;
                        }
                        case STOP : {
                            return NO_CHANGED;
                        }
                        case SUCCESS, CANCEL, FAILED : {
                            return MOVE_TO_FINISH;
                        }
                    }
                }
                case SUCCESS, CANCEL, FAILED : {
                    switch (now) {
                        case OH, CREATE, RESTART, START, STOP : {
                            return MOVE_TO_WAITING;
                        }
                        case RUNNING : {
                            return MOVE_TO_ACTIVE;
                        }
                        case SUCCESS, CANCEL, FAILED : {
                            return NO_CHANGED;
                        }
                    }
                }
            }
            return NO_CHANGED;
        }
    }
}

