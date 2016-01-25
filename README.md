# LiteCamera
Simply take pictures and cut.
### Screenshot


![](/screenshot/Screenshot_20160125-140256.png)


![](/screenshot/Screenshot_20160125-140309.png)


### Code Example

You need get CameraManager instance to open camera.

````
  CameraManager.getInstance().openCamera(MainActivity.this);
````

And you receive picrure through the following code.

```
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CROP && resultCode == RESULT_OK) {
            Bitmap bitmap= ImageUtils.decodeBitmapWithOrientationMax(data.getData().getPath(),
                    DeviceUtil.getScreenWidth(this), DeviceUtil.getScreenHeight(this));

            croppedPhoto.setImageBitmap(bitmap);
        }
  }
```

