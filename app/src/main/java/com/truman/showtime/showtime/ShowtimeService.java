package com.truman.showtime.showtime;

import java.util.List;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;

public class ShowtimeService {
    private static final String API_URL = "https://showtime-server.herokuapp.com";

    interface Showtimes {
        @GET("/showtimes")
        List<Theater> listTheaters(
                @Query("lat") String lat,
                @Query("lon") String lon,
                @Query("date") String date,
                @Query("city") String city
        );
    }

    public static Showtimes adapter() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        Showtimes showtimeAdapter = restAdapter.create(Showtimes.class);
        return showtimeAdapter;
    }
}