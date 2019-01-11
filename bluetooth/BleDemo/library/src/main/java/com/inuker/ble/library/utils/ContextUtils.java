package com.inuker.ble.library.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ContextUtils {

    private static Context mContext;

    private static Handler mHandler;

    public static void setContext(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static Context getContext() {
        return mContext;
    }

    public static void post(Runnable r) {
        mHandler.post(r);
    }

    public static String getCurrentMethodName() {
        StackTraceElement e = Thread.currentThread().getStackTrace()[4];
        return e.getMethodName();
    }

    public static void assertRuntime(boolean mainThread) {
        if (mainThread && Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException();
        }
        if (!mainThread && Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException();
        }
    }
}
