package com.inuker.ble.library.utils;

import java.util.Random;

public class RandUtils {

    private static Random mRandom;

    static {
        mRandom = new Random(System.currentTimeMillis());
    }

    public static int nextInt(int bound) {
        return mRandom.nextInt(bound);
    }
}
