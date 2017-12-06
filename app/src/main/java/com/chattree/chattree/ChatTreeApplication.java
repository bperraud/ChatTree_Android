package com.chattree.chattree;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import com.github.johnkil.print.PrintConfig;

import java.net.CookieHandler;
import java.net.CookieManager;

public class ChatTreeApplication extends Application {

    static private CookieManager cookieManager;

    // Content provider authority
    public static final String AUTHORITY    = "com.chattree.chattree.provider";
    // Account
    public static final String ACCOUNT      = "default_account";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "chattree.com";

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

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account getSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            return newAccount;
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            return accountManager.getAccountsByType(ACCOUNT_TYPE)[0];
            //Log.e("LoginActivity", "Error creating the sync account.");
            //return null;
        }
    }
}
