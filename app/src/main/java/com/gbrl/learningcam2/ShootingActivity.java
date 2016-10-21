package com.gbrl.learningcam2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

public class ShootingActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

  private static final int REQUEST_CAMERA_PERMISSION = 1;
  private String cameraId;
  private ImageReader imageReader;
  private TextureView textureView;
  private CameraState cameraState;
  private CameraDevice cameraDevice;
  private CameraManager cameraManager;
  private CameraCaptureSession captureSession;
  private CaptureRequest previewRequest, captureRequest;
  private CaptureRequest.Builder previewRequestBuilder, captureRequestBuilder;

  public enum CameraState {
    PREVIEW // Showing camera preview
    , WAITING_FOCUS_LOCK // Waiting for the focus to be locked
    , WAITING_EXPOSURE_PRECAPTURE // Waiting for the exposure to be precapture state
    , WAITING_EXPOSURE_NON_PRECAPTURE // Waiting for the exposure state to be something other than precapture
    , PICTURE_TAKEN
  }

  private CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

    private void process(CaptureResult result) {
      Integer autoFocusState = null, autoExposureState = null;
      switch (ShootingActivity.this.cameraState) {
        case PREVIEW:
          // We have nothing to do when the camera preview is working normally.
          break;
        case WAITING_FOCUS_LOCK:
          autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
          if (autoFocusState == null) {
           // captureStillPicture();
          } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == autoFocusState ||
              CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == autoFocusState) {
            // CONTROL_AE_STATE can be null on some devices
            autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (autoExposureState == null ||
                autoExposureState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
              ShootingActivity.this.cameraState = CameraState.PICTURE_TAKEN;
             // captureStillPicture();
            } else {
             // runPrecaptureSequence();
            }
          }
          break;
        case WAITING_EXPOSURE_PRECAPTURE:
          // CONTROL_AE_STATE can be null on some devices
          autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
          if (autoExposureState == null ||
              autoExposureState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
              autoExposureState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
            ShootingActivity.this.cameraState = CameraState.WAITING_EXPOSURE_NON_PRECAPTURE;
          }
          break;
        case WAITING_EXPOSURE_NON_PRECAPTURE:
          // CONTROL_AE_STATE can be null on some devices
          autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
          if (autoExposureState == null || autoExposureState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
            ShootingActivity.this.cameraState = CameraState.PICTURE_TAKEN;
            // captureStillPicture();
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

  };

  private final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

    @Override
    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
      // The camera is already closed
      if (ShootingActivity.this.cameraDevice == null) {
        return;
      }

      // When the session is ready, we start displaying the preview.
      ShootingActivity.this.captureSession = cameraCaptureSession;
      try {
        // Auto focus should be continuous for camera preview.
        ShootingActivity.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        // Finally, we start displaying the camera preview.
        ShootingActivity.this.previewRequest = ShootingActivity.this.previewRequestBuilder.build();
        ShootingActivity.this.captureSession.setRepeatingRequest(ShootingActivity.this.previewRequest, ShootingActivity.this.sessionCaptureCallback, null);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
      // showToast("Failed");
    }
  };

  private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
      ShootingActivity.this.cameraDevice = cameraDevice;
      ShootingActivity.this.cameraState = CameraState.PREVIEW;
      Surface previewSurface = new Surface(ShootingActivity.this.textureView.getSurfaceTexture());
      Surface pictureSurface = ShootingActivity.this.imageReader.getSurface();

      List<Surface> outputs = new ArrayList<>();
      outputs.add(previewSurface);
      outputs.add(pictureSurface);

      try {
        ShootingActivity.this.previewRequestBuilder = ShootingActivity.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        ShootingActivity.this.previewRequestBuilder.addTarget(previewSurface);
        ShootingActivity.this.previewRequest = ShootingActivity.this.previewRequestBuilder.build();

        ShootingActivity.this.captureRequestBuilder = ShootingActivity.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        ShootingActivity.this.captureRequestBuilder.addTarget(pictureSurface);
        ShootingActivity.this.captureRequest = ShootingActivity.this.captureRequestBuilder.build();

        ShootingActivity.this.cameraDevice.createCaptureSession(outputs, ShootingActivity.this.sessionStateCallback, null);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
      cameraDevice.close();
      ShootingActivity.this.cameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
      cameraDevice.close();
      ShootingActivity.this.cameraDevice = null;
      ShootingActivity.this.finish();
    }

  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.shooting_layout);

    this.cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    this.textureView = (TextureView) this.findViewById(R.id.textureView);
    // this.textureView = new TextureView(this);
    this.textureView.setSurfaceTextureListener(this);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        try {
          this.cameraManager.openCamera(this.cameraId, this.cameraStateCallback, null);
        } catch (CameraAccessException e) {
          e.printStackTrace();
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    try {
      for (String cameraId : this.cameraManager.getCameraIdList()) {
        CameraCharacteristics characteristics = this.cameraManager.getCameraCharacteristics(cameraId);

        Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (configurationMap == null) {
          continue;
        }

        Size[] sizes = configurationMap.getOutputSizes(ImageFormat.JPEG);
        this.imageReader = ImageReader.newInstance(sizes[0].getWidth(), sizes[0].getHeight(), ImageFormat.JPEG, 2);
        Surface suface = this.imageReader.getSurface();

        this.cameraId = cameraId;
      }
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
      } else {
        this.cameraManager.openCamera(this.cameraId, this.cameraStateCallback, null);
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {

  }
}
