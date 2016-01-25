package com.devtf.belial.camera.util;


import android.app.Activity;

import com.devtf.belial.camera.view.DialogHelper;

public final class ViewUtil {

    private Activity mActivity;

    private DialogHelper mDialogHelper;

    public ViewUtil(Activity activity) {
        mActivity = activity;
        mDialogHelper = new DialogHelper(mActivity);
    }

    public void toast(String msg, int period) {
        mDialogHelper.toast(msg, period);
    }

    public void showProgressDialog(String msg) {
        mDialogHelper.showProgressDialog(msg);
    }

    public void dismissProgressDialog() {
        mDialogHelper.dismissProgressDialog();
    }

}
