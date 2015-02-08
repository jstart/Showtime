package com.truman.showtime.showtime;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends android.support.v4.app.Fragment {

    private Movie mMovie;
    private ActionBar mToolbar;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TheaterAdapter mTheaterAdapter;

    private static final int HEADER = 0;
    private static final int SHOWTIME = 1;

    private ImageView mHeroImage;
    public MovieFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovie = (Movie) getActivity().getIntent().getSerializableExtra("MovieDetails");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_movie, container, false);

        mTheaterAdapter = new TheaterAdapter();

        mTheaterAdapter.mHeaderView = inflater.inflate(R.layout.include_movie_header, container, false);

        TextView titleView = (TextView)  mTheaterAdapter.mHeaderView.findViewById(R.id.titleView);
        titleView.setText(mMovie.name);

        TextView detailsView = (TextView)  mTheaterAdapter.mHeaderView.findViewById(R.id.detailsView);
        detailsView.setText("Genre: " + mMovie.genre + "\nRating: " + mMovie.rating + "\nRuntime: " + mMovie.runtime);

        TextView descriptionView = (TextView)  mTheaterAdapter.mHeaderView.findViewById(R.id.descriptionView);
        descriptionView.setText(mMovie.description);
        mRecyclerView = (RecyclerView) rootView;
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTheaterAdapter);
        mTheaterAdapter.notifyDataSetChanged();
        mToolbar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mToolbar != null){
            mToolbar.setTitle("");
            final ColorDrawable cd = new ColorDrawable(new Color().parseColor("#B71C1C"));
            cd.setAlpha(0);
            mToolbar.setBackgroundDrawable(cd);
            mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    cd.setAlpha(getAlphaforActionBar(recyclerView.getScrollY()));
                }

                private int getAlphaforActionBar(int scrollY) {
                    int minDist = 0, maxDist = 650;
                    if (scrollY > maxDist) {
                        return 255;
                    } else if (scrollY < minDist) {
                        return 0;
                    } else {
                        int alpha = 0;
                        alpha = (int) ((255.0 / maxDist) * scrollY);
                        return alpha;
                    }
                }
            });
        }

        mHeroImage = (ImageView)  mTheaterAdapter.mHeaderView.findViewById(R.id.imageView);
        if (mMovie.response != null){
            if (mMovie.response.Poster != null) {
                Picasso.with(getActivity()).setLoggingEnabled(true);
                Picasso.with(getActivity()).load(mMovie.response.largePoster()).into(mHeroImage);
            }
        }
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
                TextView addressTextView = (TextView) itemView.findViewById(R.id.list_item_theater_address_textview);

                titleTextView.setText(mTheater.name);
                addressTextView.setText(mTheater.address);
            }
        }

        @Override
        public void onClick(View v) {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            detailIntent.putExtra("Type", "Theater");
            detailIntent.putExtra("TheaterDetails", mTheater);
            startActivity(detailIntent);
        }

        @Override
        public boolean onLongClick(View v) {
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
}
