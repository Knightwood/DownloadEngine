package com.kiylx.download_module.viewTest;

import com.kiylx.download_module.lib_core.interfaces.DownloadResultListener;
import com.kiylx.download_module.view.SimpleDownloadInfo;

import java.util.List;
import java.util.function.Consumer;

public class TextConsel implements DownloadResultListener {

    @Override
    public void updatedActive(List<SimpleDownloadInfo> list) {
        list.forEach(
                new Consumer<SimpleDownloadInfo>() {
                    @Override
                    public void accept(SimpleDownloadInfo simpleDownloadInfo) {
                        System.out.print("Everything on the console will cleared");
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                        simpleDownloadInfo.print();
                    }
                }
        );
    }

    @Override
    public void updatedWait(List<SimpleDownloadInfo> list) {

    }

    @Override
    public void updatedFinish(List<SimpleDownloadInfo> list) {

    }
}
