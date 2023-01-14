package com.kiylx.download_module.interfaces;

import okhttp3.Response;

import java.io.IOException;

public interface ConnectionListener {
    void onResponseHandle(Response response, int code, String message);

    /**
     * 发生了重定向
     *
     * @param httpCode 301 302 303
     * @param newUrl   location指定的新地址
     */
    void onMovedPermanently(int httpCode, String newUrl);

    void onIOException(IOException e);

    void onTooManyRedirects();
}
