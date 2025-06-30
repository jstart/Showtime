package com.truman.showtime.showtime.service;

import com.truman.showtime.showtime.models.Movie;
import com.truman.showtime.showtime.models.OMDBAPIResponse;
import com.truman.showtime.showtime.models.Theater;

import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ShowtimeService {
    // TODO: Consider moving API_URL to a configuration file or BuildConfig for flexibility.
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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(Showtimes.class);
    }

    public static OMDBAPI omdbAdapter() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OMDB_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(OMDBAPI.class);
    }
}
