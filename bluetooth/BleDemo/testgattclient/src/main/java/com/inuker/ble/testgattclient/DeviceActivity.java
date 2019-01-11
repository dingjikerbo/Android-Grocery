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
import com.inuker.ble.library.utils.BluetoothUtils;
import com.inuker.ble.library.utils.ByteUtils;
import com.inuker.ble.library.utils.ContextUtils;
import com.inuker.ble.library.utils.DisplayUtils;
import com.inuker.ble.library.utils.LogUtils;
import com.inuker.ble.library.utils.RandUtils;

import static com.inuker.ble.library.Constants.UUID_MYSERVICE;
import static com.inuker.ble.library.Constants.UUID_PACKET;

public class DeviceActivity extends Activity {

    private static final String TAG = "bush";

    private Button mBtnConnect;
    private Button mBtnPacket;
    private ScrollView mScrollView;

    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private LinearLayout mResultContainer;

    private Channel mChannel;
    private volatile ChannelCallback mChannelCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_activity);

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
        mBtnPacket = findViewById(R.id.test_packet);
        mBtnPacket.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(DeviceActivity.this, R.string.device_not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                sendMessage();
            }
        });

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

        mResultContainer = findViewById(R.id.result);
    }

    private BluetoothGattCharacteristic getNotifyCharacteristic() {
        if (mBluetoothGatt != null) {
            BluetoothGattService service = mBluetoothGatt.getService(UUID_MYSERVICE);
            return service != null ? service.getCharacteristic(UUID_PACKET) : null;
        }
        return null;
    }

    private void startChannel() {
        if (mChannel != null) {
            throw new IllegalStateException();
        }
        LogUtils.v(String.format("start Channel"));
        mChannel = new Channel(String.format("GattClient -> Remote(%s:%s)", mDevice.getName(), mDevice.getAddress())) {
            @Override
            public void write(byte[] bytes, ChannelCallback callback) {
                BluetoothGattCharacteristic characteristic = getNotifyCharacteristic();
                characteristic.setValue(bytes);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                mBluetoothGatt.writeCharacteristic(characteristic);
                mChannelCallback = callback;
            }

            @Override
            public void onRecv(byte[] bytes) {
                addMessage("Recv", new String(bytes));
            }
        };
    }

    private void sendMessage() {
        if (mChannel == null) {
            return;
        }
        int a = RandUtils.nextInt(100) + 100;
        int b = RandUtils.nextInt(100) + 1;
        char operator = "+-*/".charAt(RandUtils.nextInt(4));
        final String msg = String.format("Please answer this question: %d %c %d = ?", a, operator, b);
        mChannel.send(msg.getBytes(), new ChannelCallback() {
            @Override
            public void onCallback(int code) {
                if (code == Code.SUCCESS) {
                    addMessage("Send", msg);
                }
            }
        });
    }

    private void closeChannel() {
        LogUtils.v("close Channel");
        if (mChannel != null) {
            mChannel.close();
            mChannel = null;
        }
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

    private void addMessage(String tag, String msg) {
        ContextUtils.assertRuntime(true);
        TextView textView = getTextView();
        textView.setText(String.format("%s: %s", tag, msg)); // 3 + 5 = ?
        mResultContainer.addView(textView);
    }

    private void processClick() {
        int state = BluetoothUtils.getBluetoothConnectionState(mDevice);

        switch (state) {
            case BluetoothGatt.STATE_CONNECTED:
                disconnect();
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                connect();
                break;
        }
    }

    private void connect() {
        mBluetoothGatt = mDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.v(TAG, String.format("onConnectionStateChange: status = %d, newState = %d", status, newState));

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                    onConnected();

                    if (!mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                        disconnect();
                    }

                    if (!mBluetoothGatt.discoverServices()) {
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
                    refreshProfile();

                    startChannel();

                    BluetoothUtils.setCharacteristicNotification(gatt, UUID_MYSERVICE, UUID_PACKET, true);
                    mBtnPacket.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBtnPacket.setEnabled(true);
                        }
                    }, 1000);
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
                if (mChannelCallback != null) {
                    mChannelCallback.onCallback(status == BluetoothGatt.GATT_SUCCESS ? Code.SUCCESS : Code.FAIL);
                    mChannelCallback = null;
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.v(TAG, String.format("onCharacteristicChanged service = %s, character = %s, value = %s",
                        characteristic.getService().getUuid(),
                        characteristic.getUuid(),
                        ByteUtils.byteToString(characteristic.getValue())));

                mChannel.onRead(characteristic.getValue());
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                Log.v(TAG, String.format("onReadRemoteRssi rssi = %d, status = %d", rssi, status));
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.v(TAG, String.format("onDescriptorWrite service = %s, character = %s, descriptor = %s, status = %d",
                        descriptor.getCharacteristic().getService().getUuid(),
                        descriptor.getCharacteristic().getUuid(),
                        descriptor.getUuid(), status));
            }
        });
    }

    private void refreshProfile() {
        for (BluetoothGattService service : mBluetoothGatt.getServices()) {
            Log.v(TAG, String.format("Service: %s", service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.v(TAG, String.format("  Characteristic: %s", characteristic.getUuid()));
            }
        }
    }

    private void disconnect() {
        LogUtils.e("disconnect!!");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        closeChannel();
        onDisconnected();
    }

    private void onDisconnected() {
        mBtnConnect.post(new Runnable() {
            @Override
            public void run() {
                mBtnConnect.setText(R.string.start_connect);
                mBtnPacket.setEnabled(false);
                mResultContainer.removeAllViews();
            }
        });
    }

    private void onConnected() {
        mBtnConnect.post(new Runnable() {
            @Override
            public void run() {
                mBtnConnect.setText(R.string.disconnect);
            }
        });
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }
}
