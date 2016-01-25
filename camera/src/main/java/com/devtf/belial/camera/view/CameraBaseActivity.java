package com.devtf.belial.camera.view;

import android.os.Bundle;

import com.devtf.belial.camera.core.CameraManager;


public class CameraBaseActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraManager.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.getInstance().removeActivity(this);
    }
}
