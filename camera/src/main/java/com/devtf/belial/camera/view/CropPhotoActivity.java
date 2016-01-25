package com.devtf.belial.camera.view;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.devtf.belial.camera.R;
import com.devtf.belial.camera.util.DeviceUtil;
import com.devtf.belial.camera.util.FileUtils;
import com.devtf.belial.camera.util.ImageUtils;
import com.devtf.belial.camera.widget.CropImageView;

public class CropPhotoActivity extends CameraBaseActivity {

    private Uri fileUri;
    private CropImageView mCropView;
    private Bitmap oriBitmap;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("图片裁剪");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                confirmCropPhoto();
                return false;
            }
        });

        mCropView = (CropImageView) findViewById(R.id.crop_image_view);
        mCropView.setCropMode(CropImageView.CropMode.CIRCLE);

        fileUri = getIntent().getData();
        oriBitmap = ImageUtils.decodeBitmapWithOrientationMax(fileUri.getPath(),
                DeviceUtil.getScreenWidth(this), DeviceUtil.getScreenHeight(this));

        mCropView.setImageBitmap(oriBitmap);
    }

    private void confirmCropPhoto() {
        saveImageToCache(mCropView.getCroppedBitmap());
    }

    private void saveImageToCache(Bitmap croppedImage) {
        if (croppedImage != null) {
            try {
                ImageUtils.saveToFile(this, FileUtils.getInstance(this).getCacheDir(this) + "/croppedcache",
                        false, croppedImage);
                Intent i = new Intent();
                i.setData(Uri.parse("file://" + FileUtils.getInstance(this).getCacheDir(this)
                        + "/croppedcache"));
                setResult(RESULT_OK, i);
                dismissProgressDialog();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                toast("裁剪图片异常，请重试", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop_photo, menu);
        return true;
    }

}
