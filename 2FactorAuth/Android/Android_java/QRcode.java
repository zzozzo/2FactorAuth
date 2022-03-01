package com.example.twofactor;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRcode extends AppCompatActivity {


    private ImageView iv;
    TextView timer;
    String getCompany_num;
    String time = "15";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_rcode);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        timer = (TextView)findViewById(R.id.timer);
        iv = findViewById(R.id.qrcode);
        getCompany_num = getIntent().getStringExtra("userID");

        countDown(time);

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(getCompany_num, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

                iv.setImageBitmap(bitmap);

        } catch (Exception e) {
        }

    }
    public void countDown(String time){
        long conversionTime =0;

        String getSecond = time;
        conversionTime = Long.valueOf(getSecond)*1000;

        new CountDownTimer(conversionTime, 1000){
            @Override
            public void onTick(long millisUntilFinished) {
                String sec = String.valueOf(millisUntilFinished/1000);
                timer.setText("남은 시간 " + sec);
            }

            @Override
            public void onFinish() {
                iv.setVisibility(View.INVISIBLE);
                timer.setText("시간 초과!");
            }
        }.start();
    }

}