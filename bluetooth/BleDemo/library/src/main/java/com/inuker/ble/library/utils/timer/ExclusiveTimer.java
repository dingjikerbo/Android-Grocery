package com.inuker.ble.library.utils.timer;

import android.os.Handler;
import android.os.Looper;

import com.inuker.ble.library.proxy.ProxyInterceptor;
import com.inuker.ble.library.proxy.ProxyUtils;
import com.inuker.ble.library.utils.ContextUtils;

import java.lang.reflect.Method;

/**
 * Created by dingjikerbo on 2017/4/6.
 */

/**
 * 这个timer是排它的，即启动timer的时候会给之前的timer清除
 */
public class ExclusiveTimer {

    private TimerCallback mCallback;
    private Handler mHandler;

    public synchronized void stop() {
        abandonOldTimer();
    }

    /**
     * 在哪个线程起的timer超时了就必须在那个线程回调
     */
    public synchronized void start(int duration, String name, TimerCallback callback) {
        abandonOldTimer();
        Looper looper = Looper.myLooper();
        looper = looper != null ? looper : Looper.getMainLooper();
        mHandler = new Handler(looper);
        callback.setName(name);
        mHandler.postDelayed(callback, duration);
    }

    private void abandonOldTimer() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mCallback != null) {
            mCallback.onTimerCanceled();
            mCallback = null;
        }
    }
}
