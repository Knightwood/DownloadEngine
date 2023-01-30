package com.kiylx.download_module.model;

public class StatusCode {
    /*
     * StatusCode描述downloadInfo的一些状态。
     * TaskResult和TaskState表述task的状态。
     * Lists the states that the download task can set on a download
     * to notify application of the download progress.
     * Status codes are based on DownloadManager system.
     * 100-200 informational
     * 3xx: redirects (not used)
     * 4xx: client errors
     * 5xx: server errors
     */


    /*
     * 此下载成功完成
     * Use isStatusSuccess() to capture the entire category
     * 用isStatusSuccess()捕获整个类别
     */
    public static final int STATUS_SUCCESS = 1;

    public static final int STATUS_INIT = 101;//代表初始化的状态
    public static final int STATUS_RUNNING = 102;//下载开始
    public static final int STATUS_ACTIVE = STATUS_RUNNING;//正在下载
    public static final int STATUS_STOPPED = 103;//此下载已停止
    public static final int STATUS_CANCELLED = 104;//此下载已取消
    public static final int STATUS_WAITING = 110;//等待下载
    public static final int STATUS_WAITING_TO_RETRY = 111;//等待重试
    public static final int STATUS_WAITING_FOR_NETWORK = 112;//正在等待网络连接继续进行
    /**
     * 再请求一次。
     * 例如html的下载，需要referer，但第一次请求时没有带referer，那么就需要带上referer再请求一次
     */
    public static final int STATUS_RETRY_REQUEST = 113;

    /*
     * This request couldn't be parsed. This is also used when processing
     * requests with unknown/unsupported URI schemes
     */
    public static final int STATUS_BAD_REQUEST = 400;
    /* Some possibly transient error occurred, but we can't resume the download */
    public static final int STATUS_CANNOT_RESUME = 489;
    /* The file hash is different from the specified hash of the download */
    public static final int STATUS_CHECKSUM_ERROR = 490;
    /*
     * This download couldn't be completed because of an HTTP
     * redirect response that the download manager couldn't
     * handle
     * 由于下载管理器无法处理的 HTTP 重定向响应，无法完成此下载
     */
    public static final int STATUS_UNHANDLED_REDIRECT = 493;
    /*
     * This download couldn't be completed because of an
     * unspecified unhandled HTTP code.
     * 由于未指定的未处理 HTTP 代码，无法完成此下载。
     */
    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;
    /*
     * This download couldn't be completed because of an
     * error receiving or processing data at the HTTP level
     */
    public static final int STATUS_HTTP_DATA_ERROR = 495;
    /*
     * This download couldn't be completed because there were
     * too many redirects.
     */
    public static final int STATUS_TOO_MANY_REDIRECTS = 497;
    /*
     * This download couldn't be completed due to insufficient storage
     * space. Typically, this is because the SD card is full
     */
    public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 498;

    //600-700 表示各种错误
    public static final int STATUS_ERROR = 601;
    /*
     * This download couldn't be completed because of a storage issue.
     * Typically, that's because the filesystem is missing or full
     */
    public static final int STATUS_FILE_ERROR = 602;
    public static final int STATUS_UNKNOWN_ERROR = 699;


    public static boolean isStopOrCanceled(int i) {
        return i == STATUS_CANCELLED || i == STATUS_STOPPED;
    }

    public static boolean hasError(int i) {
        return (i > 600 && i < 700);
    }

    /*
     * Returns whether the status is informational (i.e. 1xx)
     */

    public static boolean isStatusInformational(int statusCode) {
        return statusCode >= 100 && statusCode < 200;
    }

    /*
     * Returns whether the status is a success (i.e. 2xx)
     */

    public static boolean isStatusSuccess(int statusCode) {
        return statusCode >= 0 && statusCode < 100;
    }

    /*
     * Returns whether the status is a client error (i.e. 4xx)
     */

    public static boolean isStatusClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }

    /*
     * Returns whether the status is a server error (i.e. 5xx)
     */

    public static boolean isStatusServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    public static boolean isStatusCompleted(int statusCode) {
        return statusCode >= 1 && statusCode < 100 || statusCode >= 400 && statusCode < 600;
    }
}
