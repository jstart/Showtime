package com.truman.showtime.showtime.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.ui.view.SimpleDividerItemDecoration;
import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.models.Theater;
import com.truman.showtime.showtime.ui.activity.DetailActivity;

import java.util.ArrayList;

/**
 * Created by ctruman on 1/21/15.
 */
public class MoviesForTheaterFragment extends android.support.v4.app.Fragment {
    MovieAdapter mMovieAdapter;
    ArrayList<Movie> mMovieResults;
    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;
    SwipeRefreshLayout mRefreshLayout;
    String mLat;
    String mLon;
    String mCity;

    public MoviesForTheaterFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mMovieAdapter = new MovieAdapter();

            View rootView = inflater.inflate(R.layout.fragment_movies_for_theater, container, false);
            mLayoutManager = new LinearLayoutManager(getActivity());
            RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listview);
            mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);

            mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mMovieAdapter);

            Theater theater = (Theater) getActivity().getIntent().getSerializableExtra("TheaterDetails");

            if (theater == null){
                mMovieResults = new ArrayList<Movie>();
                return rootView;
            }
            mRefreshLayout.setEnabled(false);
            mMovieResults = new ArrayList<Movie>(theater.movies);

            ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
            actionBar.setTitle(theater.name);

            return rootView;
        }

    private class MovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private Movie mMovie;

            public MovieHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            public void bindMovie(Movie movieObject) {
                mMovie = movieObject;
                TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
                TextView showtimeTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);
                titleTextView.setText(mMovie.name);

                showtimeTextView.setText(mMovie.showtimesString());
            }

            @Override
            public void onClick(View v) {
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra("MovieDetails", mMovie);
                detailIntent.putExtra("Type", "Movie");

                detailIntent.putExtra("Lat", mLat);
                detailIntent.putExtra("Lon", mLon);
                detailIntent.putExtra("City", mCity);

                startActivity(detailIntent);
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
}
