package com.devtf.belial.camera.view;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.devtf.belial.camera.util.ViewUtil;

public class BaseActivity extends AppCompatActivity {

    private ViewUtil mViewUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewUtil = new ViewUtil(this);
    }

    public void toast(String msg, int period) {
        mViewUtil.toast(msg, period);
    }

    public void showProgressDialog(String msg) {
        mViewUtil.showProgressDialog(msg);
    }

    public void dismissProgressDialog() {
        mViewUtil.dismissProgressDialog();
    }

}
