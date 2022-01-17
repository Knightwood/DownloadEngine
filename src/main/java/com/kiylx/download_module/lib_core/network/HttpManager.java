package com.kiylx.download_module.lib_core.network;

import com.kiylx.download_module.lib_core.interfaces.ConnectionListener;
import com.kiylx.download_module.utils.ssl.SSLSocketClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;
import okhttp3.*;

import static java.net.HttpURLConnection.*;

public class HttpManager {
    private final OkHttpClient mOkHttpClient;
    public static final int DEFAULT_TIMEOUT = 20000; //20s
    private ConcurrentHashMap<String, List<Cookie>> cookiesStore = new ConcurrentHashMap<>();
    /* Can't be more than 7 */
    private static final int MAX_REDIRECTS = 5;

    public static class CustomHeaders {
        public static final String isPiece = "X-piece";//value: "1"是分块请求；"0"非分块的请求
    }


    private HttpManager(CookieJar cookieJar) {
        mOkHttpClient = new OkHttpClient();
        if (cookieJar == null) {
            generateClient(new CookieJar() {
                @Override
                public void saveFromResponse(@NonNull HttpUrl httpUrl, @NonNull List<Cookie> list) {
                    cookiesStore.put(httpUrl.host(), list);
                }

                @NonNull
                @Override
                public List<Cookie> loadForRequest(@NonNull HttpUrl httpUrl) {
                    List<Cookie> cookies = cookiesStore.get(httpUrl.host());
                    return cookies != null ? cookies : new ArrayList<>();
                }
            });
        } else {
            generateClient(cookieJar);
        }
    }

    public static HttpManager getInstance(CookieJar cookieJar) {
        return Singleton.INSTANCE.getManager(cookieJar);
    }

    enum Singleton {
        INSTANCE;
        private HttpManager manager=null;

        public HttpManager getManager(CookieJar cookieJar) {
            if (manager==null)
                manager=new HttpManager(cookieJar);
            return manager;
        }
    }

    public static HttpManager getInstance() {
        return getInstance(null);
    }

    public OkHttpClient client() {
        return mOkHttpClient;
    }

    /**
     * @return 返回response
     */
    public Response getResponse(Request request, ConnectionListener listener) {
        Response result;
        try (Response response = mOkHttpClient.newCall(request).execute()) {
            int responseCode = response.code();
            switch (responseCode) {
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                    String location = response.header("Location");
                    if (listener != null)
                        listener.onMovedPermanently(responseCode,location);
                    break;
                default:
                    if (listener != null)
                        listener.onResponseHandle(response, responseCode, response.message());
                    return response;
            }
            result=response;
        } catch (IOException e) {
            if (listener != null)
                listener.onIOException(e);
            return null;

        }
        if (listener != null)
            listener.onTooManyRedirects();
        return result;
    }
    public Response getResponse(Request request){
        try {
            return mOkHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private OkHttpClient generateClient(CookieJar cookieJar) {
        mOkHttpClient.newBuilder()
                .followRedirects(false)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .cookieJar(cookieJar)
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())//配置
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                .build();
        return mOkHttpClient;
    }
}
