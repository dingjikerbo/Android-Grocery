package com.example.inuker.scan;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Button mButton;
    private ListView mListView;

    private BroadcastReceiver mReceiver;

    private Map<String, ScanResult> mScanResults;
    private ScanAdapter mAdapter;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    private void processClicked() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!adapter.isDiscovering()) {
            if (!adapter.startDiscovery()) {
                Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
            } else {
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    mScanResults.put(device.getAddress(), new ScanResult(device, 0));
                }
                onScanStarted();
            }
        } else {
            if (!adapter.cancelDiscovery()) {
                Toast.makeText(this, "Stop scan failed", Toast.LENGTH_SHORT).show();
            } else {
                onScanFinished();
            }
        }
    }

    private void onScanStarted() {
        mButton.setText(R.string.stop_scan);
    }

    private void onScanFinished() {
        mButton.setText(R.string.start_scan);
    }

    private void registerReceiver() {
        if (mReceiver == null) {
            mReceiver = new BluetoothSearchReceiver();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
    }

    private void unregisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn:
                processClicked();
                break;
        }
    }

    private class BluetoothSearchReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                return;
            }
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,
                        Short.MIN_VALUE);

                ScanResult result = new ScanResult(device, rssi);

                if (!mScanResults.containsKey(device.getAddress())) {
                    mScanResults.put(device.getAddress(), result);
                }

                mAdapter.refresh(mScanResults);
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                onScanStarted();
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                onScanFinished();
            }
        }
    }

    private class ScanResult {
        BluetoothDevice device;
        int rssi;

        ScanResult(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        unregisterReceiver();
        super.onPause();
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ScanResult result = getItem(position);

            String name = result.device.getName();
            name = TextUtils.isEmpty(name) ? "Unknown" : name;
            holder.name.setText(name);

            holder.mac.setText(result.device.getAddress());
            holder.rssi.setText(String.valueOf(result.rssi));

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
        }
    }
}
