package com.kiylx.download_module.lib_core.engine;

import com.kiylx.download_module.fileio.FakeFile;
import com.kiylx.download_module.fileio.filesystem.FileKit;
import com.kiylx.download_module.lib_core.interfaces.DownloadTask;
import com.kiylx.download_module.lib_core.interfaces.PieceThread;
import com.kiylx.download_module.lib_core.interfaces.Repo;
import com.kiylx.download_module.lib_core.model.*;
import com.kiylx.download_module.lib_core.repository.TaskDataReceive;
import com.kiylx.download_module.utils.Utils;
import com.kiylx.download_module.utils.java_log_pack.Log;
import io.reactivex.annotations.NonNull;
import kotlin.Pair;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.kiylx.download_module.ContextKt.getContext;
import static com.kiylx.download_module.lib_core.model.StatusCode.*;
import static com.kiylx.download_module.lib_core.model.TaskResult.TaskResultCode.*;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;


public class DownloadTaskImpl extends DownloadTask {
    DownloadInfo info;
    private List<PieceThreadImpl> pieceThreads = Collections.emptyList();
    private ExecutorService exec;
    private FileKit fs;
    private FakeFile fakeFile=null;
    private final Repo repo = getContext().getRepo();
    public final TaskCallback callback = new TaskCallback() {
        @Override
        public void update(PieceInfo pieceInfo, boolean isRunning) throws NullPointerException {
            if (repo == null)
                throw new NullPointerException("repo is null");
            int blockId = pieceInfo.getBlockId();
            info.getSplitStart()[blockId] = pieceInfo.getStart();
            info.getSplitEnd()[blockId] = pieceInfo.getEnd();
            info.setRunning(isRunning);
            syncInfo(Repo.SyncAction.UPDATE);
            syncPieceInfo(pieceInfo, Repo.SyncAction.UPDATE);
            repo.updateInfoUI(info);
        }

        @Override
        public FakeFile getFile() {
           //return fs.get(info.getPath() + info.getFileName());
            if (fakeFile==null)
                fakeFile= Objects.requireNonNull(checkDiskAndInitFile()).getFirst();
           return fakeFile;
        }

        @Override
        public DownloadInfo getDownloadInfo() {
            return info;
        }
    };


    public DownloadTaskImpl(@NonNull DownloadInfo info) {
        super();
        this.info = info;
        lifecycle.initState(TaskLifecycle.OH, TaskLifecycle.OH);
        fs=getContext().getFileKit();
    }

    public static DownloadTask instance(DownloadInfo info) {
        return new DownloadTaskImpl(info);
    }

    @Override
    protected DownloadTask initTask() {
        lifecycle.setLifecycleState(TaskLifecycle.CREATE);
        return this;
    }

    /**
     * 清理下载任务
     * 包括出错后重新尝试下载的任务、从磁盘恢复的任务
     *
     * @return
     */
    private TaskResult cleanTask() {
        if (info.getStatusCode() == STATUS_SUCCESS || info.getStatusCode() == STATUS_CANCELLED) {
            return new TaskResult(info.getUuid(), TaskResult.TaskResultCode.DOWNLOAD_COMPLETE,
                    "Download was success or canceled",
                    STATUS_SUCCESS
            );//文件下载成功或取消
        }
        info.setStatusCode(STATUS_INIT);
        info.setStatusMsg("");

        return null;
    }

    @Override
    public TaskResult call() {
        initTask();
        TaskResult cleanTaskResult = cleanTask();
        if (cleanTaskResult != null)
            return cleanTaskResult;
        try {
            VerifyResult verifyResult = execPieceDownload();//执行分块下载
           //todo 应该在execPieceDownload()方法里写入结果   DownloadInfo.modifyMsg(info, verifyResult);//下载结果写入DownloadInfo
            return new TaskResult(info.getUuid(), verifyResult);
        } catch (Throwable t) {
            Log.e(Log.getStackTraceString(t));
            if (info != null) {
                DownloadInfo.modifyMsg(info, STATUS_UNKNOWN_ERROR, t.getMessage());//下载结果写入DownloadInfo
            }
        } finally {
            lifecycle.setLifecycleState(TaskLifecycle.STOP);
            syncInfo(Repo.SyncAction.UPDATE);//结果同步到磁盘
            closeThings();
        }
        return new TaskResult(info.getUuid(), TaskResult.TaskResultCode.ERROR, info.getStatusMsg(), info.getStatusCode());
    }

    /**
     * 执行分块下载
     *
     * @return 返回分块下载结果
     */
    private VerifyResult execPieceDownload() {
    VerifyResult verifyResult=null;
        List<Future<PieceResult>> futureList = Collections.emptyList();//存储分块任务（callable）的返回结果（future）
        lifecycle.setLifecycleState(TaskLifecycle.RUNNING);

        boolean shouldQueryDb = (isRecoveryFromDisk() || info.getRetryCount() > 0);//旧任务或者尝试重新下载的任务
        VerifyResult metaResult = TaskDataReceive.fetchMetaData(info, shouldQueryDb);//验证连接有效性
        if (metaResult != null)
            return metaResult;
        if (info.getTotalBytes() == 0) {
            return generateVerifyResult(STATUS_SUCCESS, "Length is zero; skipping", DOWNLOAD_COMPLETE);
        }
        //验证网络连接
        if (!Utils.checkConnectivity()) {
            return generateVerifyResult(STATUS_WAITING_FOR_NETWORK, "LOST CONNECTING", FAILED);
        }
        try {//创建分块执行下载
           Pair<FakeFile, VerifyResult> fileVerifyResultPair = checkDiskAndInitFile();//创建文件
            if (fileVerifyResultPair.getSecond() != null) {
                return fileVerifyResultPair.getSecond();
            }
            fakeFile=fileVerifyResultPair.getFirst();//赋予文件
            exec = (info.getThreadCounts() == 1 || !info.isPartialSupport()) ?
                    Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(info.getThreadCounts());
            pieceThreads = PieceThreadImpl.generatePieceList(repo, info, callback, shouldQueryDb);
            info.reduceRetryCount();
            setRecoveryFromDisk(false);
            //Wait all threads
            futureList = exec.invokeAll(pieceThreads);

        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + "被中断, 中断标识: " + Thread.currentThread().isInterrupted());
            // 中断后停止当前线程
            Thread.interrupted();

            verifyResult=parseResult(futureList);
            return verifyResult;
        }finally{
            if(verifyResult!=null)
                    DownloadInfo.modifyMsg(info, verifyResult);//下载结果写入DownloadInfo
        }
           verifyResult=parseResult(futureList);
            return verifyResult;
    }


    /**
     * @return 根据当前任务所处生命周期，返回任务结果
     */
    private VerifyResult parseResult(List<Future<PieceResult>> futureList) {
        if (info.getStatusCode() == HTTP_UNAVAILABLE) {
            return generateVerifyResult(STATUS_WAITING_TO_RETRY, "waiting network to retry download", FAILED);
        }
        if (futureList.size() != info.getThreadCounts())
            return generateVerifyResult(STATUS_ERROR, "some piece download failed", FAILED);
        switch (lifecycle.getNowState()) {//这里只处理了lifecycle但没有校验分块的结果，还要改吗？
            case STOP:
                return generateVerifyResult(STATUS_STOPPED, "download task paused", PAUSED);
            case DROP:
                return generateVerifyResult(STATUS_CANCELLED, "task has been dropped", CANCELED);
        }
        //没有取消或停止下载，最终任务执行结束的结果，成功或出错
        for (Future<PieceResult> pieceResultFuture : futureList) {
            try {//遍历分块结果，拿到最终结果
                int code = pieceResultFuture.get().getFinalCode();
                if (!isStatusCompleted(code)) {
                    return generateVerifyResult(STATUS_ERROR, "something wrong", FAILED);
                }else{
                return generateVerifyResult(STATUS_SUCCESS, "Download Completed", DOWNLOAD_COMPLETE);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return generateVerifyResult(STATUS_ERROR, "something wrong", FAILED);
    }

    @Override
    public void requestCancel() {
        lifecycle.setLifecycleState(TaskLifecycle.DROP);
        setFlag(false);
    }

    @Override
    public void requestStop() {
        lifecycle.setLifecycleState(TaskLifecycle.STOP);
        setFlag(false);
    }

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
    public void requestResume() {
        lifecycle.setLifecycleState(TaskLifecycle.RESTART);
        setFlag(true);
    }

    @Override
    public DownloadInfo getInfo() {
        return this.info;
    }


    private Pair<FakeFile, VerifyResult> checkDiskAndInitFile() {
        boolean isExist = fs.isExist(info.getPath(), FileKit.FileKind.file);
        if (!isExist) {//文件不存在，检查磁盘空间，并创建文件
            String path = info.getPath();
            boolean enoughSpace = fs.checkSpace(path,info.getTotalBytes());
            if (!enoughSpace)
                return new Pair<>(null, generateVerifyResult(STATUS_ERROR, "CAN NOT INIT FILE!", ERROR));
           FakeFile file= fs.create(info.getPath(),true);
            return new Pair<>(file,null);
        }
        return null;
    }

    /**
     * 线程之行结束的清理
     */
    private void closeThings() {
        if (pieceThreads != null) {
            pieceThreads.clear();
        }
    }

    /**
     * 生成verifyResult同时，把结果写入downloadInfo
     */
    private VerifyResult generateVerifyResult(int finalCode, String msg, TaskResult.TaskResultCode taskResultCode) {
        info.setStatusCode(finalCode);
        info.setStatusMsg(msg);
        return new VerifyResult(finalCode, msg, taskResultCode);
    }

    public void syncInfo(Repo.SyncAction action) {
        if (repo != null)
            repo.syncInfoToDisk(info, action);
    }

    private void syncPieceInfo(PieceInfo info, Repo.SyncAction action) {
        if (repo != null) {
            repo.syncPieceInfoToDisk(info, action);
        }
    }

}
