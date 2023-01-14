package com.kiylx.download_module.model;

/*
 * 承载结果的通用类
 */
@Deprecated
public class BasicConclusion {
    private static final int init_value = StatusCode.STATUS_INIT;
    private String message;
    private final int finalCode;
    private Object data;
    private Throwable throwable;


    public BasicConclusion(int finalCode, Throwable throwable) {
        this(finalCode);
        this.throwable = throwable;
    }

    public BasicConclusion() {
        this(init_value);
    }

    public BasicConclusion(int finalCode) {
        this.finalCode = finalCode;
    }

    public BasicConclusion(int finalCode, String message) {
        this(finalCode, message, null);
    }

    public <T> BasicConclusion(int finalCode, String message, T data) {
        this(finalCode, message, data, null);
    }

    public <T> BasicConclusion(int finalCode, String message, T data, Throwable throwable) {
        this.message = message;
        this.finalCode = finalCode;
        this.data = data;
        this.throwable = throwable;
    }

    public int getFinalCode() {
        return finalCode;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return throwable;
    }


    @Override
    public String toString() {
        return "StopRequest{" +
                "message='" + message + '\'' +
                ", finalStatus=" + finalCode +
                ", t=" + throwable +
                '}';
    }

    public <T> T getData(Class<T> tClass) {
        return tClass.cast(data);
    }

    public <T> void setData(T data) {
        this.data = data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public static final int init_value = StatusCode.STATUS_INIT;
        private String message;
        private int finalCode = init_value;
        private Object data;

        public Builder finalCode(int code) {
            this.finalCode = code;
            return this;
        }

        public Builder message(String msg) {
            this.message = msg;
            return this;
        }

        public <T> Builder data(T obj) {
            this.data=obj;
            return this;
        }

        public BasicConclusion build() {
            return new BasicConclusion(finalCode,message,data);
        }
    }
}
