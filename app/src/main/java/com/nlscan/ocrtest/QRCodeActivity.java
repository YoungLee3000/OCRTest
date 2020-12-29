package com.nlscan.ocrtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class QRCodeActivity extends BaseActivity {

    private String mCodePath;

    private ImageView mImQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        setTitle("寄件码显示");

        mCodePath = getIntent().getStringExtra("certificate");

        mImQrCode = (ImageView) findViewById(R.id.im_qrcode);

        Bitmap bmpObj = BitmapFactory.decodeFile(mCodePath);

        mImQrCode.setImageBitmap(bmpObj);




    }
}
