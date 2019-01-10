package com.inuker.ble.testgattclient;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.inuker.ble.library.utils.ByteUtils;
import com.inuker.ble.library.utils.UUIDUtils;
import com.inuker.ble.library.utils.BluetoothUtils;

public class TestActivity extends Activity implements GattServiceView.GattCaller {

    private static final String TAG = "bush";

    private Button mBtnConnect;

    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic mCharacter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        Intent intent = getIntent();
        if (intent != null) {
            mDevice = intent.getParcelableExtra("device");
        }
        if (mDevice == null) {
            finish();
        }

        mBtnConnect = findViewById(R.id.connect);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                processClick();
            }
        });
    }

    private void processClick() {
        if (mBluetoothGatt == null) {
            mBluetoothGatt = connect();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mBluetoothGatt.readRemoteRssi()) {
                        Log.v(TAG, String.format("AuOh, readRssi failed"));
                        disconnect();
                    }
                }
            }, 5000);
        } else {
            disconnect();
        }
    }

    private BluetoothGatt connect() {
        BluetoothGatt gatt = mDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.v(TAG, String.format("onConnectionStateChange: status = %d, newState = %d", status, newState));

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {

                    if (!gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                        disconnect();
                    }

                    if (!gatt.discoverServices()) {
                        disconnect();
                    }

                } else {
                    disconnect();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.v(TAG, String.format("onServicesDiscovered: status = %d", status));

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    refreshProfile(gatt);

                    if (!gatt.requestMtu(400)) {
                        disconnect();
                    }
                } else {
                    disconnect();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.v(TAG, String.format("onCharacteristicRead: service = %s, character = %s, value = %s, status = %d",
                        characteristic.getService().getUuid(),
                        characteristic.getUuid(),
                        ByteUtils.byteToString(characteristic.getValue()),
                        status));
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.v(TAG, String.format("onCharacteristicWrite service = %s, character = %s, status = %d",
                        characteristic.getService().getUuid(),
                        characteristic.getUuid(),
                        status));
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.v(TAG, String.format("onCharacteristicChanged service = %s, character = %s, value = %s",
                        characteristic.getService().getUuid(),
                        characteristic.getUuid(),
                        ByteUtils.byteToString(characteristic.getValue())));
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                Log.v(TAG, String.format("onReadRemoteRssi rssi = %d, status = %d", rssi, status));
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    disconnect();
                } else {
                    testRead(gatt);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.v(TAG, String.format("onDescriptorWrite service = %s, character = %s, descriptor = %s, status = %d",
                        descriptor.getCharacteristic().getService().getUuid(),
                        descriptor.getCharacteristic().getUuid(),
                        descriptor.getUuid(), status));
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                Log.v(TAG, String.format("onMtuChanged mtu = %d, status = %d", mtu, status));
                BluetoothGattService service = gatt.getService(UUIDUtils.makeUUID(0xA7C9));
                mCharacter = service.getCharacteristic(UUIDUtils.makeUUID(0x01));
                gatt.readCharacteristic(mCharacter);
            }
        });
        return gatt;
    }

    private void testRead(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(UUIDUtils.makeUUID(0xA7C9));
        mCharacter = service.getCharacteristic(UUIDUtils.makeUUID(0x01));
        gatt.readCharacteristic(mCharacter);
    }

    private void refreshProfile(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.v(TAG, String.format("Service: %s", service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.v(TAG, String.format("  Characteristic: %s", characteristic.getUuid()));
            }
        }
    }

    private void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        onDisconnected();
    }

    private void onDisconnected() {
        mBtnConnect.setText(R.string.start_connect);
    }

    private void onConnected() {
        mBtnConnect.setText(R.string.disconnect);
    }

    private void onReadCompleted() {

    }

    private void onWriteComplted() {

    }

    private void onNotifyCompleted(boolean success) {

    }

    private void onUnnotifyCompleted(boolean success) {

    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Override
    public void read(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @Override
    public void write(BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    @Override
    public void notify(BluetoothGattCharacteristic characteristic) {
        if (!BluetoothUtils.setCharacteristicNotification(mBluetoothGatt, characteristic.getService().getUuid(), characteristic.getUuid(), true)) {
            onNotifyCompleted(false);
        }
    }

    @Override
    public void unnotify(BluetoothGattCharacteristic characteristic) {
        if (!BluetoothUtils.setCharacteristicNotification(mBluetoothGatt, characteristic.getService().getUuid(), characteristic.getUuid(), false)) {
            onUnnotifyCompleted(false);
        }
    }
}
