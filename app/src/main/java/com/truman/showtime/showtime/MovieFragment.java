package com.truman.showtime.showtime;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by ctruman on 1/21/15.
 */
public class MovieFragment extends Fragment {
        MovieAdapter mMovieAdapter;
        ArrayList<JSONObject> mMovieResults;

        SwipeRefreshLayout mRefreshLayout;

        public MovieFragment() {
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
            String jsonString = getActivity().getIntent().getStringExtra("TheaterDetails");
            JSONObject theater;

            try {
                theater = new JSONObject(jsonString);

                ArrayList<JSONObject> listdata = new ArrayList<JSONObject>();
                JSONArray jArray = theater.getJSONArray("movies");
                if (jArray != null) {
                    for (int i=0;i<jArray.length();i++){
                        try {
                            listdata.add(jArray.getJSONObject(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mMovieResults = new ArrayList<JSONObject>(listdata);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mMovieAdapter = new MovieAdapter();

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            RecyclerView listView = (RecyclerView) rootView.findViewById(R.id.listview_movies);
            listView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
            listView.setLayoutManager(new LinearLayoutManager(getActivity()));
            listView.setAdapter(mMovieAdapter);
            return rootView;
        }

        private class MovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private JSONObject mMovie;

            public MovieHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            public void bindMovie(JSONObject movieObject) {
                mMovie = movieObject;
                TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
                TextView showtimeTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

                try {
                    titleTextView.setText(mMovie.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    JSONArray showtimes = mMovie.getJSONArray("showtimes");
                    String showtimesList = "";
                    for (int i = 0; i < showtimes.length(); i++) {
                        String row = showtimes.optString(i);
                        showtimesList += (row);
                        if (i < showtimes.length() - 1){
                            showtimesList += ", ";
                        }
                    }
                    showtimeTextView.setText(showtimesList);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClick(View v) {
//                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
//                int index = mMovieResults.indexOf(mMovie);
//                detailIntent.putExtra("TheaterDetails", mTheaterDetailsResults.get(index).toString());
//                startActivity(detailIntent);
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
                JSONObject movie = mMovieResults.get(pos);
                holder.bindMovie(movie);
            }

            @Override
            public int getItemCount() {
                return mMovieResults.size();
            }
        }

}