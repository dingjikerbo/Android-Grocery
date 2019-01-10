package com.inuker.ble.library.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;

import java.util.List;
import java.util.UUID;

public class BluetoothUtils {

    public static int getBluetoothConnectionState(BluetoothDevice device) {
        return getBluetoothManager().getConnectionState(device, BluetoothProfile.GATT);
    }

    public static int getBluetoothConnectionState(String mac) {
        BluetoothDevice device = getBluetoothDevice(mac);
        return device != null ? getBluetoothConnectionState(device) : -1;
    }

    public static List<BluetoothDevice> getConnectedDevices() {
        return getBluetoothManager().getConnectedDevices(BluetoothProfile.GATT);
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothDevice getBluetoothDevice(String mac) {
        BluetoothAdapter adapter = getBluetoothAdapter();
        return adapter != null ? adapter.getRemoteDevice(mac) : null;
    }

    public static BluetoothManager getBluetoothManager() {
        return (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public static BluetoothGattCharacteristic getCharacter(BluetoothGatt gatt, UUID serviceUUID, UUID characterUUID) {
        BluetoothGattService service = gatt.getService(serviceUUID);
        return service != null ? service.getCharacteristic(characterUUID) : null;
    }

    public static boolean setCharacteristicNotification(BluetoothGatt gatt, UUID service, UUID character, boolean enable) {
        BluetoothGattCharacteristic characteristic = getCharacter(gatt, service, character);

        if (characteristic == null) {
            return false;
        }

        if (!gatt.setCharacteristicNotification(characteristic, enable)) {
            return false;
        }

        UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);

        if (descriptor == null) {
            return false;
        }

        byte[] value = (enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        if (!descriptor.setValue(value)) {
            return false;
        }

        if (!gatt.writeDescriptor(descriptor)) {
            return false;
        }

        return true;
    }

    public static BluetoothGattServer openGattServer(BluetoothGattServerCallback callback) {
        return getBluetoothManager().openGattServer(getContext(), callback);
    }

    public static BluetoothLeAdvertiser getAdvertiser() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (!adapter.isEnabled()) {
            return null;
        }
        if (adapter.isMultipleAdvertisementSupported()) {
            return adapter.getBluetoothLeAdvertiser();
        }
        return null;
    }

    private static Context getContext() {
        return ContextUtils.getContext();
    }
}
