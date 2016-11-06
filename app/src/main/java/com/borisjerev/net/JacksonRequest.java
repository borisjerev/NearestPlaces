package com.borisjerev.net;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.borisjerev.db.Mapper;

/**
 * Created by user on 20/06/2016.
 */
public class JacksonRequest<T> extends JsonRequest<T> {
    private Class<T> responseType;

    public JacksonRequest(int method, String url, Object requestData, Class<T> responseType,
                          Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, (requestData == null) ? null : Mapper.string(requestData), listener, errorListener);
        this.responseType = responseType;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(Mapper.objectOrThrow(jsonString, responseType), HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}