package com.truman.showtime.showtime;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.android.youtube.player.YouTubeIntents;
import com.squareup.picasso.Picasso;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends android.support.v4.app.Fragment implements ObservableScrollViewCallbacks {

    private Movie mMovie;
    private ActionBar mToolbar;
    private ObservableRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TheaterAdapter mTheaterAdapter;
    private TextView mDescriptionView;
    private TextView mShowtimesTitleView;
    private LinearLayout mDetailsLayout;
    private ProgressBar mProgressBar;
    private ShowtimeService.Showtimes mShowtimeService;

    String mLat;
    String mLon;
    String mCity;
    Address mAddress;

    private View mSeparatorView;
    private ColorDrawable cd;

    private static final int HEADER = 0;
    private static final int SHOWTIME = 1;

    private ImageView mHeroImage;
    public MovieFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovie = (Movie) getActivity().getIntent().getSerializableExtra("MovieDetails");
        if (mMovie.theaters == null){
            mMovie.theaters = new ArrayList<Theater>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movie, container, false);
        mTheaterAdapter = new TheaterAdapter();

        mTheaterAdapter.mHeaderView = inflater.inflate(R.layout.include_movie_header, container, false);

        TextView titleView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.titleView);
        titleView.setText(mMovie.name);
        TextView detailsView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.detailsView);
        detailsView.setText("Genre: " + mMovie.genre + "\nRating: " + mMovie.rating + "\nRuntime: " + mMovie.runtime);
        mRecyclerView = (ObservableRecyclerView) rootView;
        mRecyclerView.setScrollViewCallbacks(this);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);
        mTheaterAdapter.notifyDataSetChanged();
        mDescriptionView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.descriptionView);
        mShowtimesTitleView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.showtimesTitleView);
        mSeparatorView = (View) mTheaterAdapter.mHeaderView.findViewById(R.id.separator);
        mDetailsLayout = (LinearLayout) mTheaterAdapter.mHeaderView.findViewById(R.id.detail_layout);
        mProgressBar = (ProgressBar) mTheaterAdapter.mHeaderView.findViewById(R.id.progress_bar);

        if (mMovie.description != null) {
            mDescriptionView.setText(mMovie.description);
            ((LinearLayout)mProgressBar.getParent()).removeView(mProgressBar);
        } else if (mMovie.id != null) {
            mDescriptionView.setAlpha(0);
            mShowtimesTitleView.setAlpha(0);
            mSeparatorView.setAlpha(0);
            mProgressBar.setIndeterminate(true);
            mProgressBar.setVisibility(View.VISIBLE);
        }
        if (mMovie.poster == null && mMovie.id != null){
            MovieDetailsTask movieDetailsTask = new MovieDetailsTask();
            movieDetailsTask.execute();
        }
        mToolbar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mToolbar != null){
            mToolbar.setTitle("");
            cd = new ColorDrawable(new Color().parseColor("#B71C1C"));
            mToolbar.setBackgroundDrawable(cd);
            cd.setAlpha(0);
        }

        mHeroImage = (ImageView)  mTheaterAdapter.mHeaderView.findViewById(R.id.imageView);
        ImageButton playButton = (ImageButton)  mTheaterAdapter.mHeaderView.findViewById(R.id.play_button);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMovie.trailer != null) {
                    Intent youtubeIntent = YouTubeIntents.createPlayVideoIntent(getActivity().getApplicationContext(), mMovie.youtubeID());
                    startActivity(youtubeIntent);
                }
            }
        };
        mHeroImage.setOnClickListener(clickListener);
        playButton.setOnClickListener(clickListener);

        if (mMovie.poster != null) {
            Picasso.with(getActivity()).setLoggingEnabled(true);
            Picasso.with(getActivity()).load(mMovie.posterURLForDensity(getActivity().getApplicationContext())).into(mHeroImage);
        }
    }

    @Override
    public void onScrollChanged(int i, boolean b, boolean b2) {
            cd.setAlpha(getAlphaForActionBar(i));
    }

    private int getAlphaForActionBar(int scrollY) {
        int minDist = 0, maxDist = 650;
        if (scrollY > maxDist) {
            mToolbar.setTitle(mMovie.name);
            return 255;
        } else if (scrollY < minDist) {
            return 0;
        } else {
            int alpha = 0;
            alpha = (int) ((255.0 / maxDist) * scrollY);
            mToolbar.setTitle("");
            return alpha;
        }
    }
    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Theater mTheater;

        public TheaterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            registerForContextMenu(itemView);
        }

        public void bindTheater(Theater theater) {
            mTheater = theater;
            if (itemView.getClass().equals(LinearLayout.class)){
                TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
                TextView showtimeTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

                titleTextView.setText(mTheater.name);
                showtimeTextView.setText(mTheater.showtimesString());
            }
        }

        @Override
        public void onClick(View v) {
//            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
//            detailIntent.putExtra("Type", "Theater");
//            detailIntent.putExtra("TheaterDetails", mTheater);
//            startActivity(detailIntent);
        }

        @Override
        public boolean onLongClick(View v) {
//            getActivity().openContextMenu(v);
            return true;
        }
    }

    private class TheaterAdapter
            extends RecyclerView.Adapter<TheaterHolder> {
        public View mHeaderView;

        @Override
        public TheaterHolder onCreateViewHolder(ViewGroup parent, int pos) {

            if (pos == HEADER) {
                return new TheaterHolder(mHeaderView);
            }
            else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_layout, parent, false);
                return new TheaterHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(TheaterHolder holder, int pos) {
            if (pos == HEADER) {
                return;
            }
            Theater theater = mMovie.theaters.get(pos-1);
            holder.bindTheater(theater);
        }

        @Override
        public int getItemCount() {
            return mMovie.theaters.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == HEADER)
                return HEADER;
            else
                return SHOWTIME;
        }
    }

    public class MovieDetailsTask extends AsyncTask<String, String, Movie> {
        String mCacheKey;
        protected Movie getResponse() {
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            mCacheKey = "movie:mid:" + mMovie.id + ":city:" + mCity + ":date:" + today.month + today.monthDay + today.year;
            String result = null;
            Movie movie = null;
            try {
                movie = cachedResultsForKey(mCacheKey);
            } catch (IOException e) {
//                e.printStackTrace();
                Log.d("Showtime", "movies ioexception miss");
            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
                Log.d("Showtime", "movies class not found miss");

            }

            if (movie == null) {
                mShowtimeService = ShowtimeService.adapter();
                movie = mShowtimeService.movieDetails(mMovie.id, mLat, mLon, "0", mCity);
            }

            return movie;
        }

        @Override
        protected Movie doInBackground(String... arg0) {
            return getResponse();
        }

        @Override
        protected void onPostExecute(Movie movie) {
            mMovie = movie;
            try {
                cacheResults(movie);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parseAndReloadResults(movie);
        }

        public void cacheResults(Movie movie) throws IOException {
            FileOutputStream fos = getActivity().getApplicationContext().openFileOutput(mCacheKey, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(movie);
            os.close();
            fos.close();
        }

        public Movie cachedResultsForKey(String cacheKey) throws IOException, ClassNotFoundException {
            FileInputStream fis = getActivity().getApplicationContext().openFileInput(cacheKey);
            ObjectInputStream is = new ObjectInputStream(fis);
            Movie movie = (Movie) is.readObject();
            is.close();
            fis.close();
            return movie;
        }

        public void parseAndReloadResults(Movie movie){
            if (movie != null) {
                if (movie.theaters.size() > 0){
                    mTheaterAdapter.notifyDataSetChanged();
                    if (mProgressBar.getParent() != null) {
                        ((LinearLayout) mProgressBar.getParent()).removeView(mProgressBar);
                    }
                    mDescriptionView.setAlpha(1);
                    mDescriptionView.setText(movie.description);
                    mShowtimesTitleView.setAlpha(1);
                    mSeparatorView.setAlpha(1);
                    if (mMovie.poster != null) {
                        Picasso.with(getActivity()).setLoggingEnabled(true);
                        Picasso.with(getActivity()).load(mMovie.posterURLForDensity(getActivity().getApplicationContext())).into(mHeroImage);
                    }
                } else {
                    ((LinearLayout)mProgressBar.getParent()).removeView(mProgressBar);
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.movie_details_error), Toast.LENGTH_LONG).show();
                }
            } else {
                ((LinearLayout)mProgressBar.getParent()).removeView(mProgressBar);
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.movie_details_error), Toast.LENGTH_LONG).show();
            }
        }
    }
}
