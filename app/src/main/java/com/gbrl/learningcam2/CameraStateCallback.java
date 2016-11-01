package com.gbrl.learningcam2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CameraStateCallback extends CameraDevice.StateCallback {

  private static final String LOG_TAG = "CSC";

  private ShootingActivity shootingActivity;

  public CameraStateCallback(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
  }

  @Override
  public void onOpened(@NonNull CameraDevice cameraDevice) {
    this.shootingActivity.setCameraDevice(cameraDevice);
    this.shootingActivity.setCameraState(ShootingActivity.CameraState.PREVIEW);

    Surface previewSurface = new Surface(this.shootingActivity.getTextureView().getSurfaceTexture());
    Surface pictureSurface = this.shootingActivity.getImageReader().getSurface();
    List<Surface> outputs = new ArrayList<>();
    outputs.add(previewSurface);
    outputs.add(pictureSurface);

    try {
      this.shootingActivity.setPreviewRequestBuilder(this.shootingActivity.getCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW));
      this.shootingActivity.getPreviewRequestBuilder().addTarget(previewSurface);
      this.shootingActivity.getCameraDevice().createCaptureSession(outputs, this.shootingActivity.getSessionStateCallback(), null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDisconnected(@NonNull CameraDevice cameraDevice) {
    cameraDevice.close();
    this.shootingActivity.setCameraDevice(null);
  }

  @Override
  public void onError(@NonNull CameraDevice cameraDevice, int error) {
    cameraDevice.close();
    this.shootingActivity.setCameraDevice(null);
    this.shootingActivity.finish();
  }

}
