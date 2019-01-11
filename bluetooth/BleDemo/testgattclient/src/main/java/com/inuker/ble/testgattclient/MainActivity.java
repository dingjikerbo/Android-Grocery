package com.inuker.ble.testgattclient;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.ble.library.utils.BluetoothUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "bush";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Button mButton;
    private ListView mListView;

    private Map<String, ScanResult> mScanResults;
    private ScanAdapter mAdapter;

    private boolean mScaning;

    private static final int REQUEST_OPEN_BLUETOOTH = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.btn);
        mButton.setOnClickListener(this);

        mListView = findViewById(R.id.listview);
        mAdapter = new ScanAdapter();
        mListView.setAdapter(mAdapter);

        mScanResults = new HashMap<>();

        Log.v("bush", String.format("sdkversion: %d", Build.VERSION.SDK_INT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                return;
            }
        }

        checkBluetooth();
    }

    private String byteToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        if (bytes != null) {
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
        }

        return sb.toString();
    }

    private final BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            ScanResult result = new ScanResult(device, rssi, scanRecord);

            if (!mScanResults.containsKey(device.getAddress())) {
                mScanResults.put(device.getAddress(), result);
                Log.v(TAG, String.format("onLeScan: mac = %s, name = %s, record = (%s)%d",
                        device.getAddress(), device.getName(), byteToString(scanRecord), scanRecord.length));
                mAdapter.refresh(mScanResults);
            }
        }
    };

    private void processClicked() {
        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter();

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!adapter.isEnabled()) {
            checkBluetooth();
            return;
        }

        if (!mScaning) {
            if (!adapter.startLeScan(mScanCallback)) {
                Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
            } else {
                mScanResults.clear();

                for (BluetoothDevice device : BluetoothUtils.getConnectedDevices()) {
                    mScanResults.put(device.getAddress(), new ScanResult(device, 0, null));
                }
                onScanStarted();
            }
        } else {
            adapter.stopLeScan(mScanCallback);
            onScanFinished();
        }
    }

    private void onScanStarted() {
        mScaning = true;
        mButton.setText(R.string.stop_scan);
    }

    private void onScanFinished() {
        mScaning = false;
        mButton.setText(R.string.start_scan);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn:
                processClicked();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        processClicked();
    }

    private class ScanResult {
        BluetoothDevice device;
        int rssi;
        byte[] record;

        ScanResult(BluetoothDevice device, int rssi, byte[] bytes) {
            this.device = device;
            this.rssi = rssi;

            if (bytes != null) {
                this.record = new byte[bytes.length];
                System.arraycopy(bytes, 0, record, 0, bytes.length);
            } else {
                this.record = new byte[0];
            }
        }
    }

    private class ScanAdapter extends BaseAdapter {

        private List<ScanResult> mDatas = new ArrayList<>();

        public void refresh(Map<String, ScanResult> results) {
            mDatas.clear();
            mDatas.addAll(results.values());
            Collections.sort(mDatas, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult o1, ScanResult o2) {
                    return o2.rssi - o1.rssi;
                }
            });
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public ScanResult getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView name;
            TextView mac;
            TextView data;
            TextView rssi;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.scan_result_item, null);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.name);
                holder.mac = convertView.findViewById(R.id.mac);
                holder.rssi = convertView.findViewById(R.id.rssi);
                holder.data = convertView.findViewById(R.id.data);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ScanResult result = getItem(position);

            String name = result.device.getName();
            name = TextUtils.isEmpty(name) ? "Unknown" : name;
            holder.name.setText(name);

            holder.mac.setText(result.device.getAddress());
            holder.rssi.setText(String.valueOf(result.rssi));

            StringBuilder sb = new StringBuilder();
            for (Parser.Pdu pdu : Parser.parse(result.record)) {
                sb.append(String.format("Type: 0x%02x, Data: %s\n", pdu.type, byteToString(pdu.data)));
            }
            holder.data.setText(sb.toString());

            convertView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
                    intent.putExtra("device", result.device);
                    startActivity(intent);
                }
            });

            return convertView;
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
        } else {
            checkBluetooth();
        }
    }

    private void checkBluetooth() {
        if (!BluetoothUtils.isBluetoothOpen()) {
            BluetoothUtils.openBluetooth(this, REQUEST_OPEN_BLUETOOTH);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_BLUETOOTH && resultCode != RESULT_OK) {
            finish();
        }
    }
}
