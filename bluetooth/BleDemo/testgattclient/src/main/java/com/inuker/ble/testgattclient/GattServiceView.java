package com.inuker.ble.testgattclient;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.inuker.ble.library.utils.ByteUtils;
import com.inuker.ble.library.utils.BluetoothUtils;
import com.inuker.ble.library.utils.DisplayUtils;

public class GattServiceView {

    private LinearLayout mRoot;

    private PopupWindow mWindow;

    public GattServiceView(LinearLayout root) {
        mRoot = root;
    }

    public void refreshView(final BluetoothGatt gatt) {
        mRoot.post(new Runnable() {
            @Override
            public void run() {
                innerRefreshView(gatt);
            }
        });
    }

    private void innerRefreshView(BluetoothGatt gatt) {
        Context context = mRoot.getContext();
        mRoot.removeAllViews();
        for (BluetoothGattService service : gatt.getServices()) {
            mRoot.addView(getServiceTextView(context, service));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                mRoot.addView(getCharacterTextView(context, characteristic));
            }
        }
    }

    private TextView getServiceTextView(Context context, BluetoothGattService service) {
        TextView textView = new TextView(context);
        textView.setText(String.format("Service: %s", service.getUuid()));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) DisplayUtils.dp2px(30));
        textView.setLayoutParams(params);
        textView.getPaint().setFakeBoldText(true);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return textView;
    }

    private TextView getCharacterTextView(Context context, final BluetoothGattCharacteristic character) {
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) DisplayUtils.dp2px(25));
        textView.setLayoutParams(params);
        textView.setText(String.format("    %s", character.getUuid()));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        textView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showPopupWindow(character);
            }
        });

        return textView;
    }

    private void showPopupWindow(final BluetoothGattCharacteristic characteristic) {
        if (mWindow == null) {
            View view = LayoutInflater.from(mRoot.getContext()).inflate(R.layout.window, null);
            mWindow = new PopupWindow(view, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mWindow.setFocusable(true);
            mWindow.setOutsideTouchable(true);
            mWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    setBackgroundAlpha(1.0f);
                }
            });
        }
        mWindow.showAtLocation(mRoot, Gravity.CENTER, 0, 0);
        View contentView = mWindow.getContentView();
        TextView title = contentView.findViewById(R.id.title);
        title.setText(characteristic.getUuid().toString());
        setBackgroundAlpha(0.3f);

        Button read = contentView.findViewById(R.id.read);
        read.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                mGattCaller.read(characteristic);
            }
        });

        Button write = contentView.findViewById(R.id.write);
        write.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                byte[] value = ByteUtils.stringToBytes("1234");
//                mGattCaller.write(characteristic, value);
            }
        });

        Button notify = contentView.findViewById(R.id.notify);
        notify.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                mGattCaller.notify(characteristic);
            }
        });

        Button unnotify = contentView.findViewById(R.id.unnotify);
        unnotify.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                mGattCaller.unnotify(characteristic);
            }
        });
    }

    public void setBackgroundAlpha(float bgAlpha) {
        Activity activity = (Activity) mRoot.getContext();
        Window window = activity.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        window.setAttributes(lp);
    }
}
