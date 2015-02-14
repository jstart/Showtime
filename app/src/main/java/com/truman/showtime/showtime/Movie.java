package com.truman.showtime.showtime;

import android.content.Context;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    String poster;
    String director;
    String description;
    List<Theater> theaters;

    public String youtubePreviewImageURL(){
        return "https://i.ytimg.com/vi/" + youtubeID() + "/hqdefault.jpg";
    }

    public String youtubeID() {
        String vId = null;
        Pattern pattern = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
        Matcher matcher = pattern.matcher(trailer);
        if (matcher.matches()){
            vId = matcher.group(1);
        }
        return vId;
    }
    public String imdbID() {
        return imdb.replaceFirst(".*/([^/?]+).*", "$1");
    }
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

    private static String getDensityName(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        if (density >= 4.0) {
            return "xxxhdpi";
        }
        if (density >= 3.0) {
            return "xxhdpi";
        }
        if (density >= 2.0) {
            return "xhdpi";
        }
        if (density >= 1.5) {
            return "hdpi";
        }
        if (density >= 1.0) {
            return "mdpi";
        }
        return "ldpi";
    }
    String posterURLForDensity(Context context){
        switch (getDensityName(context)) {
            case "ldpi":
                return poster;
            case "mdpi":
                return poster.replace("214_AL_.jpg", "6000.jpg");
            case "hdpi":
                return poster.replace("214_AL_.jpg", "800.jpg");
            case "xhdpi":
                return poster.replace("214_AL_.jpg", "1000.jpg");
            case "xxhdpi":
                return poster.replace("214_AL_.jpg", "1500.jpg");
            case "xxxhdpi":
                return poster.replace("214_AL_.jpg", "2000.jpg");
        }
        return poster;
    }

    String headerDescription() {
        String headerDescription = "";
        if (!genre.equalsIgnoreCase("false")){
            headerDescription += "Genre: " + genre;
        }
        if (!rating.equalsIgnoreCase("false")) {
            headerDescription += "\nRating: " + rating;
        }
        if (!rating.equalsIgnoreCase("false")) {
            runtime += "\nRuntime: " + runtime;
        }

        return headerDescription;
    }
}
