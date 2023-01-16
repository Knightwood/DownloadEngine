package com.kiylx.download_module.lib_core.engine1;

import com.kiylx.download_module.file.file_platform.FakeFile;
import com.kiylx.download_module.file.fileskit.FileKit;
import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.interfaces.PieceThread;
import com.kiylx.download_module.interfaces.Repo;
import com.kiylx.download_module.model.*;
import com.kiylx.download_module.network.HttpUtils;
import com.kiylx.download_module.network.TaskDataReceive;
import com.kiylx.download_module.utils.java_log_pack.JavaLogUtil;
import io.reactivex.annotations.NonNull;
import kotlin.Pair;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.kiylx.download_module.Context.updateViewInterval;
import static com.kiylx.download_module.ContextKt.getContext;
import static com.kiylx.download_module.model.StatusCode.*;
import static com.kiylx.download_module.model.TaskResult.TaskResultCode.*;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * 下载执行过程
 * TaskHandler->DownloadTask->PieceThread
 * pieceThread执行每个分块的下载，并将PieceResult作为结果返回给DownloadTask
 * DownloadTask将收集此下载任务所有PieceThread的PieceResult,生成TaskResponse,然后使用TaskResponse生成TaskResult返回给TaskHandler。
 * 下载之前的验证过程也会生成TaskResponse,如果验证的结果是下载失败，将使用TaskResponse生成TaskResult返回给TaskHandler。
 */
public class DownloadTaskImpl extends DownloadTask {
    protected final Logger logger = JavaLogUtil.setLoggerHandler();

    private List<PieceThreadImpl> pieceThreads = Collections.emptyList();
    private ExecutorService exec;
    private final Repo repo = getContext().getRepo();
    private Timer recordHandler = null;
    private boolean exit = false;//任务执行结束时，为true，可根据此标志做一些清理工作

    public final TaskCallback callback = new TaskCallback() {
        @Override
        public void update(PieceInfo pieceInfo, boolean isRunning) throws NullPointerException {
            info.setRunning(isRunning);
            //更新进度以及将下载信息保存到磁盘
            //calcSpeedAndSave();
        }

        @Override
        public FakeFile getFile() {
            return Objects.requireNonNull(checkDiskAndInitFile()).getFirst();
        }

        @Override
        public DownloadInfo getDownloadInfo() {
            return info;
        }
    };


    public DownloadTaskImpl(@NonNull DownloadInfo info) {
        super(info);
    }

    public static DownloadTask instance(DownloadInfo info) {
        return new DownloadTaskImpl(info);
    }

    private long lastSize = 0L;//上次计算速度时的文件大小
    private long lastTime = 0L;//上次计算速度时的时间

    @Override
    protected DownloadTask initTask() {
        setLifecycleState(TaskLifecycle.CREATE);
        return this;
    }

    /**
     * 清理下载任务
     * 包括出错后重新尝试下载的任务、从磁盘恢复的任务
     */
    private TaskResult cleanTask() {
        if (info.getFinalCode() == STATUS_SUCCESS || info.getFinalCode() == STATUS_CANCELLED) {
            return new TaskResult(info.getUuid(), TaskResult.TaskResultCode.DOWNLOAD_COMPLETE,
                    "Download was success or canceled",
                    STATUS_SUCCESS
            );//文件下载成功或取消
        }
        info.setFinalCode(STATUS_INIT);
        info.setFinalMsg("");

        return null;
    }

    @Override
    public TaskResult call() {
        try {
            initTask();
            TaskResult cleanTaskResult = cleanTask();
            if (cleanTaskResult != null)
                return cleanTaskResult;
            TaskResult taskResult = runDownload();
            if (taskResult != null)
                return taskResult;
            return new TaskResult(info.getUuid(), TaskResult.TaskResultCode.ERROR, info.getFinalMsg(), info.getFinalCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private TaskResult runDownload() {
        logger.info("下载任务：runDownload方法被调用");
        try {
            TaskResponse taskResponse = execPieceDownload();//执行分块下载
            //todo 应该在execPieceDownload()方法里写入结果   DownloadInfo.modifyMsg(info, verifyResult);//下载结果写入DownloadInfo
            return new TaskResult(info.getUuid(), taskResponse);
        } catch (Throwable t) {
            t.printStackTrace();
            if (info != null) {
                DownloadInfo.modifyMsg(info, STATUS_UNKNOWN_ERROR, t.getMessage());//下载结果写入DownloadInfo
            }
        } finally {
            setLifecycleState(TaskLifecycle.STOP);//todo ????
            syncInfo(Repo.SyncAction.UPDATE);//结果同步到磁盘
            closeThings();
        }
        return null;
    }

    private TaskResponse execPieceDownload() {
        TaskResponse pieceThreadResult = null;//分块下载结果
        List<Future<PieceResult>> futureList = Collections.emptyList();//存储分块任务（callable）的返回结果（future）
        setLifecycleState(TaskLifecycle.RUNNING);

        boolean shouldQueryDb = (isRecoveryFromDisk() || info.getRetryCount() > 0);//旧任务或者尝试重新下载的任务
        //验证连接有效性
        logger.info("调用fetchMetaData之前 以及 是否是旧任务" + shouldQueryDb);
        TaskResponse metaResult;
        metaResult = TaskDataReceive.fetchMetaData(info, shouldQueryDb);
        if (metaResult != null)
            return metaResult;
        if (info.getTotalBytes() == 0) {
            return genTaskResponse(STATUS_SUCCESS, "Length is zero; skipping", DOWNLOAD_COMPLETE);
        }
        //验证网络连接
        if (!HttpUtils.checkConnectivity()) {
            return genTaskResponse(STATUS_WAITING_FOR_NETWORK, "LOST CONNECTING", FAILED);
        }
        logger.info("调用fetchMetaData之后，创建分块执行下载");
        try {//创建分块执行下载
            Pair<FakeFile, TaskResponse> fileVerifyResultPair = checkDiskAndInitFile();//创建文件
            if (fileVerifyResultPair.getSecond() != null) {
                return fileVerifyResultPair.getSecond();
            }
            fakeFile = fileVerifyResultPair.getFirst();//赋予文件
            exec = (info.getThreadCounts() == 1 || !info.isPartialSupport()) ?
                    Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(info.getThreadCounts());
            pieceThreads = PieceThreadImpl.generatePieceList(repo, info, callback, shouldQueryDb);//生成分块下载线程
            info.reduceRetryCount();
            setRecoveryFromDisk(false);
            //Wait all threads
            logger.info("等待线程池下载完成");
            syncInfo(Repo.SyncAction.ADD);//此处信息初始化完成，开始执行下载。将信息存入数据库
            timerUpdate();
            futureList = exec.invokeAll(pieceThreads);
        } catch (Exception e) {
            e.printStackTrace();
            //logger.info(Thread.currentThread().getName() + "被中断, 中断标识: " + Thread.currentThread().isInterrupted());
            setLifecycleState(TaskLifecycle.FAILED);
        } finally {
            setFlag(false);//不要忘记这里，任务结束，修正isRunning状态
            pieceThreadResult = parsePieceThreadResult(futureList);//解析分块下载执行的结果
            DownloadInfo.modifyMsg(info, pieceThreadResult);//下载结果写入DownloadInfo
        }
        return pieceThreadResult;
    }

    /**
     * 周期性的同步信息磁盘和更新下载进度
     */
    private void timerUpdate() {
        if (recordHandler == null) {
            recordHandler = new Timer();
        }
        recordHandler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!exit)
                    calcSpeedAndSave();
                else {
                    cancel();
                    recordHandler.cancel();
                }
            }
        }, 1000L, updateViewInterval);
    }

    /**
     * 计算速度以及将信息同步到磁盘
     * 与上次时间间隔前相比，这个时间间隔下载了多少
     * 并更新到数据库
     */
    public void calcSpeedAndSave() {
        long currentSize = 0L;
        for (PieceInfo pieceInfo : info.getPiecesList()) {
            currentSize += pieceInfo.getCurBytes();
        }
        long deltaSize = currentSize - lastSize;
//        long deltaTime = System.currentTimeMillis() - lastTime;
        //同步存储
        syncInfo(Repo.SyncAction.UPDATE);
        long speed = deltaSize / updateViewInterval * 1000; // bytes/s
        //lastTime = System.currentTimeMillis();
        logger.info("速度（bytes/s）：  "+speed+"\n");
        lastSize = currentSize;
        info.setSpeed(speed);
        if (viewSources != null)
            viewSources.notifyViewsChanged(info, Repo.SyncAction.UPDATE, getLifecycleCollection());
    }


    /**
     * 根据所有分块线程下载的结果，返回下载任务的结论
     *
     * @return 根据当前任务所处生命周期，返回任务结果
     */
    private TaskResponse parsePieceThreadResult(List<Future<PieceResult>> futureList) {
        switch (getLifecycleCollection().getNowState()) {//这里只处理了lifecycle但没有校验分块的结果，还要改吗？
            case STOP:
                return genTaskResponse(STATUS_STOPPED, "download task paused", PAUSED);
            case CANCEL:
                return genTaskResponse(STATUS_CANCELLED, "task has been dropped", CANCELED);
        }

        setLifecycleState(TaskLifecycle.FAILED);//预先猜测结果，校验后修正
        if (info.getFinalCode() == HTTP_UNAVAILABLE) {
            return genTaskResponse(STATUS_WAITING_TO_RETRY, "waiting network to retry download", FAILED);
        }
        if (futureList == null || futureList.isEmpty()) {
            return genTaskResponse(STATUS_ERROR, "something wrong", FAILED);
        }
        if (futureList.size() != info.getThreadCounts())
            return genTaskResponse(STATUS_ERROR, "some piece download failed", FAILED);

        //没有取消或停止下载，最终任务执行结束的结果，成功或出错
        for (Future<PieceResult> pieceResultFuture : futureList) {
            try {//遍历分块结果，拿到最终结果
                int code = pieceResultFuture.get().getFinalCode();
                if (!isStatusCompleted(code)) {
                    return genTaskResponse(STATUS_ERROR, "something wrong", FAILED);
                } else {
                    setLifecycleState(TaskLifecycle.SUCCESS);
                    return genTaskResponse(STATUS_SUCCESS, "Download Completed", DOWNLOAD_COMPLETE);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return genTaskResponse(STATUS_ERROR, "something wrong", FAILED);
    }

    @Override
    public void requestCancel() {
        setFlag(false);
        setLifecycleState(TaskLifecycle.CANCEL);
    }

    @Override
    public void requestStop() {
        setFlag(false);
        setLifecycleState(TaskLifecycle.STOP);
    }

    @Override
    public void requestResume() {
        setFlag(true);
        setLifecycleState(TaskLifecycle.RESTART);
    }

    /**
     * @param canRunning 是否可以运行
     *                   false：不可另task继续运行，所以，设置停止标志并终止线程池
     *                   true：继续运行，进设置允许下载标志
     */
    private void setFlag(boolean canRunning) {
        for (PieceThread thread : pieceThreads) {
            thread.isRunning = canRunning;
        }
        if (!canRunning) {
            if (exec != null)
                exec.shutdown();
        }
    }


    @Override
    public DownloadInfo getInfo() {
        return this.info;
    }


    private Pair<FakeFile, TaskResponse> checkDiskAndInitFile() {
        boolean isExist = fs.isExist(info.getPath(), FileKit.FileKind.file);
        if (!isExist) {//文件不存在，检查磁盘空间，并创建文件
            String path = info.getPath();
            boolean enoughSpace = fs.checkSpace(path, info.getTotalBytes());
            if (!enoughSpace)
                return new Pair<>(null, genTaskResponse(STATUS_ERROR, "CAN NOT INIT FILE!", ERROR));
            FakeFile file = fs.create(info.getPath(), true);
            if (file == null)
                return new Pair<>(null, genTaskResponse(STATUS_ERROR, "CAN NOT CREATE FILE!", ERROR));
            return new Pair<>(file, null);
        } else {
            return new Pair<>(fs.find(info.getPath()), null);
        }
    }

    /**
     * 线程之行结束的清理
     */
    private void closeThings() {
        exit = true;
        if (pieceThreads != null) {
            pieceThreads.clear();
        }
        viewSources = null;
        recordHandler.cancel();
    }

    /**
     * 在承载结果信息之外，还把结果写入downloadInfo
     *
     * @param finalCode {@link StatusCode}
     */
    private TaskResponse genTaskResponse(int finalCode, String msg, TaskResult.TaskResultCode taskResultCode) {
        info.setFinalCode(finalCode);
        info.setFinalMsg(msg);
        return new TaskResponse(finalCode, msg, taskResultCode);
    }

    //更新info信息
    public void syncInfo(Repo.SyncAction action) {
        if (repo != null)
            repo.syncInfoToDisk(info, action);
        else {
            logger.info("存储库不可用");
        }
    }

}
