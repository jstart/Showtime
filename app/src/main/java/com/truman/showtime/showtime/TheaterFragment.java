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

import android.app.Fragment;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TheaterFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    TheaterAdapter mTheaterAdapter;
    ArrayList<String> mTheaterResults;
    SwipeRefreshLayout mRefreshLayout;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    public TheaterFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.theaterfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTheaterResults = new ArrayList<String>();
        mTheaterAdapter = new TheaterAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);

        RecyclerView listView = (RecyclerView) rootView.findViewById(R.id.listview_theaters);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setAdapter(mTheaterAdapter);

//        File cacheDir = getActivity().getCacheDir();
//        File file = new File(cacheDir, "90504");
//        int length = (int) file.length();
//
//        byte[] bytes = new byte[length];
//
//        FileInputStream in = null;
//        try {
//            in = new FileInputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            in.read(bytes);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                in.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        String contents = new String(bytes);
        ShowtimeApiManager api = new ShowtimeApiManager();
//        api.parseAndReloadResults(contents);

        api.execute("");
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        return rootView;
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
                api.execute(lat, lon, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRefresh() {
        ShowtimeApiManager api = new ShowtimeApiManager();
        api.execute("");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private String mTheater;

        public TheaterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        public void bindTheater(String theater) {
            mTheater = theater;
            TextView textView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            textView.setText(mTheater);
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            detailIntent.putExtra(Intent.EXTRA_TEXT, mTheater);
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
            String crime = mTheaterResults.get(pos);
            holder.bindTheater(crime);
        }

        @Override
        public int getItemCount() {
            return mTheaterResults.size();
        }
    }

    public class ShowtimeApiManager extends AsyncTask<String, String, String> {

        protected String getResponse(String parameters) {
            Log.d("", parameters);
            String result = null;

            try {
                String requestUri = "https://showtime-server.herokuapp.com/showtimes?lat=33.8358&lon=-118.3406&date=0&zipcode=90504";

                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(requestUri));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                result = out.toString();
            } catch (Exception e) {

                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected String doInBackground(String... arg0) {
            return getResponse(arg0[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mTheaterResults.clear();
            parseAndReloadResults(result);
        }

        public void parseAndReloadResults(String result){
            try {
                JSONArray jsonArray = new JSONArray(result);

//                File cacheDir = getActivity().getCacheDir();
//                File f = new File(cacheDir, "90504");
//
//                try {
//                    FileOutputStream out = new FileOutputStream(
//                            f);
//                    out.write(result.getBytes());
//                    out.close();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                for (int i = 0; i < jsonArray.length(); i++){
                    JSONObject object = jsonArray.getJSONObject(i);
                    mTheaterResults.add(object.getString("name"));
                }
                mTheaterAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
