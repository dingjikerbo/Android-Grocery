package com.inuker.ble.testgattserver;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.inuker.ble.library.utils.BluetoothUtils;

public class MainActivity extends Activity {

    private static final int REQUEST_OPEN_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Button mBtnPacket;
    private Button mBtnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnPacket = findViewById(R.id.packet);
        mBtnConnect = findViewById(R.id.connect);

        mBtnPacket.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                testPacket();
            }
        });

        mBtnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                testConnect();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                return;
            }
        }

        checkBluetooth();
    }

    private void testPacket() {
        Intent intent = new Intent(this, TestPacketActivity.class);
        startActivity(intent);
    }

    private void testConnect() {
        Intent intent = new Intent(this, TestConnectActivity.class);
        startActivity(intent);
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
            return;
        }

        checkBluetooth();
    }

    private void checkBluetooth() {
        if (!BluetoothUtils.isBluetoothOpen()) {
            BluetoothUtils.openBluetooth(this, REQUEST_OPEN_BLUETOOTH);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_BLUETOOTH) {
            if (resultCode != RESULT_OK) {
                finish();
            }
        }
    }
}
