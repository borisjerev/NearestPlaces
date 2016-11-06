package com.borisjerev.net;

import com.borisjerev.model.PlacesInfo;

import java.util.List;

/**
 * Created by user on 20/06/2016.
 */
public class ServiceResponse {
    private String status;
    private List<PlacesInfo> results;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PlacesInfo> getResults() {
        return results;
    }

    public void setResults(List<PlacesInfo> results) {
        this.results = results;
    }
}
