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
package com.truman.showtime.showtime.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
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
import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.service.ShowtimeService;
import com.truman.showtime.showtime.ui.view.SimpleDividerItemDecoration;
import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.ui.activity.DetailActivity;

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

public class MovieListFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ObservableScrollViewCallbacks {
    private MovieAdapter mMovieAdapter;
    private List<Movie> mMovieResults;

    private SwipeRefreshLayout mRefreshLayout;
    private ObservableRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private GoogleApiClient mGoogleApiClient;
    private ShowtimeService.Showtimes mShowtimeService;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private String mCity;
    private Context mApplicationContext;
    private Movie mSelectedMovie;

    public MovieListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = getActivity().getApplicationContext();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        refreshWithLocation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMovieResults = new ArrayList<Movie>();
        mMovieAdapter = new MovieAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));

        mRecyclerView = (ObservableRecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.setScrollViewCallbacks(this);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mApplicationContext));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mMovieAdapter);

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(1000 * 1000)        // 1000 seconds, in milliseconds
                .setFastestInterval(100 * 1000); // 100 seconds, in milliseconds

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

    @Override
    public void onScrollChanged(int i, boolean b, boolean b2) {
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
//        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
//        actionBar.setShowHideAnimationEnabled(true);
//        if (scrollState == ScrollState.DOWN){
//            actionBar.show();
//        } else if (scrollState == ScrollState.UP){
//            actionBar.show();
//        }
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

    public void fetchTimesForDate(String date) {
        ShowtimeApiManager api = new ShowtimeApiManager();
        String lat = String.valueOf(mLastLocation.getLatitude());
        String lon = String.valueOf(mLastLocation.getLongitude());
        api.execute(lat, lon, date);
    }

    public void refreshWithLocation() {
        if (!mGoogleApiClient.isConnected()){
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(false);
                }
            });
            Toast.makeText(mApplicationContext, getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
            return;
        }
        if (mLastLocation != null) {
            Location newLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (newLocation != null){
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
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient) != null ? LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient) : mLastLocation;
        if (mLastLocation != null) {
            fetchTimesForDate("0");
        } else {
            if (Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK")) {
                ShowtimeApiManager api = new ShowtimeApiManager();
                api.execute("33.8358", "-118.3406", "0", "Torrance,CA");
            } else if (networkLocation() != null) {
                mLastLocation = networkLocation();
                fetchTimesForDate("0");
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
        inflater.inflate(R.menu.movie, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getString(R.string.share_movie))) {
            AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mSelectedMovie.name + "\n" + "http://google.com/movies?near=" + mCity + "&mid=" + mSelectedMovie.id);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_movie)));
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onConnectionSuspended(int i) {
//        LocationManager locationManager = (LocationManager) mApplicationContext.getSystemService(Context.LOCATION_SERVICE);
//        mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        if (mLastLocation != null){
//            refreshWithLocation();
//        }
    }

    public Location networkLocation(){
        LocationManager locationManager = (LocationManager) mApplicationContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRefresh() {
        refreshWithLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(mApplicationContext, getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(false);
            }
        });

    }

    private class MovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Movie mMovie;

        public MovieHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            registerForContextMenu(itemView);
        }

        public void bindMovie(Movie movie) {
            mMovie = movie;
            TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            TextView addressTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

            titleTextView.setText(mMovie.name);
            addressTextView.setText(mMovie.description);
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            detailIntent.putExtra("Type", "Movie");
            detailIntent.putExtra("MovieDetails", mMovie);
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
            mSelectedMovie = mMovie;
            getActivity().openContextMenu(v);
            return true;
        }
    }

    private class MovieAdapter
            extends RecyclerView.Adapter<MovieHolder> {
        @Override
        public MovieHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_layout, parent, false);
            return new MovieHolder(view);
        }

        @Override
        public void onBindViewHolder(MovieHolder holder, int pos) {
            Movie movie = mMovieResults.get(pos);
            holder.bindMovie(movie);
        }

        @Override
        public int getItemCount() {
            return mMovieResults.size();
        }
    }

    public class ShowtimeApiManager extends AsyncTask<String, String, List<Movie>> {
        String mCacheKey;
        protected List<Movie> getResponse(String lat, String lon, String date) {
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            Geocoder geocoder = new Geocoder(mApplicationContext, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(new Double(lat), new Double(lon), 1);
                mCity = URLEncoder.encode(addresses.get(0).getLocality() + " " + addresses.get(0).getAdminArea(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
            mCacheKey = "movies_city_" + mCity + "_date_" + today.month + today.monthDay + today.year;
            String result = null;
            List<Movie> movies = null;
            Log.d("Showtime", mCacheKey);
            try {
                movies = cachedResultsForKey(mCacheKey);
                Log.d("Showtime", "Movies Cache hit");
            } catch (IOException e) {
//                e.printStackTrace();
                Log.d("Showtime", "movies ioexception miss");

            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
                Log.d("Showtime", "movies class not found miss");
            }

            if (movies == null) {
                mShowtimeService = ShowtimeService.adapter();
                movies = mShowtimeService.listMovies(lat, lon, date, mCity);
            }

            return movies;
        }

        @Override
        protected List<Movie> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2]);
        }

        @Override
        protected void onPostExecute(List<Movie> results) {
            mMovieResults = results;
            try {
                cacheResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(results);
        }

        public void cacheResults(List<Movie> results) throws IOException {
            if (results != null) {
                List<Movie> movies = null;
                File file = new File(mApplicationContext.getCacheDir(), mCacheKey);
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(results);
                os.close();
                fos.close();
            }
        }

        public List<Movie> cachedResultsForKey(String cacheKey) throws IOException, ClassNotFoundException {
            File file = new File(mApplicationContext.getCacheDir(), cacheKey);
            List<Movie> movies = null;
            if (file.exists()){
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream is = new ObjectInputStream(fis);
                movies = (List<Movie>) is.readObject();
                is.close();
                fis.close();
            }

            return movies;
        }

        public void parseAndReloadResults(List<Movie> result){
            if (result.size() > 0){
                mMovieAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                Toast.makeText(mApplicationContext, getString(R.string.no_movies_found), Toast.LENGTH_LONG).show();
            }
        }
    }
}
