package com.example.inuker;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

    private void processClick() {

    }
}


