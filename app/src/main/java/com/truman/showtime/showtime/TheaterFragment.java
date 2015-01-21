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
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
    ArrayList<ArrayList<String>> mTheaterResults;
    ArrayList<JSONObject> mTheaterDetailsResults;

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
    public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public SimpleDividerItemDecoration(Context context) {
            mDivider = context.getResources().getDrawable(R.drawable.line_divider);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTheaterResults = new ArrayList<ArrayList<String>>();
        mTheaterDetailsResults = new ArrayList<JSONObject>();
        mTheaterAdapter = new TheaterAdapter();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this);

        RecyclerView listView = (RecyclerView) rootView.findViewById(R.id.listview_theaters);
        listView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
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
//        api.parseAndReloadResults(contents);

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
        refreshWithLocation();
    }

    public void refreshWithLocation() {
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
        refreshWithLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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
            detailIntent.putExtra("TheaterDetails", mTheaterDetailsResults.get(index).toString());
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

    public class ShowtimeApiManager extends AsyncTask<String, String, String> {

        protected String getResponse(String lat, String lon, String city) {
            Log.d("", lat + lon);
            String result = null;

            try {
                String requestUri = String.format("https://showtime-server.herokuapp.com/showtimes?lat=%s&lon=%s&date=0", lat, lon);

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
            return getResponse(arg0[0], arg0[1], arg0[2]);
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
                    mTheaterDetailsResults.add(object);
                    ArrayList<String> fields = new ArrayList<>();
                    fields.add(object.getString("name"));
                    fields.add(object.getString("address"));

                    mTheaterResults.add(fields);
                }
                mTheaterAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
