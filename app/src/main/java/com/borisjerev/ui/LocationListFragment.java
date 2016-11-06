package com.borisjerev.ui;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.borisjerev.db.DataBaseManager;
import com.borisjerev.db.DataBaseObserver;
import com.borisjerev.model.PlacesInfo;
import com.borisjerev.net.HttpClient;
import com.borisjerev.net.HttpRequest;
import com.borisjerev.net.NetObserver;
import com.borisjerev.techhudletask.MainActivity;
import com.borisjerev.techhudletask.R;

import java.util.List;

/**
 * Created by user on 18/06/2016.
 */
public class LocationListFragment extends Fragment implements FragmentContract, DataBaseObserver, NetObserver {

    private RecyclerView mRecyclerView;
    private RecycleAdapter mAdapter;
    private Location mCurrentLocation;

    private TextView mNothingFoundView;
    private ProgressBar mProgressView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.list_fragment, container, false);
        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.listView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateValuesFromBundle(savedInstanceState);

        mNothingFoundView = (TextView) rootView.findViewById(R.id.emptyView);
        mProgressView = (ProgressBar) rootView.findViewById(R.id.progressBar);

        final List<PlacesInfo> places = DataBaseManager.getInstance(getContext()).query();
        if (places.size() > 0) {
            mProgressView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

        mAdapter = new RecycleAdapter(places);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DataBaseManager.getInstance(getContext()).registerObserver(this);
        HttpClient.getInstance(getActivity()).registerObserver(this);
    }

    @Override
    public void onPause() {
        DataBaseManager.getInstance(getContext()).unregisterObserver(this);
        HttpClient.getInstance(getActivity()).unregisterObserver(this);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(MainActivity.CURRENT_LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCurrentLocation(Location location) {
        mCurrentLocation = location;
    }

    @Override
    public void onDataSetChange(List<PlacesInfo> placesInfo) {
        mProgressView.setVisibility(View.GONE);
        if (placesInfo.size() > 0) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mNothingFoundView.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mNothingFoundView.setVisibility(View.VISIBLE);
        }

        mAdapter.setItems(placesInfo);
        mAdapter.notifyDataSetChanged();
    }

    public void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(MainActivity.CURRENT_LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(MainActivity.CURRENT_LOCATION_KEY);
            }
        }
    }

    @Override
    public void onNetRequestInProgress(HttpRequest httpRequestInProgress) {}

    @Override
    public void onNetRequestSuccess(HttpRequest httpRequestInProgress) {}

    @Override
    public void onNetRequestError(HttpRequest httpRequestInProgress) {
        onHttpRequestError();
    }

    @Override
    public void onNetRequestDataSendError(HttpRequest httpRequestInProgress) {
        onHttpRequestError();
    }

    private void onHttpRequestError() {
        if (mAdapter.getItemCount() < 1) {
            mRecyclerView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.GONE);
            mNothingFoundView.setVisibility(View.VISIBLE);
        }
    }

    public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.ViewHolder> {
        private List<PlacesInfo> mLocationListInfo;
        private String mFormatDistance;

        public RecycleAdapter(List<PlacesInfo> locationListInfo) {
            this.mLocationListInfo = locationListInfo;
            mFormatDistance = getString(R.string.format_distance);
        }

        public void setItems(List<PlacesInfo> locationListInfo) {
            this.mLocationListInfo = locationListInfo;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final public TextView bar;
            final public TextView distance;

            public ViewHolder (View v, TextView bar, TextView distance) {
                super(v);

                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            final PlacesInfo searchPlace = mLocationListInfo.get(position);
                            final PlacesInfo.Location loc = searchPlace.getGeometry().getLocation();
                            if (loc != null) {
                                final Uri gmmIntentUri =
                                        Uri.parse("geo:0,0?q=" + loc.getLat() + "," + loc.getLng()
                                                + "(" + Uri.encode(searchPlace.getName())+ ")");
                                final Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                }
                            }
                        }
                    }
                });

                this.bar = bar;
                this.distance = distance;
            }
        }

        @Override
        public RecycleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.location_info, parent, false);

            final TextView bar = (TextView) v.findViewById(R.id.bar);
            final TextView distance = (TextView) v.findViewById(R.id.distance);

            return new ViewHolder(v, bar, distance);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final PlacesInfo locationInfo = mLocationListInfo.get(position);
            holder.bar.setText(locationInfo.getName());
            holder.distance.setText(
                    String.format(mFormatDistance, String.valueOf(locationInfo.getDistanceBetweenCurrentLocation()))
            );
        }

        @Override
        public int getItemCount() {
            return mLocationListInfo.size();
        }
    }
}
