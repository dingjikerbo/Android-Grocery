package com.inuker.ble.library.utils.timer;

import com.inuker.ble.library.utils.LogUtils;

import java.util.concurrent.TimeoutException;

public abstract class TimerCallback implements Runnable {

    private String mName;

    public void setName(String name) {
        mName = name;
    }

    @Override
    public final void run() {
        LogUtils.e(String.format("%s: Timer expired!!!", mName));
        try {
            onTimerCallback();
        } catch (TimeoutException e) {
            LogUtils.e(e);
        }
    }

    public abstract void onTimerCallback() throws TimeoutException;

    public void onTimerCanceled() {
        // do nothing here
    }
}
