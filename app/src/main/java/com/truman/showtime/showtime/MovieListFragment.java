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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MovieListFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    MovieAdapter mMovieAdapter;
    ArrayList<ArrayList<String>> mMovieResults;
    ArrayList<Movie> mMovieDetailsResults;

    SwipeRefreshLayout mRefreshLayout;
    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;
    GoogleApiClient mGoogleApiClient;
    ShowtimeService.Showtimes mShowtimeService;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    public MovieListFragment() {
    }

    @Override
    public void onLocationChanged(Location location) {
        refreshWithLocation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMovieResults = new ArrayList<ArrayList<String>>();
        mMovieDetailsResults = new ArrayList<Movie>();
        mMovieAdapter = new MovieAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mMovieAdapter);

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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.movie_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
        // Handle context actions
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
        Toast.makeText(getActivity().getApplicationContext(), "Location Services Disabled", Toast.LENGTH_LONG).show();
    }

    private class MovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private ArrayList<String> mMovie;

        public MovieHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            registerForContextMenu(itemView);
        }

        public void bindMovie(ArrayList<String> MovieFields) {
            mMovie = MovieFields;
            TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            TextView addressTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

            titleTextView.setText(mMovie.get(0));
            addressTextView.setText(mMovie.get(1));
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            int index = mMovieResults.indexOf(mMovie);
            detailIntent.putExtra("MovieDetails", mMovieDetailsResults.get(index));
            startActivity(detailIntent);
        }

        @Override
        public boolean onLongClick(View v) {
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
            ArrayList<String> crime = mMovieResults.get(pos);
            holder.bindMovie(crime);
        }

        @Override
        public int getItemCount() {
            return mMovieResults.size();
        }
    }

    public class ShowtimeApiManager extends AsyncTask<String, String, List<Movie>> {
        String mCacheKey;
        protected List<Movie> getResponse(String lat, String lon, String date, String city) {
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            mCacheKey = "movies:city:" + city + "date:" + today.month + today.monthDay + today.year;
            String result = null;
            List<Movie> movies = null;
            try {
                movies = cachedResultsForKey(mCacheKey);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (movies == null) {
                mShowtimeService = ShowtimeService.adapter();
                movies = mShowtimeService.listMovies(lat, lon, date, city);
//                Movie movie = mShowtimeService.movieDetails("ddf88042ef931de1", lat, lon, date, city);
//                Log.d("Showtime", movie.name.toString());
            }

            return movies;
        }

        @Override
        protected List<Movie> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2], arg0[3]);
        }

        @Override
        protected void onPostExecute(List<Movie> results) {
            mMovieResults.clear();
            try {
                cacheResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(results);
        }

        public void cacheResults(List<Movie> results) throws IOException {
            FileOutputStream fos = getActivity().getApplicationContext().openFileOutput(mCacheKey, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(results);
            os.close();
            fos.close();
        }

        public List<Movie> cachedResultsForKey(String cacheKey) throws IOException, ClassNotFoundException {
            FileInputStream fis = getActivity().getApplicationContext().openFileInput(cacheKey);
            ObjectInputStream is = new ObjectInputStream(fis);
            List<Movie> Movies = (List<Movie>) is.readObject();
            is.close();
            fis.close();
            return Movies;
        }

        public void parseAndReloadResults(List<Movie> result){
            if (result.size() > 0){
                for (int i = 0; i < result.size(); i++){
                    Movie Movie = result.get(i);
                    mMovieDetailsResults.add(Movie);
                    ArrayList<String> fields = new ArrayList<>();
                    fields.add(Movie.name);
                    fields.add(Movie.description);

                    mMovieResults.add(fields);
                }
                mMovieAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                Toast.makeText(getActivity().getApplicationContext(), "No Movies found", Toast.LENGTH_LONG).show();
            }
        }
    }
}
