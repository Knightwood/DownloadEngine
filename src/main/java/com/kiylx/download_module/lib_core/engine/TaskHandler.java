package com.kiylx.download_module.lib_core.engine;

import com.kiylx.download_module.DownloadsListKind;
import com.kiylx.download_module.lib_core.interfaces.*;
import com.kiylx.download_module.utils.DigestUtils;
import com.kiylx.download_module.utils.java_log_pack.Log;
import com.kiylx.download_module.lib_core.data_struct.DownloadMap;
import com.kiylx.download_module.lib_core.data_struct.WaitingDownloadQueue;
import com.kiylx.download_module.lib_core.model.DownloadInfo;
import com.kiylx.download_module.lib_core.model.StatusCode;
import com.kiylx.download_module.lib_core.model.TaskResult;
import com.kiylx.download_module.lib_core.model.TaskLifecycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.*;

import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.view.ViewsAction;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.kiylx.download_module.DownloadsListKind.*;

public class TaskHandler {
    private int downloadLimit;
    private final DownloadMap active = new DownloadMap();
    private final WaitingDownloadQueue wait = new WaitingDownloadQueue();
    private final DownloadMap finish = new DownloadMap();
    private final CompositeDisposable disposables = new CompositeDisposable();

    enum SingletonEnum {
        SINGLETON;
        private TaskHandler instance = null;

        public TaskHandler getInstance() {
            if (instance == null)
                instance = new TaskHandler();
            return instance;
        }
    }

    public static TaskHandler getInstance() {
        return SingletonEnum.SINGLETON.getInstance();
    }

    private TaskHandler() {
    }

    public TasksCollection getDownloadTaskList(int kind) {
        switch (kind) {
            case DownloadsListKind.wait_kind:
                return wait;
            case DownloadsListKind.active_kind:
                return active;
            case DownloadsListKind.finish_kind:
                return finish;
        }
        return null;
    }

    public List<SimpleDownloadInfo> getAllSimpleDownloadsInfo() {
        List<SimpleDownloadInfo> list = new ArrayList<>();
        list.addAll(wait.covert(ViewsAction.generate));
        list.addAll(wait.frozenTask.covert(ViewsAction.generate));
        list.addAll(active.covert(ViewsAction.generate));
        list.addAll(finish.covert(ViewsAction.generate));
        return list;
    }

    /**
     * 添加下载任务
     *
     * @param task download task
     * @return 顺利放入active, 返回true ；
     * 任务需要继续等待，放入wait并返回false ；
     */
    public boolean addDownloadTask(DownloadTask task) {
        if (isMaxActiveDownloads()) {
            System.out.println("队列满了，等待下载");
            wait.add(task);
            task.setLifecycleState(TaskLifecycle.START);
            return false;
        } else {
            System.out.println("队列没满，执行下载");
            active.add(task);
            runTask(task);
            return true;
        }
    }

    /**
     * 重试,任务在finish中移到wait重新下载
     *
     * @param id uuid
     */
    public void reTry(UUID id) {
        DownloadTask task = finish.findTask(id);
        reTry(task);
    }

    private void reTry(DownloadTask task) {
        task.setRecoveryFromDisk(true);
        task.getInfo().plusRetryCount();
        finish.remove(task);
        addDownloadTask(task);
    }

    /**
     * @param id       uuid
     * @param taskKind DownloadsListKind中定义,DownloadTask所属的队列
     */
    public void requestPauseTask(UUID id, int taskKind) {
        DownloadTask task;
        switch (taskKind) {
            case active_kind:
                task = active.findTask(id);
                if (task != null)
                    task.requestStop();
                break;
            case wait_kind:
                wait.frozenTask.frozenTask(id);
                break;
        }
    }

    /**
     * 界面的刷新滞后于下载任务实际完成的时间
     * 暂停下载任务
     */
    public void requestPauseTask(UUID id) {
        DownloadTask task;
        task = active.findTask(id);
        if (task != null) {
            task.requestStop();
        } else {
            task = wait.findTask(id);
            if (task != null) {
                wait.frozenTask.frozenTask(task);
            }
        }
    }

    /**
     * 界面的刷新滞后于下载任务实际完成的时间
     * 取消下载任务
     */
    public void requestCancelTask(UUID id) {
        DownloadTask task;
        task = active.findTask(id);
        if (task != null) {
            task.requestCancel();
            active.remove(task);
        } else {
            task = wait.findTask(id);
            if (task != null) {
                task.requestCancel();
                wait.remove(task);
            }
        }
    }

    /**
     * 恢复下载任务
     *
     * @param id downloadInfo's uuid
     */
    public void resumeTask(UUID id) {
        DownloadTask task;
        task = wait.frozenTask.findTaskInFrozen(id);
        if (task != null) {
            wait.frozenTask.removeFrozenTask(id);
            task.requestResume();
            addDownloadTask(task);
        }
    }

    /**
     * 运行下载任务
     *
     * @param task downloadTask
     */
    private void runTask(DownloadTask task) {//如果这里开启线程去执行下载，那么前面就能拿到info.而不用等待下载过程带来的阻塞
       /* FutureTask<TaskResult> taskFuture = new FutureTask<TaskResult>(task);
        new Thread(taskFuture).start();
        try {
            TaskHandler.this.finallyTaskFinish(task, taskFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        try {
            task.call();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /*CompletableFuture<TaskResult> future = CompletableFuture.supplyAsync((Supplier<TaskResult>) () -> {
                    try {
                        return (TaskResult) task.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }).whenComplete(new BiConsumer<TaskResult, Throwable>() {
                    @Override
                    public void accept(TaskResult taskResult, Throwable throwable) {
                        System.out.println("下载任务执行结束: " + (taskResult == null));
                        TaskHandler.this.finallyTaskFinish(task, taskResult);
                    }
                })
                .exceptionally(new Function<Throwable, TaskResult>() {
                    @Override
                    public TaskResult apply(Throwable throwable) {
                        Log.e("Getting info " + task.getInfo().getUUIDString() + " error: " +
                                throwable.getMessage());
                        return null;
                    }
                });*/
        disposables.add(Observable.fromCallable(task)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<TaskResult>() {
                               @Override
                               public void accept(TaskResult taskResult) throws Exception {
                                   System.out.println("下载任务执行结束");
                                   TaskHandler.this.finallyTaskFinish(task, taskResult);
                               }
                           }
                )
        );
    }

    private boolean checkNoDownloads() {
        return active.isEmpty();
    }

    boolean isMaxActiveDownloads() {
        return active.size() == downloadLimit;
    }

    public void close() {
        disposables.clear();
    }

    public void setLimit(int downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    /**
     * @param downloadTask null: 查询wait,将等待下载的任务执行下载并加入active;
     *                     传入task： 运行task
     */
    private void scheduleDownload(DownloadTask downloadTask) {
        if (isMaxActiveDownloads())
            return;
        DownloadTask task = null;
        if (downloadTask != null) {
            task = downloadTask;
        } else {
            task = wait.poll();
        }
        if (task == null)
            return;
        active.add(task);
        runTask(task);
    }

    /**
     * @param task        downloadTask
     * @param oldTaskKind task原来的位置
     * @param nowTaskKind 将此task移动到这里
     */
    private void moveTask(DownloadTask task, int oldTaskKind, int nowTaskKind) {
        removeTask(oldTaskKind, task);
        switch (nowTaskKind) {
            case wait_kind:
                wait.add(task);
                break;
            case finish_kind:
                finish.add(task);
                break;
            case active_kind:
                active.add(task);
                break;
            case frozen_kind:
                wait.frozenTask.addTaskToFrozen(task);
                break;
        }
    }

    private void removeTask(int taskKind, DownloadTask task) {
        switch (taskKind) {
            case wait_kind:
                wait.remove(task);
                break;
            case finish_kind:
                finish.remove(task);
                break;
            case active_kind:
                active.remove(task);
                break;
            case frozen_kind:
                wait.frozenTask.removeFrozenTask(task);
                break;
        }
    }


    /**
     * @param task   download task
     * @param result 任务的执行结果
     */
    private void finallyTaskFinish(DownloadTask task, TaskResult result) {
        if (result == null)
            return;
        switch (result.taskResultCode) {
            case DOWNLOAD_COMPLETE:
                onCompleted(task, result);
            case CANCELED:
            case FAILED:
            case ERROR:
                moveTask(task, active_kind, finish_kind);
                break;
            case PAUSED:
                moveTask(task, active_kind, frozen_kind);
                break;
        }
        handleInfoStatus(task);
        scheduleDownload(null);
    }


    /**
     * @param task 任务
     */
    private void handleInfoStatus(DownloadTask task) {
        DownloadInfo info = task.getInfo();
        if (info == null)
            return;
        switch (info.getFinalCode()) {
            case StatusCode.STATUS_SUCCESS:
                checkMoveAfterDownload(task.getInfo());
                break;
            case StatusCode.STATUS_CANCELLED:
                //TODO ： 下载完成或取消处理
                break;
            case StatusCode.STATUS_WAITING_TO_RETRY:
            case StatusCode.STATUS_WAITING_FOR_NETWORK:
                reTry(task);
                //TODO ：重试
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                /* TODO: request authorization from user */
                break;
            case HttpURLConnection.HTTP_PROXY_AUTH:
                /* TODO: proxy support */
                break;
        }
    }

    /**
     * 下载完成后移动文件
     *
     * @param info download info
     */
    private void checkMoveAfterDownload(DownloadInfo info) {
        //todo 移动文件
    }

    /**
     * 下载完成，处理一些情况
     */
    private void onCompleted(DownloadTask task, TaskResult result) {
        DownloadInfo info = task.getInfo();
        if (info != null) {
            handleInfoStatus(task);
            boolean b = verifyChecksum(task);//todo 校验完成需要通知
        }
    }

    /**
     * 验证完整性
     */
    private boolean verifyChecksum(DownloadTask task) {
        DownloadInfo info = task.getInfo();
        String hash = info.getCheckSum();
        if (hash != null && !hash.isEmpty()) {
            //todo 校验文件 sha256 md5
            try {
                if (DigestUtils.isMd5Hash(info.getCheckSum())) {
                    hash = calcHashSum(info, false);

                } else if (DigestUtils.isSha256Hash(info.getCheckSum())) {
                    hash = calcHashSum(info, true);

                } else {
                    throw new IllegalArgumentException("Unknown checksum type:" + info.getCheckSum());
                }

            } catch (IOException e) {
                return false;
            }

            return (hash != null && hash.equalsIgnoreCase(info.getCheckSum()));
        }
        return false;
    }

    private String calcHashSum(DownloadInfo info, boolean sha256Hash) throws IOException {
        File file = new File(info.getPath() + info.getFileName());
        if (!file.exists())
            return null;
        try (FileInputStream is = new FileInputStream(file)) {
            return (sha256Hash ? DigestUtils.makeSha256Hash(is) : DigestUtils.makeMd5Hash(is));
        }

    }

    public void views() {

    }
}
