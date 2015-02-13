package com.truman.showtime.showtime;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

public class DetailActivity extends ActionBarActivity {
    String mLat;
    String mLon;
    String mCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            String type = (String) getIntent().getSerializableExtra("Type") != null ? (String) getIntent().getSerializableExtra("Type") : "";
            mLat = (String) getIntent().getSerializableExtra("Lat") != null ? (String) getIntent().getSerializableExtra("Lat") : "";
            mLon = (String) getIntent().getSerializableExtra("Lon") != null ? (String) getIntent().getSerializableExtra("Lon") : "";
            mCity = (String) getIntent().getSerializableExtra("City") != null ? (String) getIntent().getSerializableExtra("City") : "";

            if (type.equals("Theater")) {
                MoviesForTheaterFragment movieFragment = new MoviesForTheaterFragment();
                movieFragment.mLat = mLat;
                movieFragment.mLon = mLon;
                movieFragment.mCity = mCity;
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, movieFragment)
                        .commit();
            } else {
                MovieFragment movieFragment = new MovieFragment();
                movieFragment.mLat = mLat;
                movieFragment.mLon = mLon;
                movieFragment.mCity = mCity;
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, movieFragment)
                        .commit();
            }
        }
    }
}
