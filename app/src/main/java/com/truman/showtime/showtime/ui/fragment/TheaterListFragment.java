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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.truman.showtime.showtime.models.Theater;
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

import static com.truman.showtime.showtime.service.ShowtimeService.Showtimes;

public class TheaterListFragment extends androidx.fragment.app.Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private TheaterAdapter mTheaterAdapter;
    private ArrayList<Theater> mTheaterResults;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private Showtimes mShowtimeService;
    private Location mLastLocation;
    private String mCity;
    private Theater mSelectedTheater;
    private Context mApplicationContext;
    private int mContextMenuPosition = RecyclerView.NO_POSITION;

    public TheaterListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTheaterResults = new ArrayList<Theater>();
        mTheaterAdapter = new TheaterAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));
        mRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listview);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mApplicationContext));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);

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
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void fetchTimesForDate(String date) {
        ShowtimeAPITask api = new ShowtimeAPITask();
        String lat = String.valueOf(mLastLocation.getLatitude());
        String lon = String.valueOf(mLastLocation.getLongitude());
        api.execute(lat, lon, date);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

            if (Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK")) {
                ShowtimeAPITask api = new ShowtimeAPITask();
                api.execute("33.8358", "-118.3406", "0", "Torrance,CA");
            } else if (location != null) {
                fetchTimesForDate("0");
            } else {
                if (location != null) {
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
        inflater.inflate(R.menu.theater_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getString(R.string.directions_theater)) && mContextMenuPosition != RecyclerView.NO_POSITION) {
            Theater selectedTheater = mTheaterResults.get(mContextMenuPosition);
            String theaterString = null;
            try {
                theaterString = URLEncoder.encode(selectedTheater.address, "UTF-8");
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + theaterString);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                if (mapIntent.resolveActivity(mApplicationContext.getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (item.getTitle().equals(getString(R.string.share_theater)) && mContextMenuPosition != RecyclerView.NO_POSITION) {
            Theater selectedTheater = mTheaterResults.get(mContextMenuPosition);
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, selectedTheater.name + "\n" + selectedTheater.address);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_theater)));
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onRefresh() {
        refreshWithLocation(mLastLocation);
    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Theater mTheater;

        public TheaterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
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
            mContextMenuPosition = getAdapterPosition();
            v.showContextMenu();
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
        protected ArrayList<Theater> getResponse(String lat, String lon, String date) {
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            Geocoder geocoder = new Geocoder(mApplicationContext);
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

            ArrayList<Theater> theaters = null;

            if (mCity == null && isNetworkAvailable()) {
                mShowtimeService = ShowtimeService.adapter();
                try{
                    theaters = mShowtimeService.listTheaters(lat, lon, date, "");
                }catch(Exception e){

                }
                return theaters;
            }

            mCacheKey = "theaters_city_" + mCity + "_date_" + today.month + today.monthDay + today.year;
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

            if (theaters == null && isNetworkAvailable()) {
                mShowtimeService = ShowtimeService.adapter();
                try{
                    theaters = mShowtimeService.listTheaters(lat, lon, date, mCity);
                }catch(Exception e){

                }
            }

            return theaters;
        }

        @Override
        protected ArrayList<Theater> doInBackground(String... arg0) {
            return getResponse(arg0[0], arg0[1], arg0[2]);
        }

        @Override
        protected void onPostExecute(ArrayList<Theater> results) {
            if (results != null && results.size() > 0) {
                mTheaterResults = results;
            }
            try {
                cacheResults(results);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(results);
        }

        public void cacheResults(ArrayList<Theater> results) throws IOException {
            if (results != null && mCacheKey != null) {
                File file = new File(mApplicationContext.getCacheDir(), mCacheKey);
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(results);
                os.close();
                fos.close();
            }
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
            if (result != null && result.size() > 0){
                mTheaterAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } else {
                mRefreshLayout.setRefreshing(false);
                if (isAdded()) {
                    Toast.makeText(mApplicationContext, getString(R.string.theaters_not_found), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
