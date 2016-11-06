package com.borisjerev.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Bundle;

import com.borisjerev.db.DataBaseManager;
import com.borisjerev.db.DataBaseObserver;
import com.borisjerev.model.PlacesInfo;
import com.borisjerev.techhudletask.MainActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.borisjerev.techhudletask.R;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LocationMapFragment extends Fragment implements FragmentContract, DataBaseObserver {

    private static final int MAP_ZOOM_FACTOR = 12;

    private Location mCurrentLocation;
    private MapView mMapView;
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_fragment, container, false);

        mMapView = (MapView) v.findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);

        MapsInitializer.initialize(getActivity());

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                setupMap(googleMap);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
        DataBaseManager.getInstance(getContext()).registerObserver(this);
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        DataBaseManager.getInstance(getContext()).unregisterObserver(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(MainActivity.CURRENT_LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCurrentLocation(Location location) {
        setMyLocationEnabledIfNeeded();
        if (mCurrentLocation == null && location != null) {
            mCurrentLocation = location;
            setupInitialLocation();
        } else {
            mCurrentLocation = location;
        }
    }

    @Override
    public void onDataSetChange(List<PlacesInfo> placesInfos) {
        if (mMap != null) {
            // TODO if slow use these AsyncTask
            //new SetupMarkers(this, placesInfos, getString(R.string.format_distance)).execute();
            mMap.clear();
            for (PlacesInfo placeInfo : placesInfos) {
                MarkerOptions markerOption = new MarkerOptions()
                        .position(new LatLng(placeInfo.getGeometry().getLocation().getLat(),
                                placeInfo.getGeometry().getLocation().getLng()))
                        .title(placeInfo.getName())
                        .snippet(
                                String.format(getString(R.string.format_distance), String.valueOf(placeInfo.getDistanceBetweenCurrentLocation()))
                        );

                mMap.addMarker(markerOption);
            }
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(MainActivity.CURRENT_LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(MainActivity.CURRENT_LOCATION_KEY);
            }
        }
    }

    private void setupMap(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap != null) {
            //mMap.getUiSettings().setMyLocationButtonEnabled(true);

            setMyLocationEnabledIfNeeded();
            setupInitialLocation();
        }
    }

    private void setMyLocationEnabledIfNeeded() {
        if (mMap != null && !mMap.isMyLocationEnabled()) {
            // the permission is asked in MainActivity
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private void setupInitialLocation() {
        if (mCurrentLocation != null && mMap != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), MAP_ZOOM_FACTOR);
            mMap.animateCamera(cameraUpdate);
        }
    }

//    private void addMarkers(List<MarkerOptions> markerOptions) {
//        if (mMap != null) {
//            mMap.clear();
//            for (MarkerOptions markerOption : markerOptions) {
//                mMap.addMarker(markerOption);
//            }
//        }
//
//    }

//    private static class SetupMarkers extends AsyncTask<Void, Void, Void> {
//        private final WeakReference<LocationMapFragment> wFragment;
//        private final List<PlacesInfo> placesInfo;
//        private final String formatDistance;
//        private final List<MarkerOptions> markerOptions;
//
//        public SetupMarkers(LocationMapFragment mapFragment, List<PlacesInfo> placesInfo, String formatDistance) {
//            this.wFragment = new WeakReference<>(mapFragment);
//            this.placesInfo = placesInfo;
//            this.formatDistance = formatDistance;
//            this.markerOptions = new ArrayList<>(placesInfo.size());
//        }
//
//        protected Void doInBackground(Void... params) {
//            for (PlacesInfo placeInfo : placesInfo) {
//                MarkerOptions markerOption = new MarkerOptions()
//                        .position(new LatLng(placeInfo.getGeometry().getLocation().getLat(),
//                                placeInfo.getGeometry().getLocation().getLng()))
//                        .title(placeInfo.getName())
//                        .snippet(
//                                String.format(formatDistance, String.valueOf(placeInfo.getDistanceBetweenCurrentLocation()))
//                        );
//
//                markerOptions.add(markerOption);
//            }
//
//            return null;
//        }
//
//        protected void onPostExecute(Void v) {
//            if (wFragment.get() != null) {
//                wFragment.get().addMarkers(markerOptions);
//            }
//        }
//    }
}