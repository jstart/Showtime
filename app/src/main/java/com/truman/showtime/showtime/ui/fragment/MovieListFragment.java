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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.service.ShowtimeService;
import com.truman.showtime.showtime.ui.activity.DetailActivity;
import com.truman.showtime.showtime.ui.view.SimpleDividerItemDecoration;

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

public class MovieListFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private MovieAdapter mMovieAdapter;
    private List<Movie> mMovieResults;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ShowtimeService.Showtimes mShowtimeService;
    private Location mLastLocation;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMovieResults = new ArrayList<Movie>();
        mMovieAdapter = new MovieAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mApplicationContext));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mMovieAdapter);

        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void fetchTimesForDate(String date) {
        ShowtimeApiManager api = new ShowtimeApiManager();
        String lat = String.valueOf(mLastLocation.getLatitude());
        String lon = String.valueOf(mLastLocation.getLongitude());
        api.execute(lat, lon, date);
    }

    public void refreshWithLocation(Location location) {
        try {
            mLastLocation = location;
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(true);
                }
            });

            if (mLastLocation != null) {
                fetchTimesForDate("0");
            } else {
                if (Build.MODEL.contains("google_sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.MODEL.contains("Android SDK")) {
                    ShowtimeApiManager api = new ShowtimeApiManager();
                    api.execute("33.8358", "-118.3406", "0", "Torrance,CA");
                } else if (mLastLocation != null) {
                    fetchTimesForDate("0");
                } else {
                    mRefreshLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mRefreshLayout.setRefreshing(false);
                        }
                    });
                    Toast.makeText(mApplicationContext, getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
                }
            }
        } catch (NullPointerException e){

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
            sendIntent.putExtra(Intent.EXTRA_TEXT, mSelectedMovie.name + "\n" + "http://google.com/movies?near=" + mCity + "&mid=" + mSelectedMovie.id);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_movie)));
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onRefresh() {
        refreshWithLocation(mLastLocation);
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
            TextView titleTextView = (TextView) itemView.findViewById(R.id.movie_title_textview);
            TextView detailsTextView = (TextView) itemView.findViewById(R.id.movie_details_textview);
//            ImageView movieImageView = (ImageView) itemView.findViewById(R.id.image_view);

            titleTextView.setText(mMovie.name);
            detailsTextView.setText(mMovie.description);
//            if (mMovie.poster != null) {
//                Picasso.with(mApplicationContext).load(mMovie.posterURLForDensity(mApplicationContext)).into(movieImageView);
//            }else {
//                movieImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
//            }
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
                    .inflate(R.layout.movie_list_item_layout, parent, false);
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
                addresses = geocoder.getFromLocation(Double.valueOf(lat), Double.valueOf(lon), 1);
                if (addresses.size() > 0) {
                    String address = addresses.get(0).getLocality();
                    if (addresses.get(0).getAdminArea() != null){
                        address += " " + addresses.get(0).getAdminArea();
                    }
                    mCity = URLEncoder.encode(address, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
            List<Movie> movies = null;

            if (mCity == null && isNetworkAvailable()) {
                mShowtimeService = ShowtimeService.adapter();
                try{
                    movies = mShowtimeService.listMovies(lat, lon, date, "");
                }catch(Exception e){

                }
                return movies;
            }

            mCacheKey = "movies_city_" + mCity + "_date_" + today.month + today.monthDay + today.year;

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

            if (movies == null && isNetworkAvailable()) {
                mShowtimeService = ShowtimeService.adapter();
                try {
                    movies = mShowtimeService.listMovies(lat, lon, date, mCity);
                }catch(Exception e){

                }
            }

            return movies;
        }

        @Override
        protected List<Movie> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2]);
        }

        @Override
        protected void onPostExecute(List<Movie> results) {
            if (results != null && results.size() > 0) {
                mMovieResults = results;
            }
            try {
                cacheResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(results);
        }

        public void cacheResults(List<Movie> results) throws IOException {
            if (results != null && mCacheKey != null) {
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
            if (result != null && result.size() > 0){
                mMovieAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                if (isAdded()) {
                    Toast.makeText(mApplicationContext, getString(R.string.no_movies_found), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
