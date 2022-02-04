package com.kiylx.download_module.lib_core.network;

import com.kiylx.download_module.lib_core.model.DownloadInfo;
import com.kiylx.download_module.utils.TextUtils;
import com.kiylx.download_module.viewTest.InitTest;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static com.kiylx.download_module.lib_core.network.TaskDataReceive.parseHeaders;
import static java.net.HttpURLConnection.HTTP_OK;

class TaskDataReceiveTest {
    Response response;
    InitTest initTest;
    DownloadInfo info;


    void setUp() {
        initTest = InitTest.getInstance();
        info = initTest.info;

        Request.Builder builder = new Request.Builder()
                .url(info.getUrl());
        if (!TextUtils.isEmpty(info.getUserAgent())) {
            builder.addHeader("User-Agent", info.getUserAgent());
        }
        response = initTest.context.getHttpManager().getResponse(builder.build());
        if (response.code() == HTTP_OK || response.code() == 206) {
            parseHeaders(info, response, false);
        }
    }

    @Test
    public void testExt() {
        setUp();
        String contentDisposition = response.header("Content-Disposition");
        String contentLocation = response.header("Content-Location");
        System.out.println(contentDisposition);
        System.out.println(contentLocation);

        String tmpUrl = response.request().url().toString();
        String fileName = null;
        String ext = null;
        MediaType mimetype = null;
        //String[] mime = parseMIMEType(tmpUrl, contentDisposition, contentLocation);
        //fileName = mime[0];
        //mimetype = MediaType.parse(mime[1]);//todo 这里传入的参数怎么会是null？？
        //ext = mime[2];
        //System.out.println(fileName);
        //System.out.println(mimetype);
        //System.out.println(ext);
    }
}