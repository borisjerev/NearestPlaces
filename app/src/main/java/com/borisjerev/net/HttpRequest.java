package com.borisjerev.net;

/**
 * Created by user on 1/07/2016.
 */
public class HttpRequest {
    private String url;
    private long timestampStart;
    private long timestampEnd;
    private Object result;
    private boolean error;

    public HttpRequest(String url) {
        this.url = url;
        this.timestampStart = System.currentTimeMillis();
    }

    public void onHttpRequestFinished(Object result, boolean error) {
        this.timestampEnd = System.currentTimeMillis();
        this.result = result;
        this.error = error;
    }

    public long getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(long timestampStart) {
        this.timestampStart = timestampStart;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }
}
