package com.truman.showtime.showtime;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;


public class MainActivity extends ActionBarActivity {
    private ShowtimePagerAdapter mDemoCollectionPagerAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
//            getSupportActionBar().setElevation(10);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//            getSupportActionBar().setIcon(R.drawable.ic_launcher);
        }

        mDemoCollectionPagerAdapter =
                new ShowtimePagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new TheaterListFragment())
                    .commit();
        }
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                    }
                });
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
//        mSlidingTabLayout.setElevation(10);
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
//        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
//        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
//        // Create a tab listener that is called when the user changes tabs.
//        android.support.v7.app.ActionBar.TabListener tabListener = new android.support.v7.app.ActionBar.TabListener() {
//            @Override
//            public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                mViewPager.setCurrentItem(tab.getPosition());
//            }
//
//            @Override
//            public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//
//            }
//
//            @Override
//            public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//
//            }
//        };
//
//        for (int i = 0; i < 2; i++) {
//            String title = "";
//            switch (i) {
//                case 1:
//                    title = "Playing Nearby";
//                    break;
//                case 0:
//                    title = "Theaters";
//                    break;
//            }
//            actionBar.addTab(
//                    actionBar.newTab()
//                            .setText(title)
//                            .setTabListener(tabListener));
//        }
    }

    public class ShowtimePagerAdapter extends FragmentStatePagerAdapter {
        public ShowtimePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment;
            if (i == 1){
                fragment = new MovieListFragment();
            }else {
                fragment = new TheaterListFragment();

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
                return "Movies";
            }else {
                return "Theaters";
            }
        }
    }
}
