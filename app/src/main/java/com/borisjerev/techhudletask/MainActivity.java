package com.borisjerev.techhudletask;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.Toast;

import com.borisjerev.db.DataBaseManager;
import com.borisjerev.net.HttpClient;
import com.borisjerev.net.HttpRequest;
import com.borisjerev.net.NetObserver;
import com.borisjerev.ui.FragmentContract;
import com.borisjerev.ui.LocationListFragment;
import com.borisjerev.ui.LocationMapFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, NetObserver {

    private static final int ON_CONNECTED_PERMISSION = 0;

    public final static String CURRENT_LOCATION_KEY = "location-key";
    public final static String LAST_LOCATION_UPDATE_KEY = "update-location-key";

    // TODO make LocationManager class and move the corresponding logic there after
    private static long INITIAL_REQUEST_DELAY_BEFORE_MAKING_ANOTHER_HTTP_REQUEST = 2000l;
    private long mTimeFirstRequest = 0;
    private static final int RADIUS_METERS = 5000;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final float METERS_TRAVELLED_BEFORE_MAKING_REQUEST = 10f;
    private static final String URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=%d&key=%s";

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrLocation;
    private Location mLastUpdateLocation;
    private LocationRequest mLocationRequest;

    private boolean mFirstTimeFethcData;

    private static final int NUM_FRAGMENTS = 2;
    private static final int LIST_FRAGMENT_POSITION = 0;
    private static final int MAP_FRAGMENT_POSITION = 1;

    private boolean isTabLayout;

    private LocationListFragment mLocationListFragment;
    private LocationMapFragment mLocationMapFragment;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private FragmentManager mFragmentManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirstTimeFethcData = true;

        mFragmentManager = getSupportFragmentManager();

        isTabLayout = findViewById(R.id.fragment_container) != null;

        if (isTabLayout) {
            mLocationListFragment =
                    (LocationListFragment) mFragmentManager.findFragmentById(R.id.LocationListFragment);

            mLocationMapFragment =
                    (LocationMapFragment) mFragmentManager.findFragmentById(R.id.LocationMapFragment);
        } else {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);

            mViewPager = (ViewPager) findViewById(R.id.container);
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.requestTransparentRegion(mViewPager);

            mTabLayout = (TabLayout) findViewById(R.id.tabs);
            mTabLayout.setupWithViewPager(mViewPager);
        }

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        makePlacesUpdatesRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        HttpClient.getInstance(this).registerObserver(this);
    }

    @Override
    protected void onPause() {
        HttpClient.getInstance(this).unregisterObserver(this);
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(CURRENT_LOCATION_KEY, mCurrLocation);
        savedInstanceState.putParcelable(LAST_LOCATION_UPDATE_KEY, mLastUpdateLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrLocation = location;
        if (mCurrLocation != null) {
            onCurrentLocation(mCurrLocation);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            onConnectedAction();
        } else {
            askForPermission();
        }
    }


    private void onConnectedAction() {
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mCurrLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mCurrLocation != null) {
                onCurrentLocation(mCurrLocation);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    // NetObserver methods
    @Override
    public void onNetRequestInProgress(HttpRequest httpRequestInProgress) {}

    @Override
    public void onNetRequestSuccess(HttpRequest httpRequestInProgress) {
        mFirstTimeFethcData = false;
        onFetchDataFinished();
    }

    @Override
    public void onNetRequestError(HttpRequest httpRequestInProgress) {
        onFetchDataFinished();
    }

    @Override
    public void onNetRequestDataSendError(HttpRequest httpRequestInProgress) {
        onFetchDataFinished();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ON_CONNECTED_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    onConnectedAction();
                }
                return;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onFetchDataFinished() {
        mFirstTimeFethcData = false;
    }

    private void askForPermission() {
       if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, ON_CONNECTED_PERMISSION);
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(CURRENT_LOCATION_KEY)) {
                mCurrLocation = savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_LOCATION_UPDATE_KEY)) {
                mLastUpdateLocation = savedInstanceState.getParcelable(LAST_LOCATION_UPDATE_KEY);
            }
        }
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void onCurrentLocation() {
        if (isTabLayout) {
            mLocationListFragment.onCurrentLocation(mCurrLocation);
            mLocationMapFragment.onCurrentLocation(mCurrLocation);
        } else {
            if (mCurrLocation != null) {
                final SparseArray<Fragment> fragments = mSectionsPagerAdapter.getRegisteredFragments();
                if (fragments != null) {
                    final int numFragments = fragments.size();
                    Fragment fragment;
                    for (int i = 0; i < numFragments; i++) {
                        fragment = fragments.get(fragments.keyAt(i));
                        if (fragment != null) {
                            ((FragmentContract) fragment).onCurrentLocation(mCurrLocation);
                        }
                    }
                }
            }
        }
    }


    private void onCurrentLocation(Location location) {
        DataBaseManager.getInstance(this).insert(location);
        makePlacesUpdatesRequest();
        onCurrentLocation();
    }

    private void makePlacesUpdatesRequest() {
        if (mCurrLocation != null) {
            // use the formula for calculation the distance if slow
            final boolean makeUpdateRequest = (
                    ( (!mFirstTimeFethcData &&
                            (mLastUpdateLocation == null || mCurrLocation.distanceTo(mLastUpdateLocation) >= METERS_TRAVELLED_BEFORE_MAKING_REQUEST))
                      || System.currentTimeMillis() - INITIAL_REQUEST_DELAY_BEFORE_MAKING_ANOTHER_HTTP_REQUEST > mTimeFirstRequest
                    )
            );
            if (makeUpdateRequest) {
                if (mTimeFirstRequest == 0) {
                    mTimeFirstRequest = System.currentTimeMillis();
                }
                // this must be stored on a server and get from there
                //final String ak = new String("AIzaSyBL6Vq0YBoPhPs4DRIa9_mAr2vjAkveu4Q");
                final String ak = new String("AIzaSyDcIKT9AcB4vod09gezfkmwHi2cINUKTus");
                final String url =
                        new String (String.format(URL, mCurrLocation.getLatitude(),
                                mCurrLocation.getLongitude(), RADIUS_METERS, ak));
                HttpClient.getInstance(getApplicationContext()).postRequest(url);
            }
        }
    }

    // inner classes
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case MAP_FRAGMENT_POSITION:
                    return new LocationMapFragment();
                case LIST_FRAGMENT_POSITION:
                default:
                    return new LocationListFragment();
            }
        }

        @Override
        public int getCount() {
            return NUM_FRAGMENTS;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public SparseArray<Fragment> getRegisteredFragments() {
            return registeredFragments;
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case LIST_FRAGMENT_POSITION:
                    return getResources().getString(R.string.tabList);
                case MAP_FRAGMENT_POSITION:
                    return getResources().getString(R.string.tabMap);
            }
            return null;
        }
    }
}
