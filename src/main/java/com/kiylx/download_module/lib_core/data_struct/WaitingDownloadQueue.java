package com.kiylx.download_module.lib_core.data_struct;

import com.kiylx.download_module.lib_core.interfaces.FrozenCollection;
import com.kiylx.download_module.lib_core.interfaces.TasksCollection;
import com.kiylx.download_module.model.TaskResult;
import com.kiylx.download_module.interfaces.DownloadTask;
import com.kiylx.download_module.view.SimpleDownloadInfo;
import com.kiylx.download_module.view.ViewsAction;

import java.util.*;

import static com.kiylx.download_module.view.SimpleDownloadInfoKt.genSimpleDownloadInfo;

public class WaitingDownloadQueue implements TasksCollection {
    private static final String TAG = WaitingDownloadQueue.class.getSimpleName();
    private final ArrayDeque<DownloadTask> waiting = new ArrayDeque<>();//等待调度器自动将任务放入下载队列
    public final FrozenTaskCollection frozenTask = new FrozenTaskCollection();
    public List<SimpleDownloadInfo> views = Collections.emptyList();

    //----------------------------------TasksCollection----------------------------------------------//

    /**
     * @return 检索但不删除此双端队列的第一个元素。
     */
    public DownloadTask getFirst() {
        DownloadTask t = null;
        while (t == null) {
            try {
                t = waiting.getFirst();
            } catch (NoSuchElementException e) {
                /* Queue is empty, return */
                return null;
            }
        }
        return t;
    }

    /**
     * @return 检索但不删除此双端队列的最后一个元素。
     */
    public DownloadTask getLast() {
        DownloadTask t = null;
        while (t == null) {
            try {
                t = waiting.getLast();
            } catch (NoSuchElementException e) {
                /* Queue is empty, return */
                return null;
            }
        }
        return t;
    }

    /**
     * @param id id
     * @return 检索但不删除此双端队列中的元素。
     */
    @Override
    public DownloadTask findTask(UUID id) {
        DownloadTask t = null;
        for (DownloadTask downloadTask : waiting) {
            t = downloadTask;
            if (t.getInfo().getUuid() == id)
                return t;
        }
        return t;
    }

    @Override
    public void add(DownloadTask task) {
        if (waiting.contains(task))
            return;
        waiting.add(task);
    }

    /**
     * 此方法没有实际作用
     */
    @Override
    @Deprecated
    public void remove(TaskResult taskResult) {
    }

    @Override
    public void remove(DownloadTask task) {
        waiting.remove(task);
    }

    @Override
    public DownloadTask insert(DownloadTask task) {
        boolean b = waiting.offerLast(task);
        return b ? task : null;
    }

    @Override
    public DownloadTask delete(UUID infoId) {
        DownloadTask task = findTask(infoId);
        if (task != null)
            waiting.remove(task);
        return task;
    }

    //添加一个元素至末尾并返回true  如果队列已满，则返回false
    public boolean offer(DownloadTask task) {
        return waiting.offer(task);
    }

    //移除并返问队列头部的元素  如果队列为空，则返回null
    public DownloadTask poll() {
        return waiting.poll();
    }

    // 返回队列头部的元素  如果队列为空，则返回null
    public DownloadTask peek() {
        return waiting.peek();
    }

    @Override
    public int size() {
        return waiting.size();
    }

    @Override
    public List<SimpleDownloadInfo> covert(int viewsAction) {
        switch (viewsAction) {
            case ViewsAction.generate:
                views = new ArrayList<>();
                waiting.forEach(downloadTask -> views.add(downloadTask.getInfo().getSimpleDownloadInfo()));
            case ViewsAction.update:
                views.clear();
                waiting.forEach(downloadTask -> views.add(downloadTask.getInfo().getSimpleDownloadInfo()));
                return views;
            case ViewsAction.pull:
            default:
                return views;
        }
    }

    @Override
    public boolean isEmpty() {
        return waiting.isEmpty();
    }

    public void moveToEnd(UUID infoId) {
        DownloadTask task = findTask(infoId);
        if (task != null) {
            waiting.remove(task);
            waiting.addLast(task);
        }
    }


    //----------------------------------DownloadQueue----------------------------------------------//

    /**
     * @param id uuid
     * @return 返回在等待下载队列或暂停下载队列中查到的task
     */
    public DownloadTask queryTask(UUID id) {
        DownloadTask task = null;
        task = findTask(id);
        if (task != null) {
            return task;
        }
        task = frozenTask.findTaskInFrozen(id);
        return task;
    }

    //----------------------------------FrozenCollection----------------------------------------------//
    public class FrozenTaskCollection implements FrozenCollection {
        private final List<DownloadTask> frozen = new ArrayList<>(); //等待手动放入下载队列
        public List<SimpleDownloadInfo> views = Collections.emptyList();

        //将任务放进list，使任务永远不会下载
        @Override
        public void frozenTask(UUID infoId) {
            DownloadTask task = delete(infoId);
            if (task != null) {
                frozen.add(task);
            }
        }

        @Override
        public void frozenTask(DownloadTask task) {
            if (task != null) {
                waiting.remove(task);
                frozen.add(task);
            }
        }

        //将任务放回queue，使任务重新等待下载
        @Override
        public void activeTask(UUID id) {
            DownloadTask t = null;
            for (DownloadTask task : frozen) {
                t = task;
                if (t.getInfo().getUuid() == id)
                    break;
            }
            if (t != null) {
                waiting.add(t);
            }
        }

        @Override
        public boolean removeFrozenTask(DownloadTask task) {
            if (task == null)
                return false;
            UUID uuid = task.getInfo().getUuid();
            return removeFrozenTask(uuid);
        }

        @Override
        public boolean removeFrozenTask(UUID uuid) {
            for (int i = frozen.size() - 1; i >= 0; i--) {
                if (frozen.get(i).getInfo().getUuid() == uuid) {
                    frozen.remove(i);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void addTaskToFrozen(DownloadTask task) {
            if (task != null) {
                frozen.add(task);
            }
        }

        @Override
        public DownloadTask findTaskInFrozen(UUID id) {
            int i = findTaskPosInFrozen(id);
            if (i < 0 || i > frozen.size())
                return null;
            return frozen.get(i);
        }

        @Override
        public List<SimpleDownloadInfo> covert(int viewsAction) {
            switch (viewsAction) {
                case ViewsAction.generate:
                    views = new ArrayList<>();
                    frozen.forEach(downloadTask -> views.add(genSimpleDownloadInfo(downloadTask.getInfo())));
                case ViewsAction.update:
                    views.clear();
                    frozen.forEach(downloadTask -> views.add(genSimpleDownloadInfo(downloadTask.getInfo())));
                    return views;
                case ViewsAction.pull:
                default:
                    return views;
            }
        }

        public int findTaskPosInFrozen(UUID id) {
            int result = -1;
            for (int i = 0; i < frozen.size(); i++) {
                if (frozen.get(i).getInfo().getUuid() == id) {
                    result = i;
                    break;
                }
            }
            return result;
        }

        public int findTaskPosInFrozen(DownloadTask task) {
            int result = -1;
            UUID uuid = task.getInfo().getUuid();
            for (int i = 0; i < frozen.size(); i++) {
                if (frozen.get(i).getInfo().getUuid() == uuid) {
                    result = i;
                    break;
                }
            }
            return result;
        }
    }
}
