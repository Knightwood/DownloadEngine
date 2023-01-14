package com.kiylx.download_module.network;

import com.kiylx.download_module.interfaces.ConnectionListener;
import com.kiylx.download_module.interfaces.RemoteRepo;
import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.model.HeaderStore;
import com.kiylx.download_module.utils.TextUtils;
import okhttp3.Request;

import static com.kiylx.download_module.ContextKt.getContext;


public class PieceDataReceive implements RemoteRepo {

    public static void getResponse(DownloadInfo info, long start, long end, ConnectionListener connectionListener) {
        Request.Builder builder = new Request.Builder().url(info.getUrl());

        HeaderStore[] tmp = getContext().getRepo().getHeadersById(info.getUuid(), "ETag");
        if (tmp != null)
            for (HeaderStore header : tmp) {
                builder.addHeader(header.name, header.value);
            }
        if (!TextUtils.isEmpty(info.getUserAgent()))
            builder.addHeader("User-Agent", info.getUserAgent());
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
        if (!info.getReferer().isEmpty())
            builder.addHeader("Referer", info.getReferer());
        HeaderStore[] eTag = getContext().getRepo().getHeadersByName(info.getUuid(), "ETag");
        if (eTag != null && eTag.length > 0)
            builder.addHeader("If-Match", eTag[0].value);
        if (start != -1 && end != -1)//支持分块
            builder.addHeader("Range", "bytes=" + start + "-" + end);
        Request request = builder.build();
        //请求网络
        getContext().getHttpManager().getResponse(request, connectionListener);
    }

}
