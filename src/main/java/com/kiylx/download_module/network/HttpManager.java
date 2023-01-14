package com.kiylx.download_module.network;

import com.kiylx.download_module.interfaces.ConnectionListener;
import com.kiylx.download_module.utils.ssl.SSLSocketClient;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.net.HttpURLConnection.*;

public class HttpManager {
    private OkHttpClient mOkHttpClient = null;
    public static final int DEFAULT_TIMEOUT = 20000; //20s
    private final ConcurrentHashMap<String, List<Cookie>> cookiesStore = new ConcurrentHashMap<>();
    /* Can't be more than 7 */
    private static final int MAX_REDIRECTS = 5;

    public static class CustomHeaders {
        public static final String isPiece = "X-piece";//value: "1"是分块请求；"0"非分块的请求
    }

    private HttpManager() {
        generateClient(new CookieJar() {
            @Override
            public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                cookiesStore.put(httpUrl.host(), list);
            }

            @NotNull
            @Override
            public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                List<Cookie> cookies = cookiesStore.get(httpUrl.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        });
    }

    public static HttpManager getInstance() {
        return Singleton.INSTANCE.getManager();
    }

    enum Singleton {
        INSTANCE;
        private HttpManager manager = null;

        public HttpManager getManager() {
            if (manager == null)
                manager = new HttpManager();
            return manager;
        }
    }

    /**
     */
    public void getResponse(Request request,@NotNull ConnectionListener listener) {
        System.out.println("httpManager的getResponse方法");
        try (Response response = mOkHttpClient.newCall(request).execute()) {
            int responseCode = response.code();
            switch (responseCode) {
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                    String location = response.header("Location");
                    listener.onMovedPermanently(responseCode, location);
                    break;
                default:
                    listener.onResponseHandle(response, responseCode, response.message());
                    return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            listener.onIOException(e);
            return;
        }
        listener.onTooManyRedirects();
    }
    public Response getResponse(Request request){
        System.out.println("httpManager的getResponse方法");
        Response response = null;
        try {
            response = mOkHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};
    X509TrustManager trustAllCert = (X509TrustManager) trustAllCerts[0];

    private void generateClient(CookieJar cookieJar) {
       /*LoggingInterceptor loggingInterceptor= new LoggingInterceptor.Builder()
                .setLevel(Level.BASIC)
                .log(VERBOSE)
               .build();.addInterceptor(loggingInterceptor)*/
        mOkHttpClient = new OkHttpClient.Builder()
                .followRedirects(false)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .cookieJar(cookieJar)
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(),trustAllCert)//配置
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                .build();
    }
}
