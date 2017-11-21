package com.chattree.chattree;

import android.app.Application;
import com.github.johnkil.print.PrintConfig;

public class ChatTreeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PrintConfig.initDefault(getAssets(), "fonts/material-icon-font.ttf");
    }

}
