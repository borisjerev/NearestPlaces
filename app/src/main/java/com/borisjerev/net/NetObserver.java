package com.borisjerev.net;

/**
 * Created by user on 21/06/2016.
 */
public interface NetObserver {
    void onNetRequestInProgress(HttpRequest httpRequestInProgress);
    void onNetRequestSuccess(HttpRequest httpRequestInProgress);
    void onNetRequestError(HttpRequest httpRequestInProgress);
    void onNetRequestDataSendError(HttpRequest httpRequestInProgress);
}
