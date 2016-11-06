package com.borisjerev.model;

import android.support.annotation.NonNull;

/**
 * Created by user on 18/06/2016.
 */
public class LocationInfo implements Comparable<LocationInfo> {
    private String bar;
    private String placeID;
    private String id;
    private int distance;

    public LocationInfo(String bar, int distance){
        this.bar = bar;
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    public String getPlaceID() {
        return placeID;
    }

    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int compareTo(@NonNull LocationInfo locationListInfo) {
       return distance - locationListInfo.distance;
    }
}
