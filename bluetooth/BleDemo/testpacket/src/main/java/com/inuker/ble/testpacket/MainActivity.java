package com.inuker.ble.testpacket;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.inuker.ble.library.channel.Channel;
import com.inuker.ble.library.channel.ChannelCallback;
import com.inuker.ble.library.utils.ByteUtils;
import com.inuker.ble.library.utils.LogUtils;


public class MainActivity extends Activity {

    private Button mBtnClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnClick = findViewById(R.id.btn);
        mBtnClick.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                processClick();
            }
        });
    }

    private Channel mChannel1 = new Channel("Channel1") {
        @Override
        public void write(final byte[] bytes, final ChannelCallback callback) {
            LogUtils.v(String.format("channel1 write: %s", ByteUtils.byteToString(bytes)));
            new Thread() {
                @Override
                public void run() {
                    mChannel2.onRead(bytes);
                    callback.onCallback(0);
                }
            }.start();
        }

        @Override
        public void onRecv(byte[] bytes) {
        }
    };

    private Channel mChannel2 = new Channel("Channel2") {
        @Override
        public void write(final byte[] bytes, final ChannelCallback callback) {
            LogUtils.v(String.format("channel2 write: %s", ByteUtils.byteToString(bytes)));
            new Thread() {
                @Override
                public void run() {
                    mChannel1.onRead(bytes);
                    callback.onCallback(0);
                }
            }.start();
        }

        @Override
        public void onRecv(byte[] bytes) {
            mChannel2.send("Thank you very much!!".getBytes(), new ChannelCallback() {
                @Override
                public void onCallback(int code) {
                    LogUtils.v(String.format("mChannel2 send onCallback: code = %d", code));
                }
            });
        }
    };

    private void processClick() {
        byte[] bytes = "hello world".getBytes();

        mChannel1.send(bytes, new ChannelCallback() {

            @Override
            public void onCallback(int code) {
                LogUtils.v(String.format("mChannel1 send onCallback: code = %d", code));
            }
        });
    }
}


