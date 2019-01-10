package com.inuker.ble.testgattserver;

import com.inuker.ble.library.utils.UUIDUtils;

import java.util.UUID;

public class Constants {

    public static final int UUID_VAL_MYSERVICE = 0xA7C9;
    public static final int UUID_VAL_TOKEN = 0x01;
    public static final int UUID_VAL_AUTH = 0x02;

    public static final UUID UUID_MYSERVICE = UUIDUtils.makeUUID(UUID_VAL_MYSERVICE);
    public static final UUID UUID_TOKEN = UUIDUtils.makeUUID(UUID_VAL_TOKEN);
    public static final UUID UUID_AUTH = UUIDUtils.makeUUID(UUID_VAL_AUTH);
    public static final UUID UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final String UUID_FORMAT = "0000%04x-0000-1000-8000-00805f9b34fb";
}
