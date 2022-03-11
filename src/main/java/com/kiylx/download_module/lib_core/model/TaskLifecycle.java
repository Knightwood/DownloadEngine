package com.kiylx.download_module.lib_core.model;

/**
 * CREATE->START->DOWNLOADING->STOP
 * RESTART->START->DOWNLOADING...
 * 这里没有下载完成的状态，下载完成时，是RUNNING
 */
public enum TaskLifecycle {
     OH(0),//无状态
     CREATE(1), //创建任务
    RESTART(2),//恢复下载 ->START
    START(3), //等待
    RUNNING(4), //运行中
   // PAUSE(3), //暂停任务，等待下载时机恢复或手动恢复
    STOP(5),//停止 (取消下载需要停止）
    DROP(6);//被丢弃

    private final int code;

    TaskLifecycle(int i) {
        this.code=i;
    }

    /**
     * @param i -1 < i < 7
     * @return state
     */
    public static TaskLifecycle parse(int i) {
        return values()[i];
    }
    public static int dec(TaskLifecycle lifecycle){
        return lifecycle.code;
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

