package com.kiylx.download_module.utils.threadpool;

import com.kiylx.download_module.model.DownloadInfo;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable {
    private final BlockingQueue<DownloadInfo> active;
    private final Queue<DownloadInfo> wait;

    public Producer(BlockingQueue<DownloadInfo> active, Queue<DownloadInfo> wait) {
        this.active = active;
        this.wait = wait;
    }

    @Override
    public void run() {
        active.add(wait.peek());
    }
}
