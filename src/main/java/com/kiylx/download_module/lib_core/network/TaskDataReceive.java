package com.kiylx.download_module.lib_core.network;

import com.kiylx.download_module.lib_core.interfaces.RemoteRepo;
import com.kiylx.download_module.lib_core.interfaces.Repo;
import com.kiylx.download_module.lib_core.model.DownloadInfo;
import com.kiylx.download_module.lib_core.model.VerifyResult;
import com.kiylx.download_module.lib_core.interfaces.ConnectionListener;
import com.kiylx.download_module.lib_core.network.HttpManager;
import com.kiylx.download_module.lib_core.repository.sqlite.DownloadDb;
import com.kiylx.download_module.utils.DateUtils;
import com.kiylx.download_module.utils.TextUtils;
import com.kiylx.download_module.utils.java_log_pack.Log;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;

import static com.kiylx.download_module.ContextKt.getContext;
import static com.kiylx.download_module.lib_core.model.StatusCode.*;
import static com.kiylx.download_module.utils.Utils.EXTENSION_SEPARATOR;
import static com.kiylx.download_module.utils.Utils.parseMIMEType;
import static java.net.HttpURLConnection.*;

public class TaskDataReceive implements RemoteRepo {
    /**
     * @param info    解析得到的网络数据会写入info
     * @param queryDb true:查询数据库，恢复存储起来的header数据； false：新任务，不需要查询数据库
     * @return
     */
    @Nullable
    public static VerifyResult fetchMetaData(DownloadInfo info, boolean queryDb) {
        System.out.println("获得meta信息");

        final VerifyResult[] result = new VerifyResult[1];
        Request.Builder builder = new Request.Builder()
                .url(info.getUrl());//构建request
        if (!TextUtils.isEmpty(info.getUserAgent())) {
            builder.addHeader("User-Agent", info.getUserAgent());
        }
        if (!info.getReferer().isEmpty())
        builder.addHeader("Referer",info.getReferer());

        System.out.println("调用getResponse方法");
        try {//尝试请求，获得结果并解析
            getContext().getHttpManager().getResponse(builder.build(), new ConnectionListener() {
                @Override
                public void onResponseHandle(Response response, int code, String message) {
                    System.out.println("网络回应：" + code);
                    switch (code) {
                        case HTTP_OK:
                            result[0] = parseHeaders(info, response, queryDb);
                            break;
                        case HTTP_PRECON_FAILED:
                            result[0] = new VerifyResult(STATUS_CANNOT_RESUME,
                                    "Precondition failed", null);
                            break;
                        case HTTP_UNAVAILABLE:
                            parseUnavailableHeaders(info, response);
                            result[0] = new VerifyResult(HTTP_UNAVAILABLE, message, null);
                            break;
                        case HTTP_INTERNAL_ERROR:
                            result[0] = new VerifyResult(HTTP_INTERNAL_ERROR, message, null);
                            break;
                        default:
                            result[0] = getUnhandledHttpError(code, message);
                            break;
                    }
                }

                @Override
                public void onMovedPermanently(int httpCode, String newUrl) {
                    if (httpCode == HTTP_MOVED_PERM)
                        info.setUrl(newUrl);
                }

                @Override
                public void onIOException(IOException e) {

                }

                @Override
                public void onTooManyRedirects() {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Repo repo = getContext().getRepo();
            if (result[0] != null)
                DownloadInfo.modifyMsg(info, result[0]);//下载结果写入DownloadInfo
            if (repo != null) {
                repo.syncInfoToDisk(info, Repo.SyncAction.UPDATE);//更新存储库中downloadInfo信息
            }
        }
        return result[0];
    }

    /**
     * 测试连接如何
     *
     * @param info downloadinfo
     */
    @Nullable
    public static void testConnect(DownloadInfo info, ConnectionListener listener) {
        Request.Builder builder = new Request.Builder()
                .url(info.getUrl());//构建request
        if (!TextUtils.isEmpty(info.getUserAgent())) {
            builder.addHeader("User-Agent", info.getUserAgent());
        }
        System.out.println("调用getResponse方法");
        try {//尝试请求，获得结果并解析
            getContext().getHttpManager().getResponse(builder.build(), listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析得到的回应，
     *
     * @param info
     * @param response
     * @return
     */
    protected static VerifyResult parseHeaders(DownloadInfo info, Response response, boolean queryDb) {
        System.out.println("解析headers");
        ResponseBody body = response.body();
        Repo repo = getContext().getRepo();
        MediaType mimetype = null;
        String fileName = null;
        String ext = null;
        if (body != null) {
            mimetype = body.contentType();
        }
        /*
        如果contentType不存在或者表示自己是个字节流，尝试通过文件名称确定mime type
        例如：
        Content-Type: application/octet-stream
        Content-Disposition: attachment; filename="picture.png"
        意思是我不知道这是什么，你应该保存它，命名为“ picture.png”

        Content-Type: image/png
        Content-Disposition: attachment; filename="picture.png"
        意思是：这是一个图片，请保存它，建议命名为“ picture.png”

        Content-Type: image/png
        Content-Disposition: inline; filename="picture.png"
        意思是：这是一个 PNG 图像。 请显示它，除非您不知道如何显示 PNG 图像。 否则，或者如果用户选择保存它，我们建议您将其保存为的文件名称为picture.png*/

        if (mimetype == null || mimetype.type().equals("application/octet-stream")) {//application/octet-stream 告知客户端这是一个字节流
            //解析mimetype,文件名称，文件后缀
            String contentDisposition = response.header("Content-Disposition");
            String contentLocation = response.header("Content-Location");
            String tmpUrl = response.request().url().toString();

            String[] mime = parseMIMEType(tmpUrl, contentDisposition, contentLocation);
            fileName = mime[0];
            mimetype = MediaType.parse(mime[1]);
            ext = mime[2];
        }

        //更新downloadInfo部分信息
        if (mimetype != null && !mimetype.type().equals(info.getMimeType()))
            info.setMimeType(mimetype.type());

        if (info.getFileName().isEmpty() && fileName != null) {
            info.setFileName(fileName);
        } else {
            info.setFileName(info.getFileName() + EXTENSION_SEPARATOR + ext);
        }
        info.setPath(info.getFileFolder() + File.separator + info.getFileName());

        final String transferEncoding = response.header("Transfer-Encoding");
        if (transferEncoding == null) {
            info.setTotalBytes(body.contentLength());
        } else {
            info.setTotalBytes(-1);//长度未知
        }
        boolean partialSupport = "bytes".equalsIgnoreCase(response.header("Accept-Ranges"));
        info.setPartialSupport(partialSupport);//是否支持断点请求
        info.setThreadCounts(partialSupport ? getContext().getDefaultThreadNum() : 1);
        String eTagValue = response.header("ETag");
        repo.updateHeader(info.getUuid(), "ETag", eTagValue);

        if (!queryDb && info.getTotalBytes() != -1) {//新任务，分配每个分块的大小。
            DownloadInfo.allocPieceFileSize(info);
        }
        info.setHasMetadata(true);
        info.setFinalCode(STATUS_RUNNING);
        repo.syncInfoToDisk(info, Repo.SyncAction.UPDATE);//更新存储库中downloadInfo信息
        System.out.println("解析结束");
        DownloadInfo info1 = info;
        return null;
    }

    public static void parseUnavailableHeaders(DownloadInfo info, @NonNull Response response) {
        String header = response.header("Retry-After", "-1");
        long retryAfter = Long.parseLong(header);

        if (retryAfter > 0)
            info.setRetryAfter(constrainRetryAfter(retryAfter));
    }

    private static int constrainRetryAfter(long retryAfter) {
        retryAfter = constrain(retryAfter,
                DownloadInfo.minRetryAfter,
                DownloadInfo.maxRetryAfter);

        return (int) (retryAfter * DateUtils.SECOND_IN_MILLIS);
    }

    private static long constrain(long amount, long low, long high) {
        return amount < low ? low : (Math.min(amount, high));
    }

    public static VerifyResult getUnhandledHttpError(int code, String message) {
        final String error = "Unhandled HTTP response: " + code + " " + message;
        if (code >= 400 && code < 600)
            return new VerifyResult(code, error, null);
        else if (code >= 300 && code < 400)
            return new VerifyResult(STATUS_UNHANDLED_REDIRECT, error, null);
        else
            return new VerifyResult(STATUS_UNHANDLED_HTTP_CODE, error, null);
    }

}
