package com.kiylx.download_module.utils.threadpool;

import com.kiylx.download_module.model.DownloadInfo;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class Consumer implements Runnable{
    public Consumer(BlockingQueue<DownloadInfo> active, Queue<DownloadInfo> wait) {
    }

    @Override
    public void run() {

    }
}
