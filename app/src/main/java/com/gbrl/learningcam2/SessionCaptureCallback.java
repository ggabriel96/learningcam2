package com.gbrl.learningcam2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;

/**
 * Created by gbrl on 10/31/16.
 */

public class SessionCaptureCallback extends CameraCaptureSession.CaptureCallback {

  private ShootingActivity shootingActivity;

  public SessionCaptureCallback(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
  }

  private void process(CaptureResult result) {
    Integer autoFocusState = null, autoExposureState = null;
    switch (this.shootingActivity.getCameraState()) {
      // case PREVIEW:
      // We have nothing to do when the camera preview is working normally.
      // break;
      case WAITING_FOCUS_LOCK:
        autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (autoFocusState == null) {
          this.shootingActivity.captureStillPicture();
        } else if (autoFocusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || autoFocusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
          // CONTROL_AE_STATE can be null on some devices
          autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
          if (autoExposureState == null || autoExposureState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
            this.shootingActivity.setCameraState(ShootingActivity.CameraState.PICTURE_TAKEN);
            this.shootingActivity.captureStillPicture();
          } else {
            this.shootingActivity.runPrecaptureSequence();
          }
        }
        break;
      case WAITING_EXPOSURE_PRECAPTURE:
        // CONTROL_AE_STATE can be null on some devices
        autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
        if (autoExposureState == null || autoExposureState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || autoExposureState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
          this.shootingActivity.setCameraState(ShootingActivity.CameraState.WAITING_EXPOSURE_NON_PRECAPTURE);
        }
        break;
      case WAITING_EXPOSURE_NON_PRECAPTURE:
        // CONTROL_AE_STATE can be null on some devices
        autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
        if (autoExposureState == null || autoExposureState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
          this.shootingActivity.setCameraState(ShootingActivity.CameraState.PICTURE_TAKEN);
          this.shootingActivity.captureStillPicture();
        }
        break;
    }
  }

  @Override
  public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                  @NonNull CaptureRequest request,
                                  @NonNull CaptureResult partialResult) {
    process(partialResult);
  }

  @Override
  public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                 @NonNull CaptureRequest request,
                                 @NonNull TotalCaptureResult result) {
    process(result);
  }
}
