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

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TheaterListFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ObservableScrollViewCallbacks {
    private TheaterAdapter mTheaterAdapter;
    private ArrayList<Theater> mTheaterResults;

    private SwipeRefreshLayout mRefreshLayout;
    private ObservableRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private GoogleApiClient mGoogleApiClient;
    private ShowtimeService.Showtimes mShowtimeService;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private String mCity;
    private Theater mSelectedTheater;
    private Context mApplicationContext;

    public TheaterListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = getActivity().getApplicationContext();
    }

    @Override
    public void onLocationChanged(Location location) {
        refreshWithLocation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTheaterResults = new ArrayList<Theater>();
        mTheaterAdapter = new TheaterAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (ObservableRecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.setScrollViewCallbacks(this);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mApplicationContext));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);

        buildGoogleApiClient();
        mGoogleApiClient.connect();
//        mLocationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_NO_POWER)
//                .setInterval(1000 * 1000)        // 1000 seconds, in milliseconds
//                .setFastestInterval(100 * 1000); // 100 second, in milliseconds

        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        mGoogleApiClient = new GoogleApiClient.Builder(mApplicationContext)
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
            ShowtimeAPITask api = new ShowtimeAPITask();
            String lat = String.valueOf(mLastLocation.getLatitude());
            String lon = String.valueOf(mLastLocation.getLongitude());

            Geocoder geocoder = new Geocoder(mApplicationContext, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
//                final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
//                actionBar.setSubtitle("near " + addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea());
                mCity = URLEncoder.encode(addresses.get(0).getLocality() + addresses.get(0).getAdminArea(), "UTF-8");
                api.execute(lat, lon, "0", URLEncoder.encode(addresses.get(0).getLocality(), "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK")) {
                ShowtimeAPITask api = new ShowtimeAPITask();
                api.execute("33.8358", "-118.3406", "0", "Torrance,CA");
            } else {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                mRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.setRefreshing(false);
                    }
                });
                Toast.makeText(mApplicationContext, getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.theater_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
        // Handle context actions
        if (item.getTitle().equals("Directions")){
            String theaterString = null;
            try {
                theaterString = URLEncoder.encode(mSelectedTheater.address, "UTF-8");
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + theaterString);
                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");
                // Attempt to start an activity that can handle the Intent
                if (mapIntent.resolveActivity(mApplicationContext.getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (item.getTitle().equals("Share")){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mSelectedTheater.name + "\n" + mSelectedTheater.address);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_theater)));
        }
        return super.onContextItemSelected(item);
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
        Log.d("Showtime", Build.MODEL);
        if (Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK")) {
            refreshWithLocation();
        } else {
            Toast.makeText(mApplicationContext, getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onScrollChanged(int i, boolean b, boolean b2) {
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Theater mTheater;

        public TheaterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            registerForContextMenu(itemView);
        }

        public void bindTheater(Theater theater) {
            mTheater = theater;
            TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            TextView addressTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

            titleTextView.setText(mTheater.name);
            addressTextView.setText(mTheater.address);
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            detailIntent.putExtra("Type", "Theater");
            detailIntent.putExtra("TheaterDetails", mTheater);

            if (Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK")) {
                detailIntent.putExtra("Lat", "33.8358");
                detailIntent.putExtra("Lon", "-118.3406");
                detailIntent.putExtra("City", "Torrance,+CA");
            } else {
                detailIntent.putExtra("Lat", String.valueOf(mLastLocation.getLatitude()));
                detailIntent.putExtra("Lon", String.valueOf(mLastLocation.getLongitude()));
                detailIntent.putExtra("City", mCity);
            }

            startActivity(detailIntent);
        }

        @Override
        public boolean onLongClick(View v) {
            mSelectedTheater = mTheater;
            getActivity().openContextMenu(v);
            return true;
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
            Theater theater = mTheaterResults.get(pos);
            holder.bindTheater(theater);
        }

        @Override
        public int getItemCount() {
            return mTheaterResults.size();
        }
    }

    public class ShowtimeAPITask extends AsyncTask<String, String, ArrayList<Theater>> {
        String mCacheKey;
        protected ArrayList<Theater> getResponse(String lat, String lon, String date, String city) {
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            mCacheKey = "theaters_city_" + city + "_date_" + today.month + today.monthDay + today.year;
            String result = null;
            ArrayList<Theater> theaters = null;
            Log.d("Showtime", mCacheKey);
            try {
                theaters = cachedResultsForKey(mCacheKey);
                Log.d("Showtime", "theaters Cache hit");
            } catch (IOException e) {
//                e.printStackTrace();
                Log.d("Showtime", "theaters ioexception miss");

            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
                Log.d("Showtime", "theaters class not found miss");
            }

            if (theaters == null) {
                mShowtimeService = ShowtimeService.adapter();
                theaters = mShowtimeService.listTheaters(lat, lon, date, city);
            }

            return theaters;
        }

        @Override
        protected ArrayList<Theater> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2], arg0[3]);
        }

        @Override
        protected void onPostExecute(ArrayList<Theater> results) {
            mTheaterResults = results;
            try {
                cacheResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(results);
        }

        public void cacheResults(ArrayList<Theater> results) throws IOException {
            File file = new File(mApplicationContext.getCacheDir(), mCacheKey);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(results);
            os.close();
            fos.close();
        }

        public ArrayList<Theater> cachedResultsForKey(String cacheKey) throws IOException, ClassNotFoundException {
            File file = new File(mApplicationContext.getCacheDir(), cacheKey);
            ArrayList<Theater> theaters = null;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream is = new ObjectInputStream(fis);
                theaters = (ArrayList<Theater>) is.readObject();
                is.close();
                fis.close();
            }
            return theaters;
        }

        public void parseAndReloadResults(ArrayList<Theater> result){
            if (result.size() > 0){
                mTheaterAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                Toast.makeText(mApplicationContext, getString(R.string.theaters_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }
}
