package com.kiylx.download_module.viewTest;

import com.kiylx.download_module.lib_core.engine.DownloadTaskImpl;
import com.kiylx.download_module.lib_core.interfaces.DownloadTask;
import com.kiylx.download_module.lib_core.model.DownloadInfo;

public class Main {
    public static void main(String... args) throws Exception {
      //test1();
      //test2();
    }
    public static void test1(){
        TestThread testThread = new TestThread();
        DownloadInfo info= testThread.getDownloadInfo(testThread.url1, testThread.path1, "原神");
        DownloadTask task=DownloadTaskImpl.instance(info);
        try {
            task.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void test2(){
        TestThread testThread = new TestThread();
        testThread.start();
        try {
            testThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
