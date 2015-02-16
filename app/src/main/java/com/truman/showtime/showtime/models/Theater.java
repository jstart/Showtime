package com.truman.showtime.showtime.models;

import java.io.Serializable;
import java.util.List;

public class Theater implements Serializable {
    String id;
    String name;
    String address;
    String phoneNumber;
    List<Movie> movies;
    List<String> showtimes;

    public String showtimesString() {
        if(showtimes == null)
            return "";
        String showtimesList = "";
        for (int i = 0; i < showtimes.size(); i++) {
            String row = showtimes.get(i);
            showtimesList += (row);
            if (i < showtimes.size() - 1){
                showtimesList += ", ";
            }
        }
        return showtimesList;
    }
}
