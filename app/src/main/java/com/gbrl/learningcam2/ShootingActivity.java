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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShootingActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

  private static final int REQUEST_ALL_PERMISSIONS = 1;
  // private static final int REQUEST_CAMERA_PERMISSION = 1;
  // private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 2;

  private String cameraId;
  private File latestPhotoFile;
  private int sensorOrientation;
  private ImageReader imageReader;
  private TextureView textureView;
  private CameraState cameraState;
  private CameraDevice cameraDevice;
  private CameraManager cameraManager;
  private CaptureRequest previewRequest;
  private CameraCaptureSession captureSession;
  private CaptureRequest.Builder previewRequestBuilder;
  private Boolean hasCameraPermission, hasWriteExternalStoragePermission;

  /**
   * Conversion from screen rotation to JPEG orientation.
   */
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

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
        // case PREVIEW:
        // We have nothing to do when the camera preview is working normally.
        // break;
        case WAITING_FOCUS_LOCK:
          autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
          if (autoFocusState == null) {
            ShootingActivity.this.captureStillPicture();
          } else if (autoFocusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || autoFocusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
            // CONTROL_AE_STATE can be null on some devices
            autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (autoExposureState == null || autoExposureState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
              ShootingActivity.this.cameraState = CameraState.PICTURE_TAKEN;
              ShootingActivity.this.captureStillPicture();
            } else {
              ShootingActivity.this.runPrecaptureSequence();
            }
          }
          break;
        case WAITING_EXPOSURE_PRECAPTURE:
          // CONTROL_AE_STATE can be null on some devices
          autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
          if (autoExposureState == null || autoExposureState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || autoExposureState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
            ShootingActivity.this.cameraState = CameraState.WAITING_EXPOSURE_NON_PRECAPTURE;
          }
          break;
        case WAITING_EXPOSURE_NON_PRECAPTURE:
          // CONTROL_AE_STATE can be null on some devices
          autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
          if (autoExposureState == null || autoExposureState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
            ShootingActivity.this.cameraState = CameraState.PICTURE_TAKEN;
            ShootingActivity.this.captureStillPicture();
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
        // Save the previewRequest for when we go back to the original state after taking a picture, for example
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
      ShootingActivity.this.textureView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Log.i("onClick", "takePicture");
          ShootingActivity.this.takePicture();
        }
      });

      Surface previewSurface = new Surface(ShootingActivity.this.textureView.getSurfaceTexture());
      Log.i("onOpened", "ShootingActivity.this.imageReader.getSurface()");
      Surface pictureSurface = ShootingActivity.this.imageReader.getSurface();
      List<Surface> outputs = new ArrayList<>();
      outputs.add(previewSurface);
      outputs.add(pictureSurface);

      try {
        ShootingActivity.this.previewRequestBuilder = ShootingActivity.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        ShootingActivity.this.previewRequestBuilder.addTarget(previewSurface);
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

  /**
   * This callback will be called when a still image is ready to be saved.
   */
  private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
    @Override
    public void onImageAvailable(ImageReader reader) {
      if (ShootingActivity.this.setupPhotoFile())
        ShootingActivity.this.saveImage(reader.acquireLatestImage());
    }
  };

  private String uniqueImageName() {
    return "JPEG_" + new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(Calendar.getInstance().getTime()) + ".jpg";
  }

  private Boolean setupPhotoFile() {
    this.latestPhotoFile = null;
    File publicPicturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File icPicturesDirectory = new File(publicPicturesDirectory.getPath() + File.separator + getResources().getString(R.string.pictures_directory));
    Log.i("env pubextpic", publicPicturesDirectory.getPath());
    Log.i("ic dir", icPicturesDirectory.getPath());
    if (icPicturesDirectory.exists() || icPicturesDirectory.mkdir()) {
      this.latestPhotoFile = new File(icPicturesDirectory.getPath() + File.separator + this.uniqueImageName());
    }
    return this.latestPhotoFile != null;
  }

  private void saveImage(Image image) {
    if (ShootingActivity.this.latestPhotoFile == null || image == null) return;
    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(ShootingActivity.this.latestPhotoFile);
      output.write(bytes);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      image.close();
      if (output != null) {
        try {
          output.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void takePicture() {
    try {
      // Tell captureCallback to wait for the lock.
      ShootingActivity.this.cameraState = CameraState.WAITING_FOCUS_LOCK;
      // Lock the focus as the first step for a still image capture.
      ShootingActivity.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
      ShootingActivity.this.captureSession.capture(ShootingActivity.this.previewRequestBuilder.build(), ShootingActivity.this.sessionCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Run the precapture sequence for capturing a still image. This method should be called when
   * we get a response in captureCallback from takePicture().
   */
  private void runPrecaptureSequence() {
    try {
      // This is how to tell the camera to trigger.
      ShootingActivity.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
      // Tell captureCallback to wait for the precapture sequence to be set.
      ShootingActivity.this.cameraState = CameraState.WAITING_EXPOSURE_PRECAPTURE;
      ShootingActivity.this.captureSession.capture(ShootingActivity.this.previewRequestBuilder.build(), ShootingActivity.this.sessionCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Retrieves the JPEG orientation from the specified screen rotation.
   *
   * @param rotation The screen rotation.
   * @return The JPEG orientation (one of 0, 90, 270, and 360)
   */
  private int getOrientation(int rotation) {
    // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
    // We have to take that into account and rotate JPEG properly.
    // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
    // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
    return (ORIENTATIONS.get(rotation) + this.sensorOrientation + 270) % 360;
  }

  /**
   * Capture a still picture
   */
  private void captureStillPicture() {
    try {
      if (ShootingActivity.this.cameraDevice == null) {
        return;
      }
      final CaptureRequest.Builder captureRequestBuilder = ShootingActivity.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureRequestBuilder.addTarget(this.imageReader.getSurface());
      // Use the same AE and AF modes as the preview.
      captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

      // Orientation
      int rotation = ShootingActivity.this.getWindowManager().getDefaultDisplay().getRotation();
      captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

      CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
          Log.d("capt_done", "local onCaptureCompleted");
          ShootingActivity.this.unlockFocus();
        }
      };

      this.captureSession.stopRepeating();
      this.captureSession.capture(captureRequestBuilder.build(), captureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Unlock the focus. This method should be called when still image capture sequence is
   * finished.
   */
  private void unlockFocus() {
    try {
      this.cameraState = CameraState.PREVIEW;
      // Reset the auto-focus trigger
      this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
      // After this, the camera will go back to the normal state of preview.
      this.captureSession.capture(this.previewRequest, this.sessionCaptureCallback, null);
      this.captureSession.setRepeatingRequest(this.previewRequest, this.sessionCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void setup() {
    this.cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    this.textureView = (TextureView) this.findViewById(R.id.textureView);
    // this.textureView.setSurfaceTextureListener(this);
  }

  /**
   * Closes the current CameraDevice
   */
  private void closeCamera() {
    if (this.captureSession != null) {
      this.captureSession.close();
      this.captureSession = null;
    }
    if (this.cameraDevice != null) {
      this.cameraDevice.close();
      this.cameraDevice = null;
    }
    if (this.imageReader != null) {
      this.imageReader.close();
      this.imageReader = null;
    }
  }

  private void setupCamera() throws CameraAccessException {
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

      this.sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

      Size[] sizes = configurationMap.getOutputSizes(ImageFormat.JPEG);
      Size largest = Collections.max(Arrays.asList(sizes), new CompareSizesByArea());
      Log.i("for", "onSurfaceTextureAvailable ImageReader.newInstance");
      this.imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
      this.imageReader.setOnImageAvailableListener(this.onImageAvailableListener, null);

      this.cameraId = cameraId;
      break;
    }
  }

  private void openCamera() throws CameraAccessException, SecurityException {
    this.hasCameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    this.hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(ShootingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    if (this.hasCameraPermission && this.hasWriteExternalStoragePermission) {
      this.setupCamera();
      this.cameraManager.openCamera(this.cameraId, this.cameraStateCallback, null);
    } else {
      String[] permissions = new String[2];
      if (!this.hasCameraPermission) {
        permissions[0] = Manifest.permission.CAMERA;
      }
      if (!this.hasWriteExternalStoragePermission) {
        permissions[1] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
      }
      ActivityCompat.requestPermissions(ShootingActivity.this, permissions, ShootingActivity.REQUEST_ALL_PERMISSIONS);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.shooting_layout);
    this.setup();
  }

  @Override
  protected void onResume() {
    Log.i("ACTIVITY", "onResume");
    super.onResume();
    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).
    if (this.textureView.isAvailable()) {
      Log.i("onResume", "openCamera");
      try {
        this.openCamera();
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    } else {
      this.textureView.setSurfaceTextureListener(this);
    }
  }

  @Override
  protected void onPause() {
    Log.i("ACTIVITY", "onPause");
    super.onPause();
    this.closeCamera();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_ALL_PERMISSIONS:
        if (grantResults.length > 0) {
          if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            this.hasCameraPermission = Boolean.TRUE;
          if (grantResults[1] == PackageManager.PERMISSION_GRANTED)
            this.hasWriteExternalStoragePermission = Boolean.TRUE;
        }
        break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  /**
   * Compares two {@code Size}s based on their areas.
   */
  static class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    try {
      this.openCamera();
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
