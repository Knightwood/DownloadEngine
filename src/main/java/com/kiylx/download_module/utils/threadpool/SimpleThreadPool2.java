package com.kiylx.download_module.utils.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleThreadPool2 {
    private static final String TAG = "TaskPool";
    private static volatile SimpleThreadPool2 sSimpleThreadPool2;
    //线程池
    private ExecutorService mExecutorService;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int THREAD_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    //核心线程数
    private static final int CORE_POOL_SIZE = THREAD_SIZE;

    public static SimpleThreadPool2 getInstance() {
        if (sSimpleThreadPool2 == null) {
            synchronized (SimpleThreadPool2.class) {
                if (sSimpleThreadPool2 == null) {
                    sSimpleThreadPool2 = new SimpleThreadPool2();
                }
            }
        }
        return sSimpleThreadPool2;
    }

    public synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    Integer.MAX_VALUE,
                    60,
                    TimeUnit.MILLISECONDS,
                    new SynchronousQueue<Runnable>());
        }
        return mExecutorService;
    }
    public void executeTask(DownloadTaskRunnable runnable){
        mExecutorService.execute(runnable);
    }

}
