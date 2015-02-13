package com.truman.showtime.showtime;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class MainActivity extends ActionBarActivity {
    private ShowtimePagerAdapter mDemoCollectionPagerAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private MixpanelAPI mMixpanel;
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
            mViewPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                        }
                    });
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
    }

    protected void onDestroy() {
        mMixpanel.flush();
        super.onDestroy();
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
                return getString(R.string.movies_tab_title);
            }else {
                return getString(R.string.theaters_tab_title);
            }
        }
    }
}
