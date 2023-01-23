package com.kiylx.download_module.lib_core.engine1;

import com.kiylx.download_module.file.file_platform.FakeFile;
import com.kiylx.download_module.interfaces.ConnectionListener;
import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.interfaces.PieceThread;
import com.kiylx.download_module.interfaces.Repo;
import com.kiylx.download_module.model.*;
import com.kiylx.download_module.network.PieceDataReceive;
import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import static com.kiylx.download_module.ContextKt.getContext;
import static com.kiylx.download_module.model.StatusCode.*;
import static com.kiylx.download_module.network.TaskDataReceive.getUnhandledHttpError;
import static com.kiylx.download_module.network.TaskDataReceive.parseUnavailableHeaders;
import static java.net.HttpURLConnection.*;

public class PieceThreadImpl extends PieceThread {
    protected static final Logger logger= JavaLogUtil.setLoggerHandler();

    private DownloadTask.TaskCallback callback;
    private FakeFile rf;
    private InputStream inputStream =null;
    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onResponseHandle(Response response, int code, String message) {
            logger.info("分块的response: " + code);
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
                    TaskResponse taskResponse = getUnhandledHttpError(code, message);
                    generatePieceResult(taskResponse.getFinalCode(), taskResponse.getMessage());
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

    public PieceThreadImpl(DownloadTask.TaskCallback callback, PieceInfo pieceInfo) {
        super(pieceInfo);
        this.callback = callback;
        isRunning = true;//Very important 。 After investigating the bug for a long time, I finally found that it was because this value was not set here。fuck！！！
    }

    @Override
    public PieceResult call() {
        if (getPieceInfo().getFinalCode() == StatusCode.STATUS_SUCCESS || getPieceInfo().getTotalBytes() == 0)
            return generatePieceResult(PieceCode.success, "SUCCESS");
        getPieceInfo().clean();
        getPieceInfo().setFinalCode(StatusCode.STATUS_ACTIVE);
        //下面开始传输数据
        rf = callback.getFile();
        rf.seek(getStart());
        PieceDataReceive.getResponse(callback.getDownloadInfo(), getStart(), getEnd(), connectionListener);
        return pieceResult;
    }

    //todo 这里真的需要验证吗？？？
    private boolean verifyResponse(Response response) {
        /*
         * To detect when we're really finished, we either need a length, closed
         * connection, or chunked encoding.
         * 为了检测我们何时真正完成，我们需要一个长度、闭合连接或分块编码。
         */
        boolean hasLength = getTotalBytes() >0;//有长度
        boolean isConnectionClose = "close".equalsIgnoreCase(response.header("Connection"));//关闭
        boolean isEncodingChunked = "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"));//chunked分片

        if (!(hasLength || isConnectionClose || isEncodingChunked)) {
            //没有长度信息，没有关闭，也不是chunked
            //尝试获取内容长度
            try {
                long contentLength = Long.parseLong(response.header("Content-Length"));
                if (contentLength != -1) {//得到长度信息，赋值
                    getPieceInfo().setTotalBytes(contentLength);
                } else {//没有获得长度信息，报错
                    generatePieceResult(STATUS_CANNOT_RESUME,
                            "Can't know size of download, giving up");
                    logger.info("Can't know size of download, giving up1");
                    return false;
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
                generatePieceResult(STATUS_CANNOT_RESUME,
                        "Can't know size of download, giving up");
                logger.info("Can't know size of download, giving up2");
                return false;
            } catch (Exception e) {
                logger.info("Can't know size of download, giving up3");
                e.printStackTrace();
                return false;
            }
        }
        logger.info("分块校验成功");
        return true;
    }

    private void transferData(Response response) {
        logger.info("传输数据");
        //todo 这里真的需要验证吗？？？
//        if (!verifyResponse(response))//验证未通过，return
//            return;
        if (response.body() != null) {
            try {
                inputStream = Objects.requireNonNull(response.body()).byteStream();
                byte[] b = new byte[BUFFER_SIZE];
                //从流中读取的数据长度
                int len;
                //流没有读尽和没有暂停时执行循环以写入文件
                while (((len = inputStream.read(b)) != -1) && isRunning) {
                    rf.write(b, 0, len);
                    curBytesPlus(len);//累加进度
                    if (callback != null)
                        updateProgress();
                }
                //流没有读尽且暂停时的处理
                if ((!isRunning) && len != -1) {
                    logger.info("暂停");
                    //暂停
                    generatePieceResult(PieceCode.stop, "Stop");
                } else {
                    logger.info("完成下载");
                    //完成无误； 如果已知，则验证长度
                    if (getTotalBytes() != -1 && getCurBytes() != getEnd() + 1) {
                        generatePieceResult(STATUS_HTTP_DATA_ERROR,
                                "Piece length mismatch; found "
                                        + getCurBytes() + " instead of " + (getEnd() + 1));
                    }//分块下载成功
                    generatePieceResult(PieceCode.success, "SUCCESS");
                }

            } catch (Throwable e) {
                e.printStackTrace();
                generatePieceResult(PieceCode.error, "some thing wrong", e);
            } finally {
                if (callback != null)
                    callback.update(getPieceInfo(), isRunning);
                closeThings(response);
            }
        }
    }

    private long currentTime = 0L;
    private long currentSize = 0L;

    //以时间间隔更新分块及及info信息
    private void updateProgress() {
        long deltaTime = System.currentTimeMillis() - currentTime;
        if (deltaTime >= MIN_PROGRESS_TIME) {
            currentTime = System.currentTimeMillis();
            long deltaSize = getCurBytes() - currentSize;
            currentSize = getCurBytes();

            long speed = deltaSize / deltaTime * 1000; // bytes/s 分块的速度
            getPieceInfo().setSpeed(speed);
        }
        if (callback != null)
            callback.update(getPieceInfo(), isRunning);
    }
    /**
     * 线程之行结束的清理
     */
    private void closeThings(Response response) {
        try {
            if (response != null) {
                response.close();
            }
            if (rf != null) {
                rf.close();
                rf = null;
            }
            if (inputStream!=null){
                inputStream.close();
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
            List<PieceInfo> pieceInfoList = info.getPiecesList();
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

    public static List<PieceThreadImpl> generateNewPieceList(DownloadInfo downloadInfo,
                                                             DownloadTask.TaskCallback callback) {
        List<PieceThreadImpl> result = new ArrayList<>();
        List<PieceInfo> pieceInfos = new ArrayList<>();
        //新任务
        if (downloadInfo.getThreadCounts() == 1 || !downloadInfo.isPartialSupport()) {//单线程下载
            result = new ArrayList<>(1);
            PieceInfo pieceInfo =new PieceInfo(downloadInfo.getUuid(), 0, 0, downloadInfo.getTotalBytes());
            PieceThreadImpl thread = new PieceThreadImpl(callback, pieceInfo);
            result.add(thread);
            pieceInfos.add(thread.getPieceInfo());
        } else {
            for (int i = 0; i < downloadInfo.getThreadCounts(); i++) {
                PieceInfo pieceInfo =new PieceInfo(downloadInfo.getUuid(), i, downloadInfo.getSplitStart()[i], downloadInfo.getSplitEnd()[i]);
                PieceThreadImpl thread = new PieceThreadImpl(callback, pieceInfo);
                result.add(thread);
                pieceInfos.add(thread.getPieceInfo());
            }
        }
        downloadInfo.getPiecesList().clear();
        downloadInfo.getPiecesList().addAll(pieceInfos);
        return result;

    }
}
