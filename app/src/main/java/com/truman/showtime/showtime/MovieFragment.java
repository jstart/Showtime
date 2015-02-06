package com.truman.showtime.showtime;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends android.support.v4.app.Fragment {

    private Movie mMovie;
    private ActionBar mToolbar;
    public MovieFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovie = (Movie) getActivity().getIntent().getSerializableExtra("MovieDetails");
        mToolbar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        mToolbar.setTitle(mMovie.name);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_movie, container, false);

        final ColorDrawable cd = new ColorDrawable(new Color().parseColor("#B71C1C"));
        mToolbar.setBackgroundDrawable(cd);

        cd.setAlpha(0);
        ScrollViewX scrollView = (ScrollViewX) rootView.findViewById(R.id.scrollview);
        scrollView.setOnScrollViewListener(new ScrollViewX.OnScrollViewListener() {

            @Override
            public void onScrollChanged(ScrollViewX v, int l, int t, int oldl, int oldt) {
                cd.setAlpha(getAlphaforActionBar(v.getScrollY()));
            }

            private int getAlphaforActionBar(int scrollY) {
                int minDist = 0,maxDist = 650;
                if(scrollY>maxDist){
                    return 255;
                }
                else if(scrollY<minDist){
                    return 0;
                }
                else {
                    int alpha = 0;
                    alpha = (int)  ((255.0/maxDist)*scrollY);
                    return alpha;
                }
            }
        });
        return rootView;
    }

}
