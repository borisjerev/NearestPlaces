package com.borisjerev.db;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.widget.Toast;

import com.borisjerev.model.PlacesInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 21/06/2016.
 */
// TODO do I need CouchDB, keep it simple for now
public class DataBaseManager {
    private static DataBaseManager INSTANCE;
    // application context
    private static Context mCtx;
    private Location mCurrentLocation;
    private final List<PlacesInfo> mCache = new ArrayList<>();

    private final Map<String, WeakReference<DataBaseObserver>> mObservers = new HashMap<>();

    private final Object mLocationLock = new Object();

    private static final int METERS_LOCATION_ACCURACY = 1;

    public static synchronized DataBaseManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DataBaseManager(context);
        }
        return INSTANCE;
    }

    private DataBaseManager(Context context) {
        mCtx = context.getApplicationContext();
    }

    public void insert(List<PlacesInfo> placesInfoList) {
        final List<PlacesInfo> newPlaces = placesInfoList;
        new AsyncTask<Void, Void, Void>() {

            protected Void doInBackground(Void... params) {
                addToDatabase(newPlaces);
                return null;
            }

            protected void onPostExecute(Void v) {
                notifyObservers(new ArrayList<>(mCache));
            }

        }.execute();
    }

    public void insert(Location location) {
        final Location newLocation = location;
        new AsyncTask<Void, Void, Boolean>() {

            protected Boolean doInBackground(Void... params) {

                Location oldLocation = getCurrentLocation();
                boolean changedLocation = newLocation != null &&
                        (oldLocation == null || newLocation.distanceTo(oldLocation) > METERS_LOCATION_ACCURACY );

                if (changedLocation) {
                    setCurrentLocation(newLocation);
                    sortByDistance(newLocation, mCache);
                }
                return changedLocation;
            }

            protected void onPostExecute(Boolean changedLocation) {
                if (changedLocation && getCacheSize() > 0) {
                    notifyObservers(new ArrayList<>(mCache));
                }
            }

        }.execute();
    }

    public List<PlacesInfo> query() {
        synchronized (mCache) {
            return new ArrayList<>(mCache);
        }
    }

    public void addToDatabase(List<PlacesInfo> placesInfoList) {
        Location currLocation = getCurrentLocation();
        synchronized (mCache) {
            if (placesInfoList == null) {
                placesInfoList = new ArrayList<>(0);
            }
            List<PlacesInfo> placesInfo = sortByDistance(currLocation, placesInfoList);
            mCache.clear();
            mCache.addAll(placesInfo);
        }
    }

    private void notifyObservers(List<PlacesInfo> placesInfoList) {
        for (WeakReference observer : mObservers.values()) {
            if (observer.get() != null) {
                ((DataBaseObserver)observer.get()).onDataSetChange(placesInfoList);
            }
        }
    }

    public void registerObserver(DataBaseObserver observer) {
        mObservers.put(observer.getClass().getName(), new WeakReference<>(observer));
    }

    public void unregisterObserver(DataBaseObserver observer) {
        mObservers.remove(observer.getClass().getName());
    }

    private List<PlacesInfo> sortByDistance(Location location, List<PlacesInfo> placesInfoList) {
        synchronized (mCache) {
            if (location != null && placesInfoList.size() > 0) {
                float[] distance = new float[3];
                for (PlacesInfo placeInfo : placesInfoList) {
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                            placeInfo.getGeometry().getLocation().getLat(), placeInfo.getGeometry().getLocation().getLng(),
                            distance);

                    placeInfo.setDistanceBetweenCurrentLocation((int) distance[0]);
                }

                Collections.sort(placesInfoList, new Comparator<PlacesInfo>() {
                    @Override
                    public int compare(PlacesInfo t1, PlacesInfo t2) {
                        return t1.getDistanceBetweenCurrentLocation() - t2.getDistanceBetweenCurrentLocation();
                    }
                });
            }
        }
        return placesInfoList;
    }

    private int getCacheSize() {
        synchronized (mCache) {
            return mCache.size();
        }
    }

    private Location getCurrentLocation() {
        synchronized (mLocationLock) {
            return mCurrentLocation;
        }
    }

    private void setCurrentLocation(Location location) {
        synchronized (mLocationLock) {
            mCurrentLocation = location;
        }
    }
}
