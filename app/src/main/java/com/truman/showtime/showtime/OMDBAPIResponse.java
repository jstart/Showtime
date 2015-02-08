package com.truman.showtime.showtime;

import java.io.Serializable;

/**
 * Created by ctruman on 2/6/15.
 */
public class OMDBAPIResponse implements Serializable {
    String Poster;

    String largePoster(){
        return Poster.replace("300.jpg", "2000.jpg");
    }
}
