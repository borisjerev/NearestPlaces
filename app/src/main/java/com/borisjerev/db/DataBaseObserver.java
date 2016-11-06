package com.borisjerev.db;

import com.borisjerev.model.PlacesInfo;

import java.util.List;

/**
 * Created by user on 21/06/2016.
 */
public interface DataBaseObserver {
    void onDataSetChange(List<PlacesInfo> placesInfo);
}
