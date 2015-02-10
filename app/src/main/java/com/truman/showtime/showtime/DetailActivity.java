package com.truman.showtime.showtime;

import android.location.Address;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

public class DetailActivity extends ActionBarActivity {
    String mLat;
    String mLon;
    String mCity;
    Address mAddress;

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
            mAddress = (Address) getIntent().getSerializableExtra("Address");
            mCity = (String) getIntent().getSerializableExtra("City") != null ? (String) getIntent().getSerializableExtra("City") : "";

            if (type.equals("Theater")) {
                MoviesForTheaterFragment movieFragment = new MoviesForTheaterFragment();
                movieFragment.mLat = mLat;
                movieFragment.mLon = mLon;
                movieFragment.mCity = mCity;
                movieFragment.mAddress = mAddress;
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, movieFragment)
                        .commit();
            } else {
                MovieFragment movieFragment = new MovieFragment();
                movieFragment.mLat = mLat;
                movieFragment.mLon = mLon;
                movieFragment.mCity = mCity;
                movieFragment.mAddress = mAddress;
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, movieFragment)
                        .commit();
            }
        }
    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.detail, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            Intent settingsIntent = new Intent(this, SettingsActivity.class);
//            startActivity(settingsIntent);
//        }
//        return super.onOptionsItemSelected(item);
//    }

}
