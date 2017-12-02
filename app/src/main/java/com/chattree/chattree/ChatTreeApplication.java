package com.chattree.chattree;

import android.app.Application;
import com.github.johnkil.print.PrintConfig;

import java.net.CookieHandler;
import java.net.CookieManager;

public class ChatTreeApplication extends Application {

    static private CookieManager cookieManager;


    @Override
    public void onCreate() {
        super.onCreate();
        PrintConfig.initDefault(getAssets(), "fonts/material-icon-font.ttf");

        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    public static CookieManager getCookieManager() {
        return cookieManager;
    }
}
