package com.truman.showtime.showtime.ui.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.android.youtube.player.YouTubeIntents;
import com.squareup.picasso.Picasso;
import com.truman.showtime.showtime.R;
import com.truman.showtime.showtime.service.ShowtimeService;
import com.truman.showtime.showtime.ui.view.SimpleDividerItemDecoration;
import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.models.Theater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends android.support.v4.app.Fragment implements ObservableScrollViewCallbacks, AdapterView.OnItemSelectedListener {

    private Movie mMovie;
    private ActionBar mToolbar;
    private ObservableRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TheaterAdapter mTheaterAdapter;
    private TextView mDescriptionView;
    private TextView mShowtimesTitleView;
    private LinearLayout mDetailsLayout;
    private ProgressBar mProgressBar;
    private Spinner mSpinner;
    private ShowtimeService.Showtimes mShowtimeService;
    private Theater mSelectedTheater;
    private Context mApplicationContext;

    public String mLat;
    public String mLon;
    public String mCity;

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
        mApplicationContext = getActivity().getApplicationContext();
        if (mMovie.theaters == null){
            mMovie.theaters = new ArrayList<Theater>();
        }
        setHasOptionsMenu(true);
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
        detailsView.setText(mMovie.headerDescription());
        mRecyclerView = (ObservableRecyclerView) rootView;
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mApplicationContext));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);
        mTheaterAdapter.notifyDataSetChanged();
        mDescriptionView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.descriptionView);
        mShowtimesTitleView = (TextView) mTheaterAdapter.mHeaderView.findViewById(R.id.showtimesTitleView);
        mSeparatorView = (View) mTheaterAdapter.mHeaderView.findViewById(R.id.separator);
        mDetailsLayout = (LinearLayout) mTheaterAdapter.mHeaderView.findViewById(R.id.detail_layout);
        mProgressBar = (ProgressBar) mTheaterAdapter.mHeaderView.findViewById(R.id.progress_bar);

        mSpinner = (Spinner) mTheaterAdapter.mHeaderView.findViewById(R.id.today_spinner);
        List<String> list = new ArrayList<String>(Arrays.asList("Today", "Tomorrow", "Friday"));
        ArrayAdapter adapter = new ArrayAdapter(mApplicationContext, R.layout.spinner_item, list);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);
        mSpinner.getBackground().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_ATOP);

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
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mToolbar.setTitle("");
                cd = new ColorDrawable(getResources().getColor(R.color.primary_dark));
                mToolbar.setBackgroundDrawable(cd);
                cd.setAlpha(0);
                mRecyclerView.setScrollViewCallbacks(this);
            } else {
                mToolbar.setTitle(mMovie.name);
            }
        }

        mHeroImage = (ImageView)  mTheaterAdapter.mHeaderView.findViewById(R.id.imageView);
        ImageButton playButton = (ImageButton)  mTheaterAdapter.mHeaderView.findViewById(R.id.play_button);
        if (!mMovie.trailer.equals("false")) {
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (YouTubeIntents.canResolvePlayVideoIntent(mApplicationContext)) {
                        Intent youtubeIntent = YouTubeIntents.createPlayVideoIntent(mApplicationContext, mMovie.youtubeID());
                        youtubeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        startActivity(youtubeIntent);
                    } else {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        i.setData(Uri.parse(mMovie.trailer));
                        startActivity(i);
                    }
                }
            };
            mHeroImage.setOnClickListener(clickListener);
            playButton.setOnClickListener(clickListener);
        } else {
            playButton.setVisibility(View.INVISIBLE);
        }

        if (mMovie.poster != null) {
            Picasso.with(mApplicationContext).setLoggingEnabled(true);
            Picasso.with(mApplicationContext).load(mMovie.posterURLForDensity(mApplicationContext)).into(mHeroImage);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate menu resource file.
        inflater.inflate(R.menu.movie, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() == getString(R.string.share_movie)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mMovie.name + "\n" + "http://google.com/movies?near=" + mCity + "&mid=" + mMovie.id);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_movie)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.showtimes_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
        // Handle context actions
        if (item.getTitle().equals(getString(R.string.directions_theater))){
            // Create a Uri from an intent string. Use the result to create an Intent.
            String theaterString = null;
            try {
                theaterString = URLEncoder.encode(mSelectedTheater.address, "UTF-8");
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + theaterString);
                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");
                // Attempt to start an activity that can handle the Intent
                if (mapIntent.resolveActivity(mApplicationContext.getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (item.getTitle().equals(getString(R.string.share_showtimes))){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mMovie.name + "\n" + mSelectedTheater.name + "\n" + mSelectedTheater.address + "\n" + mSelectedTheater.showtimesString() + "\nhttp://google.com/movies?near=" + mCity + "&mid=" + mMovie.id);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_showtimes)));
        } else if (item.getTitle().equals(getString(R.string.buy_tickets))){

        }
        return super.onContextItemSelected(item);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class TheaterHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Theater mTheater;

        public TheaterHolder(View itemView, int pos) {
            super(itemView);
            if (pos != HEADER) {
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
                registerForContextMenu(itemView);
            }
        }

        public void bindTheater(Theater theater) {
            mTheater = theater;
            TextView titleTextView = (TextView) itemView.findViewById(R.id.list_item_theater_textview);
            TextView showtimeTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

            titleTextView.setText(mTheater.name);
            showtimeTextView.setText(mTheater.showtimesString());
        }

        @Override
        public void onClick(View v) {
            mSelectedTheater = mTheater;
            getActivity().openContextMenu(v);
        }

        @Override
        public boolean onLongClick(View v) {
            mSelectedTheater = mTheater;
            getActivity().openContextMenu(v);
            return true;
        }
    }

    private class TheaterAdapter
            extends RecyclerView.Adapter<TheaterHolder> {
        public View mHeaderView;

        @Override
        public TheaterHolder onCreateViewHolder(ViewGroup parent, int pos) {
            if (pos == HEADER) {
                return new TheaterHolder(mHeaderView, pos);
            }
            else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_layout, parent, false);
                return new TheaterHolder(view, pos);
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
            mCacheKey = mMovie.id + mCity + today.month + today.monthDay + today.year;
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
            if (movie != null) {
                File file = new File(mApplicationContext.getCacheDir(), mCacheKey);
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(movie);
                os.close();
                fos.close();
            }
        }

        public Movie cachedResultsForKey(String cacheKey) throws IOException, ClassNotFoundException {
            File file = new File(mApplicationContext.getCacheDir(), cacheKey);
            Movie movie = null;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream is = new ObjectInputStream(fis);
                movie = (Movie) is.readObject();
                is.close();
                fis.close();
            }
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
                        Picasso.with(mApplicationContext).setLoggingEnabled(true);
                        Picasso.with(mApplicationContext).load(mMovie.posterURLForDensity(mApplicationContext)).into(mHeroImage);
                    }
                } else {
                    ((LinearLayout)mProgressBar.getParent()).removeView(mProgressBar);
                    Toast.makeText(mApplicationContext, getString(R.string.movie_details_error), Toast.LENGTH_LONG).show();
                }
            } else {
                ((LinearLayout)mProgressBar.getParent()).removeView(mProgressBar);
                Toast.makeText(mApplicationContext, getString(R.string.movie_details_error), Toast.LENGTH_LONG).show();
            }
        }
    }
}
