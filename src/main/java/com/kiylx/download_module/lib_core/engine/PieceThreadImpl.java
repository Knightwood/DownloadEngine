package com.kiylx.download_module.lib_core.engine;

import com.kiylx.download_module.file_platform.FakeFile;
import com.kiylx.download_module.lib_core.interfaces.ConnectionListener;
import com.kiylx.download_module.lib_core.interfaces.DownloadTask;
import com.kiylx.download_module.lib_core.interfaces.PieceThread;
import com.kiylx.download_module.lib_core.interfaces.Repo;
import com.kiylx.download_module.lib_core.model.*;
import com.kiylx.download_module.lib_core.network.PieceDataReceive;
import io.reactivex.annotations.NonNull;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.kiylx.download_module.ContextKt.getContext;
import static com.kiylx.download_module.lib_core.model.StatusCode.*;
import static com.kiylx.download_module.lib_core.network.TaskDataReceive.getUnhandledHttpError;
import static com.kiylx.download_module.lib_core.network.TaskDataReceive.parseUnavailableHeaders;
import static java.net.HttpURLConnection.*;

public class PieceThreadImpl extends PieceThread {
    private DownloadTask.TaskCallback callback;
    private FakeFile rf;
    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onResponseHandle(Response response, int code, String message) {
            System.out.println("分块的response: " + code);
            switch (code) {
                case HTTP_OK:
                case HTTP_PARTIAL:
                    transferData(response);
                    break;
                case HTTP_PRECON_FAILED:
                    generatePieceResult(STATUS_CANNOT_RESUME,
                            "Precondition failed");
                    break;
                case HTTP_UNAVAILABLE:
                    parseUnavailableHeaders(callback.getDownloadInfo(), response);
                    generatePieceResult(HTTP_UNAVAILABLE, message);
                    break;
                case HTTP_INTERNAL_ERROR:
                    generatePieceResult(HTTP_INTERNAL_ERROR, message);
                    break;
                default:
                    VerifyResult verifyResult = getUnhandledHttpError(code, message);
                    generatePieceResult(verifyResult.getFinalCode(), verifyResult.getMessage());
                    break;
            }
        }

        @Override
        public void onMovedPermanently(int httpCode, String newUrl) {

        }

        @Override
        public void onIOException(IOException e) {
            if (e instanceof ProtocolException && e.getMessage().startsWith("Unexpected status line"))
                generatePieceResult(STATUS_UNHANDLED_HTTP_CODE, "", e);
            else if (e instanceof SocketTimeoutException)
                generatePieceResult(HTTP_GATEWAY_TIMEOUT, "Download timeout");
            else
                /* Trouble with low-level sockets */
                generatePieceResult(STATUS_HTTP_DATA_ERROR, "", e);
        }

        @Override
        public void onTooManyRedirects() {
            generatePieceResult(STATUS_TOO_MANY_REDIRECTS, "Too many redirects");
        }
    };

    /**
     * @param callback Task实现此接口，分块任务用它进行沟通
     * @param id       downloadInfo 's uuid
     * @param blockId  pieceThread 's blockId
     * @param start    piece download block 's start pos
     * @param end      piece download block 's end pos
     */
    public PieceThreadImpl(DownloadTask.TaskCallback callback, UUID id, int blockId, long start, long end) {
        this(callback, new PieceInfo(id, blockId, start, end));
    }

    public PieceThreadImpl(DownloadTask.TaskCallback callback, @NonNull PieceInfo pieceInfo) {
        super(pieceInfo);
        this.callback = callback;
        isRunning = true;//Very important 。 After investigating the bug for a long time, I finally found that it was because this value was not set here。fuck！！！
    }

    @Override
    public PieceResult call() {
        if (pieceInfo.getFinalCode() == StatusCode.STATUS_SUCCESS || pieceInfo.getTotalBytes() == 0)
            return generatePieceResult(pieceCode.success, "SUCCESS");
        pieceInfo.clean();
        pieceInfo.setFinalCode(StatusCode.STATUS_ACTIVE);
        //下面开始传输数据
        rf = callback.getFile();
        rf.seek(getStart());
        PieceDataReceive.getResponse(callback.getDownloadInfo(), getStart(), getEnd(), connectionListener);
        return pieceResult;
    }

    private boolean verifyResponse(Response response) {
        /*
         * To detect when we're really finished, we either need a length, closed
         * connection, or chunked encoding.
         * 为了检测我们何时真正完成，我们需要一个长度、闭合连接或分块编码。
         */
        boolean hasLength = getTotalBytes() != -1;//有长度
        boolean isConnectionClose = "close".equalsIgnoreCase(response.header("Connection"));//关闭
        boolean isEncodingChunked = "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"));//chunked分片

        if (!(hasLength || isConnectionClose || isEncodingChunked)) {//没有长度信息，没有关闭，也不是chunked
            //尝试获取内容长度
            try {
                long contentLength = Long.parseLong(response.header("Content-Length"));
                if (contentLength != -1 && pieceInfo.getBlockId() == 0) {//得到长度信息，赋值
                    pieceInfo.setTotalBytes(contentLength);
                } else {//没有获得长度信息，报错
                    generatePieceResult(STATUS_CANNOT_RESUME,
                            "Can't know size of download, giving up");
                    return false;
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
                generatePieceResult(STATUS_CANNOT_RESUME,
                        "Can't know size of download, giving up");
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        System.out.println("分块校验成功");
        return true;
    }

    private void transferData(Response response) {
        System.out.println("传输数据");
        if (!verifyResponse(response))//验证未通过，return
            return;
        if (response.body() != null) {
            try (InputStream inputStream = Objects.requireNonNull(response.body()).byteStream()) {
                byte[] b = new byte[BUFFER_SIZE];
                //从流中读取的数据长度
                int len;
                //流没有读尽和没有暂停时执行循环以写入文件
                while (((len = inputStream.read(b)) != -1) && isRunning) {
                    rf.write(b, 0, len);
                    startPlus(len);//累加进度
                    if (callback != null)
                        //callback.update(pieceInfo, isRunning);
                        updateProgress(len);
                }
                //流没有读尽且暂停时的处理
                if ((!isRunning) && len != -1) {
                    System.out.println("暂停");
                    //暂停
                    generatePieceResult(pieceCode.stop, "Stop");
                } else {
                    System.out.println("完成下载");
                    //完成无误； 如果已知，则验证长度
                    if (getTotalBytes() != -1 && getCurBytes() != getEnd() + 1) {
                        generatePieceResult(STATUS_HTTP_DATA_ERROR,
                                "Piece length mismatch; found "
                                        + getCurBytes() + " instead of " + (getEnd() + 1));
                    }//分块下载成功
                    generatePieceResult(pieceCode.success, "SUCCESS");
                }

            } catch (Throwable e) {
                e.printStackTrace();
                generatePieceResult(pieceCode.error, "some thing wrong", e);
            } finally {
                if (callback != null)
                    callback.update(pieceInfo, isRunning);
                closeThings();
            }
        }
    }

    private long currentTime = 0L;
    private long currentSize = 0L;

    //以时间间隔更新分块及及info信息
    private void updateProgress(long len) {
        long deltaTime = System.currentTimeMillis() - currentTime;
        long deltaSize = getCurBytes() - currentSize;

        if (deltaTime >= MIN_PROGRESS_TIME && deltaSize >= MIN_PROGRESS_STEP) {
            currentTime = System.currentTimeMillis();
            currentSize = getCurBytes();

            long speed = deltaSize / deltaTime * 1000; // bytes/s 分块的速度
            pieceInfo.setSpeed(speed);
        }
        if (callback != null)
            callback.update(pieceInfo, isRunning);
    }
    /**
     * 线程之行结束的清理
     */
    private void closeThings() {
        try {
            if (rf != null) {
                rf.close();
                rf = null;
            }
            callback = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<PieceThreadImpl> generatePieceList(Repo repo,
                                                          DownloadInfo info,
                                                          DownloadTask.TaskCallback callback,
                                                          boolean shouldQueryDb) {
        List<PieceThreadImpl> result = new ArrayList<>();
        if (shouldQueryDb) {//从磁盘恢复
            List<PieceInfo> pieceInfoList = repo.queryPieceInfo(info.getUuid());
            if (!pieceInfoList.isEmpty() && info.getThreadCounts() == pieceInfoList.size()) {
                for (PieceInfo pieceInfo : pieceInfoList) {
                    result.add(new PieceThreadImpl(callback, pieceInfo));
                }
                repo.deletePieceInfo(info.getUuid());//已经生成了新的pieceThread,删除存储库中的旧数据
            } else {
                repo.deletePieceInfo(info.getUuid());

                getContext().getFileKit().rmdir(info.getPath());
                return generateNewPieceList(info, callback);
            }
        } else {
            return generateNewPieceList(info, callback);
        }

        return result;
    }

    public static List<PieceThreadImpl> generateNewPieceList(DownloadInfo info,
                                                             DownloadTask.TaskCallback callback) {
        List<PieceThreadImpl> result = new ArrayList<>();
        List<PieceInfo> pieceInfos = new ArrayList<>();
        //新任务
        if (info.getThreadCounts() == 1 || !info.isPartialSupport()) {//单线程下载
            result = new ArrayList<>(1);
            PieceThreadImpl thread = new PieceThreadImpl(callback, info.getUuid(), 0, 0, info.getTotalBytes());
            result.add(thread);
            pieceInfos.add(thread.pieceInfo);
        } else {
            for (int i = 0; i < info.getThreadCounts(); i++) {
                PieceThreadImpl thread = new PieceThreadImpl(callback, info.getUuid(), i, info.getSplitStart()[i], info.getSplitEnd()[i]);
                result.add(thread);
                pieceInfos.add(thread.pieceInfo);
            }
        }
        return result;

    }
}
