package com.example.inuker.gatt_server;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends Activity {

    private static final String TAG = "bush";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String UUID_FORMAT = "0000%04x-0000-1000-8000-00805f9b34fb";

    private Button mButton;
    private BluetoothLeAdvertiser mAdvertiser;

    private boolean mAdvertising;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.btn);

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mAdvertising) {
                    startAdvertise();
                } else {
                    stopAdvertise();
                }
                mButton.setText(mAdvertising ? "stop" : "start");
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
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

    private void startAdvertise() {
        mAdvertiser = getAdvertiser();

        if (mAdvertiser == null) {
            Toast.makeText(this, "Advertise not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();

        AdvertiseData.Builder builder = new AdvertiseData.Builder();


        UUID uuid = UUID.fromString(String.format(UUID_FORMAT, 0x1234));
        byte[] record = new byte[] {9, 7, 0, 6};
        builder.addServiceData(new ParcelUuid(uuid), record);
        builder.addManufacturerData(0x7860, new byte[] {3, 1, 9, 2});
        builder.setIncludeDeviceName(false);
        builder.setIncludeTxPowerLevel(false);
        AdvertiseData data = builder.build();

        mAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        mAdvertising = true;
    }

    private void stopAdvertise() {
        if (mAdvertiser != null && mAdvertising) {
            mAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertising = false;
        }
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

    private BluetoothLeAdvertiser getAdvertiser() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (!adapter.isEnabled()) {
            Toast.makeText(this, "Please open bluetooth", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (adapter.isMultipleAdvertisementSupported()) {
            return adapter.getBluetoothLeAdvertiser();
        }
        return null;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }
}
