package com.truman.showtime.showtime.models;

import android.content.Context;

import java.io.Serializable;

/**
 * Created by ctruman on 2/6/15.
 */
public class OMDBAPIResponse implements Serializable {
    String Poster;
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
                return Poster;
            case "mdpi":
                return Poster.replace("300.jpg", "6000.jpg");
            case "hdpi":
                return Poster.replace("300.jpg", "800.jpg");
            case "xhdpi":
                return Poster.replace("300.jpg", "1000.jpg");
            case "xxhdpi":
                return Poster.replace("300.jpg", "1500.jpg");
            case "xxxhdpi":
                return Poster.replace("300.jpg", "2000.jpg");
        }
        return Poster;
    }
}
