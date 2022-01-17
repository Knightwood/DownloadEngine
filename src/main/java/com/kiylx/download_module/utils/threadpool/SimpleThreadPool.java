package com.kiylx.download_module.utils.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

/**
 * 创建者 kiylx
 * 创建时间 2020/8/20 21:23
 * packageName：com.example.kiylx.ti.tool
 * 描述：
 */
public class SimpleThreadPool {
    //线程池
    private static volatile SimpleThreadPool sTaskPool;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int THREAD_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    //核心线程数
    private static final int CORE_POOL_SIZE = THREAD_SIZE;
    //线程池
    private ExecutorService mExecutorService;

    public static SimpleThreadPool getInstance() {
        if (sTaskPool == null) {
            synchronized (SimpleThreadPool.class) {
                if (sTaskPool == null) {
                    sTaskPool = new SimpleThreadPool();
                }
            }
        }
        return sTaskPool;
    }

    /**
     * 构造函数，创建一个线程池
     */
    private SimpleThreadPool(){
        if (mExecutorService==null){
            mExecutorService=ThreadPoolUtil.getInstance().newThreadPool(CORE_POOL_SIZE,
                    Integer.MAX_VALUE,
                    500,
                    new SynchronousQueue<>());
        }
    }

    public ExecutorService getExecutorService() {
        return mExecutorService;
    }
}
