package com.devtf.belial.camera.view;


import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.devtf.belial.camera.core.CameraManager;
import com.devtf.belial.camera.Constants;
import com.devtf.belial.camera.Photo;
import com.devtf.belial.camera.R;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.devtf.belial.camera.brdcompat.BitmapRegionDecoderCompat;
import com.devtf.belial.camera.core.CameraHelper;
import com.devtf.belial.camera.util.AnimatorUtils;
import com.devtf.belial.camera.util.DeviceUtil;
import com.devtf.belial.camera.util.FileUtils;
import com.devtf.belial.camera.util.IOUtil;
import com.devtf.belial.camera.util.ImageUtils;
import com.devtf.belial.camera.util.StringUtils;
import com.devtf.belial.camera.widget.CameraGrid;
import com.devtf.belial.camera.widget.arclayout.Arc;
import com.devtf.belial.camera.widget.arclayout.ArcLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CameraActivity extends CameraBaseActivity {

    private CameraHelper mCameraHelper;
    private Camera.Parameters parameters = null;
    private Camera cameraInst = null;
    private float pointX, pointY;
    static final int FOCUS = 1;
    static final int ZOOM = 2;
    private int mode;   //0是聚焦 1是放大
    private float dist;
    private int PHOTO_SIZE = 2000;
    private int mCurrentCameraId = 0;
    private Handler handler = new Handler();

    View takePhotoPanel;
    Button takePicture;
    ImageView flashBtn;
    ImageView changeBtn;
    ImageView gird;
    View focusIndex;
    SurfaceView surfaceView;
    View menuLayout;
    ArcLayout arcLayout;
    Button fab;
    CameraGrid cameraGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraHelper = new CameraHelper(this);

        initView();
        initEvent();
    }

    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.camera_surfaceView);
        focusIndex = findViewById(R.id.focus_index);
        flashBtn = (ImageView) findViewById(R.id.flashBtn);
        changeBtn = (ImageView) findViewById(R.id.change);
        gird = (ImageView) findViewById(R.id.grid);
        takePicture = (Button) findViewById(R.id.take_picture);
        takePhotoPanel = findViewById(R.id.panel_take_photo);
        menuLayout = findViewById(R.id.menu_layout);
        arcLayout = (ArcLayout) findViewById(R.id.arc_layout);
        fab = (Button) findViewById(R.id.camera_fab);
        cameraGrid = (CameraGrid) findViewById(R.id.masking);

        cameraGrid.setShowGrid(false);
        arcLayout.setArc(Arc.BOTTOM_RIGHT);

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        surfaceView.setFocusable(true);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
    }

    private void initEvent() {
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    cameraInst.takePicture(null, null, new CustomPictureCallback());
                } catch (Throwable t) {
                    t.printStackTrace();
                    toast("拍照失败，请重试！", Toast.LENGTH_LONG);
                    try {
                        cameraInst.startPreview();
                    } catch (Throwable e) {

                    }
                }
            }
        });

        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnLight(cameraInst);
            }
        });

        gird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchGrid();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isSelected()) {
                    hideMenu();
                } else {
                    showMenu();
                }
                v.setSelected(!v.isSelected());
            }
        });

        boolean canSwitch = false;
        try {
            canSwitch = mCameraHelper.hasFrontCamera() && mCameraHelper.hasBackCamera();
        } catch (Exception e) {
            Log.e(TAG, "获取相机信息失败");
        }
        if (!canSwitch) {
            changeBtn.setVisibility(View.GONE);
        } else {
            changeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCamera();
                }
            });
        }

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 主点按下
                    case MotionEvent.ACTION_DOWN:
                        pointX = event.getX();
                        pointY = event.getY();
                        mode = FOCUS;
                        break;
                    // 副点按下
                    case MotionEvent.ACTION_POINTER_DOWN:
                        dist = spacing(event);
                        // 如果连续两点距离大于10，则判定为多点模式
                        if (spacing(event) > 10f) {
                            mode = ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = FOCUS;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == FOCUS) {
                            pointFocus((int) event.getRawX(), (int) event.getRawY());
                        } else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                float tScale = (newDist - dist) / dist;
                                if (tScale < 0) {
                                    tScale = tScale * 10;
                                }
                                addZoomIn((int) tScale);
                            }
                        }
                        break;
                }
                return false;
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    pointFocus((int) pointX, (int) pointY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
                layout.setMargins((int) pointX - 60, (int) pointY - 60, 0, 0);
                focusIndex.setLayoutParams(layout);
                focusIndex.setVisibility(View.VISIBLE);
                ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                sa.setDuration(600);
                focusIndex.startAnimation(sa);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        focusIndex.setVisibility(View.INVISIBLE);
                    }
                }, 600);
            }
        });

        takePhotoPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void showMenu() {
        menuLayout.setVisibility(View.VISIBLE);

        List<Animator> animList = new ArrayList<>();

        for (int i = 0, len = arcLayout.getChildCount(); i < len; i++) {
            animList.add(createShowItemAnimator(arcLayout.getChildAt(i)));
        }

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(400);
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.playTogether(animList);
        animSet.start();
    }

    private void hideMenu() {
        List<Animator> animList = new ArrayList<>();

        for (int i = arcLayout.getChildCount() - 1; i >= 0; i--) {
            animList.add(createHideItemAnimator(arcLayout.getChildAt(i)));
        }

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(400);
        animSet.setInterpolator(new AnticipateInterpolator());
        animSet.playTogether(animList);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                menuLayout.setVisibility(View.INVISIBLE);
            }
        });
        animSet.start();
    }

    private Animator createShowItemAnimator(View item) {
        float dx = fab.getLeft() - item.getLeft();
        float dy = fab.getTop() - item.getTop();

        if (Build.VERSION.SDK_INT < 11) {
            RotateAnimation animation = new RotateAnimation(0f, 720f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animation.setDuration(100);
            animation.setFillAfter(true);
            item.startAnimation(animation);
        } else {
            item.setRotation(0f);
        }

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.rotation(0f, 720f),
                AnimatorUtils.translationX(dx, 0f),
                AnimatorUtils.translationY(dy, 0f)
        );

        return anim;
    }

    private Animator createHideItemAnimator(final View item) {
        float dx = fab.getLeft() - item.getLeft();
        float dy = fab.getTop() - item.getTop();

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.rotation(720f, 0f),
                AnimatorUtils.translationX(0f, dx),
                AnimatorUtils.translationY(0f, dy)
        );

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (Build.VERSION.SDK_INT < 11) {
                    AnimatorUtils.translationX(0f);
                    AnimatorUtils.translationY(0f);
                } else {
                    item.setTranslationX(0f);
                    item.setTranslationY(0f);
                }
            }
        });

        return anim;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent result) {
        if (requestCode == Constants.REQUEST_CROP && resultCode == RESULT_OK) {
            setResult(resultCode, result);
            finish();
        }
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private int curZoomValue = 0;

    private void addZoomIn(int delta) {

        try {
            Camera.Parameters params = cameraInst.getParameters();
            Log.d(TAG, "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                cameraInst.setParameters(params);
                return;
            } else {
                cameraInst.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pointFocus(int x, int y) {
        cameraInst.cancelAutoFocus();
        parameters = cameraInst.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showPoint(x, y);
        }
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
        }

        autoFocus();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showPoint(int x, int y) {
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<>();

            int rectY = -x * 2000 / DeviceUtil.getScreenWidth(this) + 1000;
            int rectX = y * 2000 / DeviceUtil.getScreenHeight(this) - 1000;

            int left = rectX < -900 ? -1000 : rectX - 100;
            int top = rectY < -900 ? -1000 : rectY - 100;
            int right = rectX > 900 ? 1000 : rectX + 100;
            int bottom = rectY > 900 ? 1000 : rectY + 100;
            Rect area1 = new Rect(left, top, right, bottom);
            areas.add(new Camera.Area(area1, 800));
            parameters.setMeteringAreas(areas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private final class CustomPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePicTask(data).execute();
            camera.startPreview();
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {

        private byte[] data;

        protected void onPreExecute() {
            showProgressDialog("处理中...");
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (StringUtils.isNotEmpty(result)) {
                dismissProgressDialog();
                CameraManager.getInstance().processPhotoItem(CameraActivity.this,
                        new Photo(result, System.currentTimeMillis()));
            } else {
                toast("拍照失败，请稍后重试！", Toast.LENGTH_LONG);
            }
        }
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {

        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                if (cameraInst != null) {
                    cameraInst.stopPreview();
                    cameraInst.release();
                    cameraInst = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Camera was closed");
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null == cameraInst) {
                try {
                    cameraInst = Camera.open();
                    cameraInst.setPreviewDisplay(holder);
                    initCamera();
                    cameraInst.startPreview();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            autoFocus();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cameraInst == null) {
                    return;
                }
                cameraInst.autoFocus(new Camera.AutoFocusCallback() {

                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();
                        }
                    }
                });
            }
        };
    }

    private Camera.Size adapterSize = null;
    private Camera.Size previewSize = null;

    private void initCamera() {
        parameters = cameraInst.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);

        setUpPicSize(parameters);
        setUpPreviewSize(parameters);

        if (adapterSize != null) {
            parameters.setPictureSize(adapterSize.width, adapterSize.height);
        }
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        setDisplay(parameters, cameraInst);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst.startPreview();
        cameraInst.cancelAutoFocus();
    }

    private void setUpPicSize(Camera.Parameters parameters) {
        if (adapterSize == null) {
            adapterSize = findBestPictureResolution();
        }
    }

    private void setUpPreviewSize(Camera.Parameters parameters) {
        if (previewSize == null) {
            previewSize = findBestPreviewResolution();
        }
    }

    private static final int MIN_PREVIEW_PIXELS = 480 * 320;
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final String TAG = "Camera";

    private Camera.Size findBestPreviewResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return defaultPreviewResolution;
        }

        List<Camera.Size> supportedPreviewResolutions = new ArrayList<>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder builder = new StringBuilder();
        for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
            builder.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height)
                    .append(' ');
        }

        double screenAspectRatio = (double) DeviceUtil.getScreenWidth(this);
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            if (width * height < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == DeviceUtil.getScreenWidth(this)
                    && maybeFlippedHeight == DeviceUtil.getScreenHeight(this)) {
                return supportedPreviewResolution;
            }
        }
        Camera.Size largestPreview;
        if (!supportedPreviewResolutions.isEmpty()) {
            largestPreview = supportedPreviewResolutions.get(0);
            return largestPreview;
        }

        return defaultPreviewResolution;
    }

    private Camera.Size findBestPictureResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值

        StringBuilder picResolutionSb = new StringBuilder();
        for (Camera.Size supportedPicResolution : supportedPicResolutions) {
            picResolutionSb.append(supportedPicResolution.width).append('x')
                    .append(supportedPicResolution.height).append(" ");
        }
        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);
        Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
        Log.d(TAG, "default picture resolution " + defaultPictureResolution.width + "x"
                + defaultPictureResolution.height);

        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(
                supportedPicResolutions);
        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) DeviceUtil.getScreenWidth(this)
                / (double) DeviceUtil.getScreenHeight(this);
        Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然后在比较宽高比
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，对于照片，则取其中最大比例的，而不是选择与屏幕分辨率相同的
        if (!sortedSupportedPicResolutions.isEmpty()) {
            return sortedSupportedPicResolutions.get(0);
        }

        return defaultPictureResolution;
    }

    private void setDisplay(Camera.Parameters parameters, Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }

    public String saveToSDCard(byte[] data) throws IOException {
        Bitmap croppedImage;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        PHOTO_SIZE = options.outHeight > options.outWidth ? options.outWidth : options.outHeight;
        int height = options.outHeight > options.outWidth ? options.outHeight : options.outWidth;
        options.inJustDecodeBounds = false;
        Rect r;
        if (mCurrentCameraId == 1) {
            r = new Rect(height - PHOTO_SIZE, 0, height, PHOTO_SIZE);
        } else {
            r = new Rect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        }
        try {
            croppedImage = decodeRegionCrop(data, r);
        } catch (Exception e) {
            return null;
        }
        String imagePath = ImageUtils.saveToFile(this, FileUtils.getInstance(this).getSystemPhotoPath(), true,
                croppedImage);
        croppedImage.recycle();

        return imagePath;
    }

    private Bitmap decodeRegionCrop(byte[] data, Rect rect) {
        InputStream is = null;
        System.gc();
        Bitmap croppedImage = null;
        try {
            is = new ByteArrayInputStream(data);
            BitmapRegionDecoderCompat decoder = BitmapRegionDecoderCompat.newInstance(is, false);

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());
            } catch (IllegalArgumentException e) {
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeStream(is);
        }

        Matrix m = new Matrix();
        m.setRotate(90, PHOTO_SIZE / 2, PHOTO_SIZE / 2);
        if (mCurrentCameraId == 1) {
            m.postScale(1, -1);
        }
        Bitmap rotatedImage = Bitmap.createBitmap(croppedImage, 0, 0, PHOTO_SIZE, PHOTO_SIZE, m, true);
        if (rotatedImage != croppedImage && croppedImage != null)
            croppedImage.recycle();

        return rotatedImage;
    }

    private void turnLight(Camera mCamera) {
        if (mCamera == null || mCamera.getParameters() == null
                || mCamera.getParameters().getSupportedFlashModes() == null) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        String flashMode = mCamera.getParameters().getFlashMode();
        List<String> supportedModes = mCamera.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(parameters);
            flashBtn.setImageResource(R.drawable.camera_flash_on);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                flashBtn.setImageResource(R.drawable.camera_flash_auto);
                mCamera.setParameters(parameters);
            } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                flashBtn.setImageResource(R.drawable.camera_flash_off);
                mCamera.setParameters(parameters);
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            flashBtn.setImageResource(R.drawable.camera_flash_off);
        }
    }

    private void switchGrid() {
        if (cameraGrid.isShowGrid()) {
            cameraGrid.setShowGrid(false);
        } else {
            cameraGrid.setShowGrid(true);
        }
    }

    private void switchCamera() {
        mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
        releaseCamera();
        setUpCamera(mCurrentCameraId);
    }

    private void releaseCamera() {
        if (cameraInst != null) {
            cameraInst.setPreviewCallback(null);
            cameraInst.release();
            cameraInst = null;
        }
        adapterSize = null;
        previewSize = null;
    }

    private void setUpCamera(int mCurrentCameraId2) {
        cameraInst = getCameraInstance(mCurrentCameraId2);
        if (cameraInst != null) {
            try {
                cameraInst.setPreviewDisplay(surfaceView.getHolder());
                initCamera();
                cameraInst.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            toast("切换失败，请重试！", Toast.LENGTH_LONG);
        }
    }

    private Camera getCameraInstance(final int id) {
        Camera c = null;
        try {
            c = mCameraHelper.openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

}
