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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.ble.library.Constants;
import com.inuker.ble.library.channel.Channel;
import com.inuker.ble.library.channel.ChannelCallback;
import com.inuker.ble.library.channel.Code;
import com.inuker.ble.library.utils.ByteUtils;
import com.inuker.ble.library.utils.ContextUtils;
import com.inuker.ble.library.utils.DisplayUtils;
import com.inuker.ble.library.utils.LogUtils;

public class TestPacketActivity extends Activity {

    private static final String TAG = "bush";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Button mButton;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothGattServer mGattServer;

    private LinearLayout mContainer;
    private ScrollView mScrollView;

    private boolean mServerRunning;

    private Channel mChannel;
    private ChannelCallback mChannelCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_packet);

        setTitle(R.string.test_packet);

        mButton = findViewById(R.id.btn);
        mContainer = findViewById(R.id.container);
        mScrollView = findViewById(R.id.scrollview);

        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScrollView.post(new Runnable() {
                    public void run() {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

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
            Log.v(TAG, String.format("AdvertiseCallback onStartSuccess: %s", Thread.currentThread().getName()));
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.v(TAG, String.format("AdvertiseCallback onStartFailure %d: %s", errorCode, Thread.currentThread().getName()));
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

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                    startChannel(device);
                } else {
                    closeChannel();
                }
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
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                characteristic.setValue(value);
                mChannel.onRead(value);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                Log.v(TAG, String.format("onDescriptorWriteRequest device = %s, service = %s, character = %s, descriptor = %s, value = %s", device.getAddress(),
                        descriptor.getCharacteristic().getService().getUuid(),
                        descriptor.getCharacteristic().getUuid(),
                        descriptor.getUuid(),
                        ByteUtils.byteToString(value)));
                descriptor.setValue(value);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
                Log.v(TAG, String.format("onNotificationSent device = %s, status = %d, callback = %s",
                        device.getAddress(), status, mChannelCallback));

                if (mChannelCallback != null) {
                    mChannelCallback.onCallback(status == BluetoothGatt.GATT_SUCCESS ? Code.SUCCESS : Code.FAIL);
                    setChannelCallback(null);
                }
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

    private synchronized void setChannelCallback(ChannelCallback callback) {
        mChannelCallback = callback;
    }

    private void startChannel(final BluetoothDevice device) {
        if (mChannel != null) {
            throw new IllegalStateException();
        }
        LogUtils.v(String.format("start channel"));
        mChannel = new Channel(String.format("GattServer -> Client(%s:%s)", device.getName(), device.getAddress())) {
            @Override
            public void write(byte[] bytes, ChannelCallback callback) {
                BluetoothGattCharacteristic characteristic = getNotifyCharacteristic();
                characteristic.setValue(bytes);
                setChannelCallback(callback);
                mGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }

            @Override
            public void onRecv(byte[] bytes) {
                String msg = new String(bytes);
                dispatchRecvData(msg);
            }
        };
    }

    private void closeChannel() {
        LogUtils.v("close channel");
        mChannel.close();
        mChannel = null;
    }

    private TextView getTextView() {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) DisplayUtils.dp2px(30));
        textView.setLayoutParams(params);
        textView.getPaint().setFakeBoldText(true);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return textView;
    }

    private void dispatchRecvData(String msg) {
        addMessage("Recv", msg);

        int result = processRecvData(msg);
        String msg2 = msg.replace("?", result + "");
        final String reply = "So easy, this is my answer: " + msg2.substring(msg2.indexOf(":") + 1);

        mChannel.send(reply.getBytes(), new ChannelCallback() {
            @Override
            public void onCallback(int code) {
                if (code == Code.SUCCESS) {
                    addMessage("Reply", reply);
                }
            }
        });
    }

    private void addMessage(String tag, String msg) {
        ContextUtils.assertRuntime(true);
        TextView textView = getTextView();
        textView.setText(String.format("%s: %s", tag, msg)); // 3 + 5 = ?
        mContainer.addView(textView);
    }

    private int processRecvData(String s) {
        char[] operators = {'+', '-', '*', '/'};
        int operator = 0, index = 0;
        for (; operator < operators.length; operator++) {
            if ((index = s.indexOf(operators[operator])) >= 0) {
                break;
            }
        }
        int start = s.indexOf(":");
        int tidx = s.indexOf('=');
        s = tidx >= 0 ? s.substring(0, tidx) : s;
        int a = Integer.parseInt(s.substring(start + 1, index).trim());
        int b = Integer.parseInt(s.substring(index + 1).trim());
        switch (operator) {
            case 0:
                return a + b;
            case 1:
                return a - b;
            case 2:
                return a * b;
            case 3:
                return a / b;
            default:
                return 0;
        }
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
