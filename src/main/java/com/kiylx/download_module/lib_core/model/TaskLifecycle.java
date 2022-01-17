package com.kiylx.download_module.lib_core.model;

/**
 * CREATE->START->DOWNLOADING->STOP
 * RESTART->START->DOWNLOADING...
 */
public enum TaskLifecycle {
    CREATE(0), //创建任务
    START(1), //等待
    RUNNING(2), //运行中
   // PAUSE(3), //暂停任务，等待下载时机恢复或手动恢复
    RESTART(4),//恢复下载 ->START
    STOP(5),//停止 (取消下载需要停止）
    DROP(6),//被丢弃
    OH(7);//无状态

    TaskLifecycle(int i) {
    }

    /**
     * @param i -1 < i < 8
     * @return state
     */
    public static TaskLifecycle parse(int i) {
        return values()[i];
    }

    /**
     * 描述task放在那个队列
     */
    public static class MoveStatus{
        //wait->active
        public static final int MOVE_TO_ACTIVE=0;
        //ACTIVE->FINISH
        public static final int MOVE_TO_FINISH=1;
        //ACTIVE->WAIT
        public static final int MOVE_TO_WAIT=2;

        public static final int MOVE_TO_FROZEN=3;

        public int code(TaskLifecycle old, TaskLifecycle now){
            //if (old==)
            return 0;
        }
    }
}

