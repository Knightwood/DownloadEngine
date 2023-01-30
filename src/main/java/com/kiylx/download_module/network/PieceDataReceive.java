package com.kiylx.download_module.network;

import com.kiylx.download_module.interfaces.ConnectionListener;
import com.kiylx.download_module.interfaces.RemoteRepo;
import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.model.HeaderName;
import com.kiylx.download_module.model.HeaderStore;
import com.kiylx.download_module.model.HeaderStoreKt;
import com.kiylx.download_module.utils.TextUtils;
import okhttp3.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.kiylx.download_module.ContextKt.getContext;


public class PieceDataReceive implements RemoteRepo {

    public static void getResponse(DownloadInfo info, long start, long end, ConnectionListener connectionListener) {
        Request.Builder builder = new Request.Builder().url(info.getUrl());
        Map<String, String> headerMap = HeaderStoreKt.filterCustomHeaders(info);
        headerMap.forEach(builder::addHeader);//添加自定义header
        /*
         * Defeat transparent gzip compression, since it doesn't allow us to
         * easily resume partial downloads.
         */
        builder.addHeader("Accept-Encoding", "identity");
        /*
         * Defeat connection reuse, since otherwise servers may continue
         * streaming large downloads after cancelled.
         */
        builder.addHeader("Connection", "close");

        String eTag = info.getCustomHeaders().get(HeaderName.ETag);
        if (!TextUtils.isEmpty(eTag))
            builder.addHeader("If-Match", eTag);
        if (start != -1 && end != -1)//支持分块
            builder.addHeader(HeaderName.Range, HeaderName.rangeStr(start, end));
        Request request = builder.build();
        //请求网络
        getContext().getHttpManager().getResponse(request, connectionListener);
    }

}
