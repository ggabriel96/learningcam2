package com.gbrl.learningcam2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;

/**
 * Created by gbrl on 10/31/16.
 */

public class SessionStateCallback extends CameraCaptureSession.StateCallback {

  private ShootingActivity shootingActivity;

  public SessionStateCallback(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
  }

  @Override
  public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
    // The camera is already closed
    if (this.shootingActivity.getCameraDevice() == null) {
      return;
    }

    // When the session is ready, we start displaying the preview.
    this.shootingActivity.setCaptureSession(cameraCaptureSession);
    try {
      // Auto focus should be continuous for camera preview.
      this.shootingActivity.getPreviewRequestBuilder().set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

      // Finally, we start displaying the camera preview.
      // Save the previewRequest for when we go back to the original state after taking a picture, for example
      this.shootingActivity.setPreviewRequest(this.shootingActivity.getPreviewRequestBuilder().build());
      this.shootingActivity.getCaptureSession().setRepeatingRequest(this.shootingActivity.getPreviewRequest(), this.shootingActivity.getSessionCaptureCallback(), null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
    // showToast("Failed");
  }
}
