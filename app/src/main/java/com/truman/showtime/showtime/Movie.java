package com.truman.showtime.showtime;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ctruman on 1/28/15.
 */

public class Movie implements Serializable {
    String id;
    String name;
    String runtime;
    String rating;
    String genre;
    String imdb;
    String trailer;
    List<String> showtimes;
}
