package com.truman.showtime.showtime;

import java.util.List;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public class ShowtimeService {
    private static final String API_URL = "https://showtime-server.herokuapp.com";
    private static final String OMDB_API_URL = "http://www.omdbapi.com";

    interface Showtimes {
        @GET("/showtimes")
        List<Theater> listTheaters(
                @Query("lat") String lat,
                @Query("lon") String lon,
                @Query("date") String date,
                @Query("city") String city
        );

        @GET("/movies")
        List<Movie> listMovies(
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
                .build();

        Showtimes showtimeAdapter = restAdapter.create(Showtimes.class);
        return showtimeAdapter;
    }

    public static OMDBAPI omdbAdapter() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(OMDB_API_URL)
                .build();

        OMDBAPI omdbAdapter = restAdapter.create(OMDBAPI.class);
        return omdbAdapter;
    }
}
