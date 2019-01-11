package com.inuker.ble.testgattserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;

import com.inuker.ble.library.utils.BluetoothUtils;

import static com.inuker.ble.library.Constants.UUID_MYSERVICE;
import static com.inuker.ble.library.Constants.UUID_NOTIFY;
import static com.inuker.ble.library.Constants.UUID_PACKET;

public class GattServerHelper {

    public static BluetoothLeAdvertiser createAdvertiser() {
        return BluetoothUtils.getAdvertiser();
    }

    public static AdvertiseSettings getDefaultAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();
    }

    public static AdvertiseData getDefaultAdvertiseData() {
        byte[] record = new byte[]{9, 7, 0, 6};
        return new AdvertiseData.Builder().addServiceData(new ParcelUuid(UUID_MYSERVICE), record).build();
    }

    public static BluetoothGattServer createGattServer(BluetoothGattServerCallback callback) {
        return BluetoothUtils.openGattServer(callback);
    }

    public static BluetoothGattService createGattService() {
        BluetoothGattService gattService = new BluetoothGattService(UUID_MYSERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristicPacket = new BluetoothGattCharacteristic(UUID_PACKET,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_NOTIFY, BluetoothGattDescriptor.PERMISSION_WRITE);
        characteristicPacket.addDescriptor(descriptor);
        gattService.addCharacteristic(characteristicPacket);

        return gattService;
    }
}
