package com.truman.showtime.showtime.ui.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.ui.fragment.MovieListFragment;
import com.truman.showtime.showtime.ui.fragment.TheaterListFragment;
import com.truman.showtime.showtime.ui.view.SlidingTabLayout;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

public class MainActivity extends ActionBarActivity {
    private ShowtimePagerAdapter mDemoCollectionPagerAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private MixpanelAPI mMixpanel;
    private TheaterListFragment theaterListFragment;
    private MovieListFragment movieListFragment;

    private static final String MIXPANEL_TOKEN = "a6131725e7cc0ba03dd1bb423f9ba65a";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMixpanel =
                MixpanelAPI.getInstance(this, MIXPANEL_TOKEN);
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
                mMixpanel.track(String.valueOf(mDemoCollectionPagerAdapter.getPageTitle(position)), null);
                mViewPager.setCurrentItem(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        theaterListFragment = new TheaterListFragment();
        movieListFragment = new MovieListFragment();
    }

    protected void onDestroy() {
        mMixpanel.flush();
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        SmartLocation.with(this).location().stop();
    }

    protected void onResume() {
        super.onResume();

        if (Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK")) {
//            api.execute("33.8358", "-118.3406", "0", "Torrance,CA");
        }
        SmartLocation.with(this).location().oneFix()
                .provider(new LocationGooglePlayServicesWithFallbackProvider(this))
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        theaterListFragment.refreshWithLocation(location);
                        movieListFragment.refreshWithLocation(location);
                        Toast.makeText(getApplicationContext(), location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();
                    }
                });
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
