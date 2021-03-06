package com.truman.showtime.showtime.service;

import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.models.OMDBAPIResponse;
import com.truman.showtime.showtime.models.Theater;

import java.util.ArrayList;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public class ShowtimeService {
    private static final String API_URL = "http://showtime.ctruman.info:5000";
    private static final String OMDB_API_URL = "http://www.omdbapi.com";

    public interface Showtimes {
        @GET("/showtimes")
        ArrayList<Theater> listTheaters(
                @Query("lat") String lat,
                @Query("lon") String lon,
                @Query("date") String date,
                @Query("city") String city
        );

        @GET("/movies")
        ArrayList<Movie> listMovies(
                @Query("lat") String lat,
                @Query("lon") String lon,
                @Query("date") String date,
                @Query("city") String city
        );

        @GET("/movie/{mid}")
        Movie movieDetails(
                @Path("mid") String mid,
                @Query("lat") String lat,
                @Query("lon") String lon,
                @Query("date") String date,
                @Query("city") String city
        );
    }

    interface OMDBAPI {
        @GET("/")
        OMDBAPIResponse getResponse(
                @Query("i") String tmdbID,
                @Query("r") String format
        );
    }

    public static Showtimes adapter() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        Showtimes showtimeAdapter = restAdapter.create(Showtimes.class);
        return showtimeAdapter;
    }

    public static OMDBAPI omdbAdapter() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(OMDB_API_URL)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        OMDBAPI omdbAdapter = restAdapter.create(OMDBAPI.class);
        return omdbAdapter;
    }
}
