package com.example.dell.matrxtest;

import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
private MyImageView myImageView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        image = findViewById(R.id.image);

        myImageView  = findViewById(R.id.myImageView);
        myImageView.setMyOnClickListener(new MyImageView.MyOnclickListener() {
            @Override
            public void onClick(String part) {
                Log.d(TAG, "onClick: ------------------------------"+part);
            }
        });

    }
}
