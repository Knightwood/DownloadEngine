package com.kiylx.download_module;

public class TestThread extends Thread {
    String url = "";
    String path = "";
    String name = "";

    public TestThread(String url, String path, String name) {
        this.url = url;
        this.path = path;
        this.name = name;
    }

    @Override
    public void run() {
        Context.ContextConfigs cont = new Context.ContextConfigs();
        cont.setLimit(2);
        cont.setThreadNum(3);
        Downloads downloads = Downloads.Companion.downloadsInstance(cont);
        downloads.execDownloadTask(url, path, name, 3);
        try {
            sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
