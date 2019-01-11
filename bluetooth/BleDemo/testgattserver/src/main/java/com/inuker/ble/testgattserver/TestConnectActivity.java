package com.inuker.ble.testgattserver;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.inuker.ble.library.Constants;
import com.inuker.ble.library.channel.Channel;
import com.inuker.ble.library.channel.ChannelCallback;
import com.inuker.ble.library.utils.ByteUtils;

public class TestConnectActivity extends Activity {

    private static final String TAG = "bush";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Button mButton;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothGattServer mGattServer;

    private boolean mServerRunning;

    private Channel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_connect);

        setTitle(R.string.test_connect);

        mButton = findViewById(R.id.btn);

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                processClicked();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    private void processClicked() {
        if (mServerRunning) {
            stopAdvertising();
            stopGattServer();
            mButton.setText(R.string.start);
        } else {
            startAdvertising();
            startGattServer();
            mButton.setText(R.string.stop);
        }
        mServerRunning = !mServerRunning;
    }

    private void startAdvertising() {
        if (mAdvertiser != null) {
            throw new IllegalStateException();
        }
        mAdvertiser = GattServerHelper.createAdvertiser();
        AdvertiseSettings settings = GattServerHelper.getDefaultAdvertiseSettings();
        AdvertiseData data = GattServerHelper.getDefaultAdvertiseData();
        mAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mAdvertiser == null) {
            throw new IllegalStateException();
        }
        mAdvertiser.stopAdvertising(mAdvertiseCallback);
        mAdvertiser = null;
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.v(TAG, String.format("onStartSuccess: %s", Thread.currentThread().getName()));
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.v(TAG, String.format("onStartFailure %d: %s", errorCode, Thread.currentThread().getName()));
        }
    };

    private void startGattServer() {
        if (mGattServer != null) {
            throw new IllegalStateException();
        }
        mGattServer = GattServerHelper.createGattServer(new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                Log.v(TAG, String.format("onConnectionStateChange device = %s, status = %d, newState = %d", device.getAddress(), status, newState));
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                Log.v(TAG, String.format("onCharacteristicReadRequest device = %s, service = %s, character = %s, requestId = %d, ", device.getAddress(), characteristic.getService().getUuid(),
                        characteristic.getUuid(), requestId));

                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                Log.v(TAG, String.format("onCharacteristicWriteRequest device = %s, service = %s, character = %s, value = %s", device.getAddress(), characteristic.getService().getUuid(),
                        characteristic.getUuid(), ByteUtils.byteToString(value)));
                characteristic.setValue(value);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                Log.v(TAG, String.format("onDescriptorWriteRequest device = %s, service = %s, character = %s, descriptor = %s, value = %s", device.getAddress(),
                        descriptor.getCharacteristic().getService().getUuid(),
                        descriptor.getCharacteristic().getUuid(),
                        descriptor.getUuid(),
                        ByteUtils.byteToString(value)));
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                mChannel.onRead(value);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
                Log.v(TAG, String.format("onNotificationSent device = %s, status = %d", device.getAddress(), status));
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
                Log.v(TAG, String.format("onMtuChanged device = %s, mtu = %d", device.getAddress(), mtu));
            }
        });
        BluetoothGattService service = GattServerHelper.createGattService();
        mGattServer.addService(service);
    }

    private BluetoothGattCharacteristic getNotifyCharacteristic() {
        if (mGattServer != null) {
            BluetoothGattService service = mGattServer.getService(Constants.UUID_MYSERVICE);
            return service != null ? service.getCharacteristic(Constants.UUID_PACKET) : null;
        }
        return null;
    }

    private void startChannel(final BluetoothDevice device) {
        if (mChannel != null) {
            throw new IllegalStateException();
        }
        mChannel = new Channel(String.format("GattServer -> Client(%s:%s)", device.getName(), device.getAddress())) {
            @Override
            public void write(byte[] bytes, ChannelCallback callback) {
                mGattServer.notifyCharacteristicChanged(device, getNotifyCharacteristic(), false);
            }

            @Override
            public void onRecv(byte[] bytes) {

            }
        };
    }

    private void stopGattServer() {
        if (mGattServer == null) {
            throw new IllegalStateException();
        }
        mGattServer.close();
        mGattServer = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean grantedLocation = true;
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    grantedLocation = false;
                }
            }
        }

        if (!grantedLocation) {
            Toast.makeText(this, "Permission error !!!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
