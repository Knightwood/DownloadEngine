package com.kiylx.download_module.utils.threadpool;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建者 kiylx
 * 创建时间 2020/8/19 18:29
 * packageName：com.example.kiylx.ti.tool
 * 描述：通用线程池
 */
public class ThreadPoolUtil {
    private volatile static ThreadPoolUtil util;
    private final HashMap<String, ExecutorService> kindsOfThreadPool = new HashMap<>();// 存储自定义的线程池
    private final HashMap<String, ExecutorService> predefinedThreadPool = new HashMap<>();// java中预定义好的线程池
    private static final String Single = "SingleThreadExecutor";
    private static final String Fixed = "FixedThreadPool";
    private static final String Cached = "CachedThreadPool";
    private static final String Scheduled = "ScheduledThreadPool";

    public static ThreadPoolUtil getInstance() {
        if (util == null) {
            synchronized (ThreadPoolUtil.class) {
                if (util == null) {
                    util = new ThreadPoolUtil();
                }
            }
        }
        return util;
    }

    /**
     * 根据名称返回线程池，若是未查找到，返回null
     *
     * @param <T>
     * @param threadPoolName
     * @return
     */
    public synchronized <T extends BlockingQueue<Runnable>> ExecutorService getThreadPool(String threadPoolName) {
        if (threadPoolName == null) {
            return null;
        }
        if (kindsOfThreadPool.containsKey(threadPoolName)) {
            return kindsOfThreadPool.get(threadPoolName);
        } else {
            return null;
        }

    }

    /**
     *
     * @param <T>             继承自workQueue
     * @param corePoolSize    核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime   超出核心线程数的线程存活时间。单位：MILLISECONDS
     * @param workQueue       workQueue
     * @return 根据条件生成name，然后查找hashmap中有没有特定的线程池；若是有，直接返回，没有则创建并返回；
     */
    public synchronized <T extends BlockingQueue<Runnable>> ExecutorService newThreadPool(int corePoolSize,
                                                                                          int maximumPoolSize, long keepAliveTime, T workQueue) {
        String name = null;
        name = getThreadPoolName(corePoolSize, maximumPoolSize, keepAliveTime, workQueue.toString());
        if (kindsOfThreadPool.containsKey(name)) {
            return kindsOfThreadPool.get(name);
        } else {
            ExecutorService tmp = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                    TimeUnit.MILLISECONDS, workQueue);
            kindsOfThreadPool.put(name, tmp);
            return tmp;
        }
    }

    /**
     *
     * @return 返回预定义好的线程池
     */
    public synchronized ExecutorService getSingleThreadExecutor() {
        if (!predefinedThreadPool.containsKey(Single)) {
            predefinedThreadPool.put(Single, Executors.newSingleThreadExecutor());
        }
        return predefinedThreadPool.get(Single);
    }

    public synchronized ExecutorService getFixedThreadPool(int nThreads) {
        if (!predefinedThreadPool.containsKey(Fixed)) {
            predefinedThreadPool.put(Fixed,Executors.newFixedThreadPool(nThreads));
        }
        return predefinedThreadPool.get(Fixed);

    }

    public synchronized ExecutorService getCachedThreadPool() {
        if (!predefinedThreadPool.containsKey(Cached)) {
            predefinedThreadPool.put(Cached, Executors.newCachedThreadPool());
        }
        return predefinedThreadPool.get(Cached);

    }

    public synchronized ExecutorService getScheduledThreadPool(int corePoolSize) {
        if (!predefinedThreadPool.containsKey(Scheduled)) {
            predefinedThreadPool.put(Scheduled, Executors.newScheduledThreadPool(corePoolSize));
        }
        return predefinedThreadPool.get(Scheduled);

    }

    /**
     *
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param workQueueName
     * @return 根据参数，返回线程池的名称
     */
    private String getThreadPoolName(int corePoolSize, int maximumPoolSize, long keepAliveTime, String workQueueName) {
        String s = corePoolSize + "/" +
                maximumPoolSize + "/" +
                keepAliveTime + "/" +
                workQueueName;
        return s;
    }
}
