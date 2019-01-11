package com.inuker.ble.testgattclient;

import android.app.Application;
import android.content.Context;

import com.inuker.ble.library.utils.ContextUtils;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ContextUtils.setContext(this);
    }

}
