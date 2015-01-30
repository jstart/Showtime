package com.truman.showtime.showtime;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by ctruman on 1/19/15.
 */
public class ShowtimeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread() {
            public void run() {
                Fabric.with(getApplicationContext(), new Crashlytics());
            }
        };
    }
}
