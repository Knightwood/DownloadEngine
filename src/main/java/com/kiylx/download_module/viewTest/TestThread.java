package com.kiylx.download_module.viewTest;

import com.kiylx.download_module.Context;
import com.kiylx.download_module.Downloads;
import com.kiylx.download_module.lib_core.model.DownloadInfo;
import com.kiylx.download_module.view.ViewsCenter;

import static com.kiylx.download_module.ContextKt.getContext;

public class TestThread extends Thread {
   InitTest initTest;
    public String url1 = "https://autopatchcn.yuanshen.com/client_app/download/launcher/20211221175613_GebZo2g1tjID2onr/mihoyo/yuanshen_setup_20211214161000.exe";
    public String url2 = "https://nite07-my.sharepoint.cn/personal/nite_nite07_partner_onmschina_cn/_layouts/15/download.aspx?UniqueId=b5510019-c457-4658-bb3c-0067f3450962&Translate=false&tempauth=eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTBmZjEtY2UwMC0wMDAwMDAwMDAwMDAvbml0ZTA3LW15LnNoYXJlcG9pbnQuY25AYmEyOGRmYmMtZDYzOC00ZjgzLThhZDktN2VkYmYxNTlmYzE0IiwiaXNzIjoiMDAwMDAwMDMtMDAwMC0wZmYxLWNlMDAtMDAwMDAwMDAwMDAwIiwibmJmIjoiMTY0MzI2MDYzNiIsImV4cCI6IjE2NDMyNjQyMzYiLCJlbmRwb2ludHVybCI6Im9kbTZpQlArYkN2N3ZYdktmQ2h2amdiem1MTlNIcXlUaFNxVTZ5UnB5aG89IiwiZW5kcG9pbnR1cmxMZW5ndGgiOiIxNjEiLCJpc2xvb3BiYWNrIjoiVHJ1ZSIsImNpZCI6Ik9HWXlOalV6T1RrdFltSTVOQzAwTnpVMUxUZzBaR010T0RBMU9HRmxORGRoTkdJeSIsInZlciI6Imhhc2hlZHByb29mdG9rZW4iLCJzaXRlaWQiOiJORFU1T0RJMk1tWXRPRFprTmkwME5tSXlMVGt6WmpVdFpURTFabVF4TVRJd016RTMiLCJhcHBfZGlzcGxheW5hbWUiOiJwYW5pbmRleCIsImFwcGlkIjoiYjE3OTA4YmMtMDY0NC00OTQwLTlmNjQtZjBiMTdjNWQ2ZjQ5IiwidGlkIjoiYmEyOGRmYmMtZDYzOC00ZjgzLThhZDktN2VkYmYxNTlmYzE0IiwidXBuIjoibml0ZUBuaXRlMDcucGFydG5lci5vbm1zY2hpbmEuY24iLCJwdWlkIjoiMTAwMzMyMzBDNjNDN0Y3MSIsImNhY2hla2V5IjoiMGguZnxtZW1iZXJzaGlwfDEwMDMzMjMwYzYzYzdmNzFAbGl2ZS5jb20iLCJzY3AiOiJteWZpbGVzLnJlYWQgYWxsZmlsZXMucmVhZCBhbGxmaWxlcy53cml0ZSBhbGxwcm9maWxlcy5yZWFkIiwidHQiOiIyIiwidXNlUGVyc2lzdGVudENvb2tpZSI6bnVsbCwiaXBhZGRyIjoiNTIuMTMwLjEwLjE2MSJ9.RkR3ZkhsNHBNdzgwMGYrT0JZNXIwb3VIbUljc3VvM0MvK1ZqZENaaFdCMD0&ApiVersion=2.0";
    public String path1 = "/home/kiylx/下载/testFload";
    public String path2;
    public DownloadInfo info = new DownloadInfo(url1, path1, "原神");

    public TestThread() {
        initTest=InitTest.getInstance();
    }

    public DownloadInfo getDownloadInfo(String url, String path, String name){
        return new DownloadInfo(url,path,name);
    }

    @Override
    public void run() {

        //testFS();
        try {
            test1(url1,path1,"原神");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*try {
            test2(url2,path1,"视频软件");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }


    public void testFS(){
        getContext().getFileKit().create(path1,true);
    }

    public void test1(String url, String path, String name) throws InterruptedException {
        DownloadInfo info =getDownloadInfo(url, path, name);
        execDownload(url, path, name);//如果不能立马拿到info,这里阻塞的话，会没法向下执行
        //sleep(2000L);
        //stopDownload(info);
        //sleep(2000L);
        //resumeDownload(info);
    }

    public void test2(String url, String path, String name) throws InterruptedException {
        DownloadInfo info =getDownloadInfo(url, path, name);
        execDownload(url, path, name);
        sleep(2000L);
        cancelDownload(info);
    }

    public DownloadInfo execDownload(String url, String path, String name) {
        return initTest.downloads.execDownloadTask(url, path, name);
    }

    public void stopDownload(DownloadInfo info) {
        initTest.downloads.pauseDownload(info.getUuid());
    }

    public void resumeDownload(DownloadInfo info) {
        initTest.downloads.resumeTask(info.getUuid());
    }

    public void cancelDownload(DownloadInfo info) {
        initTest.downloads.cancelTask(info.getUuid());
    }
}