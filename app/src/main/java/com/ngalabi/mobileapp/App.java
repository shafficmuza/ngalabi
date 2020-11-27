package com.ngalabi.mobileapp;

import androidx.multidex.MultiDexApplication;
import com.onesignal.OneSignal;

public class App extends MultiDexApplication {


    @Override public void onCreate() {
        super.onCreate();

// Logging set to help debug issues, remove before releasing your app.
       // OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

    }


} 