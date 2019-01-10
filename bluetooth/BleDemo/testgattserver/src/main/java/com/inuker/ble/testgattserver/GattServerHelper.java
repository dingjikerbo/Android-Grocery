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

import static com.inuker.ble.testgattserver.Constants.UUID_AUTH;
import static com.inuker.ble.testgattserver.Constants.UUID_MYSERVICE;
import static com.inuker.ble.testgattserver.Constants.UUID_NOTIFY;
import static com.inuker.ble.testgattserver.Constants.UUID_TOKEN;

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

        BluetoothGattCharacteristic characteristicToken = new BluetoothGattCharacteristic(UUID_TOKEN, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_NOTIFY, BluetoothGattDescriptor.PERMISSION_WRITE);
        characteristicToken.addDescriptor(descriptor);
        gattService.addCharacteristic(characteristicToken);

        BluetoothGattCharacteristic characteristicEvent = new BluetoothGattCharacteristic(UUID_AUTH, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        gattService.addCharacteristic(characteristicEvent);

        return gattService;
    }
}
