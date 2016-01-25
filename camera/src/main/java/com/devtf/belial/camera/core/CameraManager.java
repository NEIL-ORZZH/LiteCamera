package com.devtf.belial.camera.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;


import com.devtf.belial.camera.Constants;
import com.devtf.belial.camera.Photo;
import com.devtf.belial.camera.view.CameraActivity;
import com.devtf.belial.camera.view.CropPhotoActivity;

import java.util.Stack;

public class CameraManager {

    private static CameraManager mInstance;
    private Stack<Activity> cameras = new Stack<>();

    public static CameraManager getInstance() {
        if (mInstance == null) {
            synchronized (CameraManager.class) {
                if (mInstance == null)
                    mInstance = new CameraManager();
            }
        }
        return mInstance;
    }

    public void openCamera(Activity context) {
        Intent intent = new Intent(context, CameraActivity.class);
        context.startActivityForResult(intent, Constants.REQUEST_CROP);
    }

    public void processPhotoItem(Activity activity, Photo photo) {
        Uri uri = photo.getImageUri().startsWith("file:") ? Uri.parse(photo
                .getImageUri()) : Uri.parse("file://" + photo.getImageUri());

        Intent intent = new Intent(activity, CropPhotoActivity.class);
        intent.setData(uri);
        activity.startActivityForResult(intent, Constants.REQUEST_CROP);
    }

    public void addActivity(Activity act) {
        cameras.add(act);
    }

    public void removeActivity(Activity act) {
        cameras.remove(act);
    }

}
