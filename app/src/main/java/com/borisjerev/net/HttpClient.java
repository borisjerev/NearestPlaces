package com.borisjerev.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.borisjerev.db.DataBaseManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 20/06/2016.
 */
public class HttpClient {
    private static HttpClient INSTANCE;
    private RequestQueue mRequestQueue;
    // application context
    private Context mCtx;
    private final Map<String, WeakReference<NetObserver>> mObservers = new HashMap<>();
    private final Map<String, HttpRequest> httpRequestsQueue = new HashMap<>();

    private HttpClient(Context context) {
        mCtx = context.getApplicationContext();
        mRequestQueue = getRequestQueue();
    }

    public static synchronized HttpClient getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new HttpClient(context);
        }
        return INSTANCE;
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public void registerObserver(NetObserver observer) {
        mObservers.put(observer.getClass().getName(), new WeakReference<>(observer));
    }

    public void unregisterObserver(NetObserver observer) {
        mObservers.remove(observer.getClass().getName());
    }

    public void postRequest(final String url) {
        final JacksonRequest<ServiceResponse> jRequest = new JacksonRequest<>(Request.Method.GET, url,
                null,
                ServiceResponse.class,
                new Response.Listener<ServiceResponse>() {
                    @Override
                    public void onResponse(ServiceResponse response) {
                        boolean error = true;
                        if ("ok".equalsIgnoreCase(response.getStatus())) {
                            DataBaseManager.getInstance(mCtx).insert(response.getResults());
                            error = false;
                        }
                        onRequestFinished(url, response, error);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        onRequestFinished(url, volleyError, true);
                    }
                });
        addToRequestQueue(jRequest);
        notifyObservers(onRequestStart(url));
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    private HttpRequest onRequestStart(String url) {
        HttpRequest httpRequest = new HttpRequest(url);
        httpRequestsQueue.put(url, httpRequest);

        return httpRequest;
    }

    private void onRequestFinished(String url, Object result, boolean error) {
        final HttpRequest httpRequest = httpRequestsQueue.get(url);
        httpRequestsQueue.remove(url);
        if(httpRequest != null) {
            httpRequest.onHttpRequestFinished(result, error);
            notifyObservers(httpRequest);
        }
    }

    private void notifyObservers(HttpRequest httpRequest) {
        final Object result = httpRequest.getResult();
        if (result == null) {
            for (WeakReference observer : mObservers.values()) {
                if (observer.get() != null) {
                    ((NetObserver)observer.get()).onNetRequestInProgress(httpRequest);
                }
            }
        } else {
            if (result instanceof ServiceResponse) {
                if (!httpRequest.isError()) {
                    for (WeakReference observer : mObservers.values()) {
                        if (observer.get() != null) {
                            ((NetObserver) observer.get()).onNetRequestSuccess(httpRequest);
                        }
                    }
                } else {
                    for (WeakReference observer : mObservers.values()) {
                        if (observer.get() != null) {
                            ((NetObserver) observer.get()).onNetRequestDataSendError(httpRequest);
                        }
                    }
                }
            } else if (result instanceof VolleyError) {
                for (WeakReference observer : mObservers.values()) {
                    if (observer.get() != null) {
                        ((NetObserver) observer.get()).onNetRequestError(httpRequest);
                    }
                }
            }
        }
    }
}
