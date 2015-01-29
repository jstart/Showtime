package com.truman.showtime.showtime;

import java.io.Serializable;
import java.util.List;

public class Theater implements Serializable {
    String id;
    String name;
    String address;
    String phoneNumber;
    List<Movie> movies;
}
