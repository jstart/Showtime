package com.truman.showtime.showtime.ui.activity;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.view.MenuItemCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.ui.fragment.MovieListFragment;
import com.truman.showtime.showtime.ui.fragment.TheaterListFragment;
import com.truman.showtime.showtime.ui.view.SlidingTabLayout;

import java.io.IOException;
import java.util.List;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

public class MainActivity extends AppCompatActivity {
    private ShowtimePagerAdapter mDemoCollectionPagerAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private TheaterListFragment theaterListFragment;
    private MovieListFragment movieListFragment;
    private SearchView mSearchView;
    private Location mLastLocation;

    private static final String MIXPANEL_TOKEN = "a6131725e7cc0ba03dd1bb423f9ba65a";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        mDemoCollectionPagerAdapter =
                new ShowtimePagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setSelectedIndicatorColors(Color.WHITE);
        mSlidingTabLayout.setBackgroundColor(getResources().getColor(R.color.primary_dark));
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mViewPager.setCurrentItem(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        theaterListFragment = new TheaterListFragment();
        movieListFragment = new MovieListFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search);

        mSearchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setQueryHint(getString(R.string.location_hint));
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Geocoder geocoder = new Geocoder(getApplicationContext());
                mSearchView.clearFocus();

                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocationName(s, 1);
                    if (addresses.size() > 0 && addresses.get(0).hasLatitude()) {
                        Location location = new Location("");
                        location.setLatitude(addresses.get(0).getLatitude());
                        location.setLongitude(addresses.get(0).getLongitude());

                        movieListFragment.refreshWithLocation(location);
                        theaterListFragment.refreshWithLocation(location);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    protected void onDestroy() {
        SmartLocation.with(this).location().stop();
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        SmartLocation.with(this).location().stop();
    }

    protected void onResume() {
        super.onResume();
        try {
            SmartLocation.with(this).location().oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        if (mLastLocation == null || (mLastLocation.getLatitude() != location.getLatitude()
                                && mLastLocation.getLongitude() != location.getLongitude())) {
                            mLastLocation = location;
                            theaterListFragment.refreshWithLocation(location);
                            movieListFragment.refreshWithLocation(location);
                        }
                    }
                });
        } catch (IllegalArgumentException e){
            Toast.makeText(getApplicationContext(), getString(R.string.location_services_disabled), Toast.LENGTH_LONG).show();
        }
    }

    public class ShowtimePagerAdapter extends FragmentStatePagerAdapter {
        public ShowtimePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment;
            if (i == 1){
                fragment = movieListFragment;
            }else {
                fragment = theaterListFragment;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 1){
                return getString(R.string.movies_tab_title);
            }else {
                return getString(R.string.theaters_tab_title);
            }
        }
    }
}
