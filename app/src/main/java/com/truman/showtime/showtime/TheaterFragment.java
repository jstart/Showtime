/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.truman.showtime.showtime;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TheaterFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    TheaterAdapter mTheaterAdapter;
    ArrayList<ArrayList<String>> mTheaterResults;
    ArrayList<Theater> mTheaterDetailsResults;

    SwipeRefreshLayout mRefreshLayout;
    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;
    GoogleApiClient mGoogleApiClient;
    ShowtimeService.Showtimes mShowtimeService;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    public TheaterFragment() {
    }

    @Override
    public void onLocationChanged(Location location) {
        refreshWithLocation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTheaterResults = new ArrayList<ArrayList<String>>();
        mTheaterDetailsResults = new ArrayList<Theater>();
        mTheaterAdapter = new TheaterAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_NO_POWER)
                .setInterval(1000 * 1000)        // 1000 seconds, in milliseconds
                .setFastestInterval(100 * 1000); // 100 second, in milliseconds

        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setNestedScrollingEnabled(true);
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        mRefreshLayout.post(new Runnable() {
            @Override public void run() {
                mRefreshLayout.setRefreshing(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnecting()){
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        refreshWithLocation();
    }

    public void refreshWithLocation() {

        if (mLastLocation != null) {
            Location newLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation.getLatitude() == newLocation.getLatitude() && mLastLocation.getLongitude() == newLocation.getLongitude()){
                mRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.setRefreshing(false);
                    }
                });
                return;
            }
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            ShowtimeApiManager api = new ShowtimeApiManager();
            String lat = String.valueOf(mLastLocation.getLatitude());
            String lon = String.valueOf(mLastLocation.getLongitude());
            Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                api.execute(lat, lon, "0", URLEncoder.encode(addresses.get(0).getLocality(), "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                        mRefreshLayout.setRefreshing(false);
                }
            });
            Toast.makeText(getActivity().getApplicationContext(), "Location Services Disabled", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRefresh() {
        refreshWithLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity().getApplicationContext(), "Location Services Disabled", Toast.LENGTH_LONG).show();
    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ArrayList<String> mTheater;

        public TheaterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        public void bindTheater(ArrayList<String> theaterFields) {
            mTheater = theaterFields;
            TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            TextView addressTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

            titleTextView.setText(mTheater.get(0));
            addressTextView.setText(mTheater.get(1));
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            int index = mTheaterResults.indexOf(mTheater);
            detailIntent.putExtra("TheaterDetails", mTheaterDetailsResults.get(index));
            startActivity(detailIntent);
        }
    }

    private class TheaterAdapter
            extends RecyclerView.Adapter<TheaterHolder> {
        @Override
        public TheaterHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_layout, parent, false);
            return new TheaterHolder(view);
        }

        @Override
        public void onBindViewHolder(TheaterHolder holder, int pos) {
            ArrayList<String> crime = mTheaterResults.get(pos);
            holder.bindTheater(crime);
        }

        @Override
        public int getItemCount() {
            return mTheaterResults.size();
        }
    }

    public class ShowtimeApiManager extends AsyncTask<String, String, List<Theater>> {

        protected List<Theater> getResponse(String lat, String lon, String date, String city) {
            String result = null;
            mShowtimeService = ShowtimeService.adapter();
            List<Theater> theaters = mShowtimeService.listTheaters(lat, lon, date, city);
            return theaters;
        }

        @Override
        protected List<Theater> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2], arg0[3]);
        }

        @Override
        protected void onPostExecute(List<Theater> result) {
            mTheaterResults.clear();
            parseAndReloadResults(result);
        }

        public void parseAndReloadResults(List<Theater> result){
            if (result.size() > 0){
                for (int i = 0; i < result.size(); i++){
                    Theater theater = result.get(i);
                    mTheaterDetailsResults.add(theater);
                    ArrayList<String> fields = new ArrayList<>();
                    fields.add(theater.name);
                    fields.add(theater.address);

                    mTheaterResults.add(fields);
                }
                mTheaterAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                Toast.makeText(getActivity().getApplicationContext(), "No theaters found", Toast.LENGTH_LONG).show();
            }
        }
    }
}
