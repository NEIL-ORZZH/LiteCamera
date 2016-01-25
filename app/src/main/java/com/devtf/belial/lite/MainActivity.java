package com.devtf.belial.lite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.devtf.belial.camera.core.CameraManager;
import com.devtf.belial.camera.Constants;
import com.devtf.belial.camera.util.DeviceUtil;
import com.devtf.belial.camera.util.ImageUtils;

public class MainActivity extends AppCompatActivity {

    private Button openCamera;
    private ImageView croppedPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openCamera = (Button) findViewById(R.id.open_camera);
        croppedPhoto = (ImageView) findViewById(R.id.display_crop_photo);
        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraManager.getInstance().openCamera(MainActivity.this);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CROP && resultCode == RESULT_OK) {
            Bitmap bitmap= ImageUtils.decodeBitmapWithOrientationMax(data.getData().getPath(),
                    DeviceUtil.getScreenWidth(this), DeviceUtil.getScreenHeight(this));

            croppedPhoto.setImageBitmap(bitmap);
        }

    }
}
