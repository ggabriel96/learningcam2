package com.gbrl.learningcam2.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.gbrl.learningcam2.R;

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
import java.util.List;

/**
 * @TODO better modularize camera-related classes so that this activity can go to the activities package
 */
public class ShootingActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,
    SensorEventListener {

  private static final String LOG_TAG = "SA";
  private static final int REQUEST_ALL_PERMISSIONS = 1;
  private static final String[] requiredPermissions = {
      Manifest.permission.CAMERA
      , Manifest.permission.WRITE_EXTERNAL_STORAGE
  };
  /**
   * Conversion from screen rotation to JPEG orientation.
   */
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ShootingActivity.ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ShootingActivity.ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ShootingActivity.ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ShootingActivity.ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  private final LocationHandler locationHandler = new LocationHandler(this);
  private final CameraStateCallback cameraStateCallback = new CameraStateCallback(this);
  private final SessionStateCallback sessionStateCallback = new SessionStateCallback(this);
  private final SessionCaptureCallback sessionCaptureCallback = new SessionCaptureCallback(this);
  private String cameraId;
  private File latestPhotoFile;
  /**
   * This callback will be called when a still image is ready to be saved.
   */
  private final ImageReader.OnImageAvailableListener onImageAvailableListener =
      new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
          Log.d(LOG_TAG, "onImageAvailable");
          if (ShootingActivity.this.setupPhotoFile()) {
            ShootingActivity.this.saveImage(reader.acquireLatestImage());
            ShootingActivity.this.broadcastNewPicture();
          }
        }
      };
  private int sensorOrientation;
  private ImageReader imageReader;
  private TextureView textureView;
  private CameraState cameraState;
  private CameraDevice cameraDevice;
  private CameraManager cameraManager;
  private SensorManager sensorManager;
  private CaptureRequest previewRequest;
  private CameraCaptureSession captureSession;
  private GestureDetectorCompat gestureDetector;
  private OrientationManager orientationManager;
  private Sensor accelerometer, gyroscope, rotation;
  private CaptureRequest.Builder previewRequestBuilder;
  private float[] accelerometerValues, gyroscopeValues, rotationValues;
  private Integer accelerometerAccuracy, gyroscopeAccuracy, rotationAccuracy;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(LOG_TAG, "onCreate");
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.shooting_layout);
    this.setup();
    this.setRotationAnimation();
    this.locationHandler.build();
  }

  @Override
  protected void onStart() {
    Log.d(LOG_TAG, "onStart");
    super.onStart();
    this.locationHandler.connect(Boolean.TRUE);
  }

  @Override
  protected void onResume() {
    Log.d(LOG_TAG, "onResume");
    super.onResume();
    this.registerSensors();
    this.orientationManager.enable();
    this.locationHandler.startLocationUpdates();
    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).
    if (this.textureView.isAvailable()) {
      Log.d(LOG_TAG, "onResume textureView is available, openCamera");
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
    Log.d(LOG_TAG, "onPause");
    super.onPause();
    this.closeCamera();
    this.orientationManager.disable();
    this.locationHandler.stopLocationUpdates();
    this.sensorManager.unregisterListener(this);
  }

  @Override
  protected void onStop() {
    Log.d(LOG_TAG, "onStop");
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    Log.d(LOG_TAG, "onDestroy");
    super.onDestroy();
    this.locationHandler.disconnect();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.d(LOG_TAG, "onConfigurationChanged");
    super.onConfigurationChanged(newConfig);
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      Log.d(LOG_TAG, "LANDSCAPE");
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      Log.d(LOG_TAG, "PORTRAIT");
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    Log.d(LOG_TAG, "onRequestPermissionsResult");
    if (requestCode == ShootingActivity.REQUEST_ALL_PERMISSIONS) return;
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    this.gestureDetector.onTouchEvent(event);
    return super.onTouchEvent(event);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    Log.d(LOG_TAG, "onWindowFocusChanged");
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      this.goFullscreen();
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    Log.d(LOG_TAG, "onSurfaceTextureAvailable");
    try {
      this.openCamera();
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (this.accelerometer != null && event.sensor.equals(this.accelerometer)) {
      this.accelerometerValues = event.values;
    } else if (this.gyroscope != null && event.sensor.equals(this.gyroscope)) {
      this.gyroscopeValues = event.values;
    } else if (this.rotation != null && event.sensor.equals(this.rotation)) {
      this.rotationValues = event.values;
    }
    // StringBuffer sb = new StringBuffer("Sensor values: ");
    // for (int i = 0; i < event.values.length; i++) {
    //   if (i > 0) sb.append(", ");
    //   sb.append(event.values[i]);
    // }
    // Log.i(LOG_TAG, "onSensorChanged " + sb.toString());
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    if (sensor.equals(this.accelerometer)) {
      this.accelerometerAccuracy = accuracy;
    } else if (sensor.equals(this.gyroscope)) {
      this.gyroscopeAccuracy = accuracy;
    } else if (sensor.equals(this.rotation)) {
      this.rotationAccuracy = accuracy;
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    Log.d(LOG_TAG, "onSurfaceTextureSizeChanged");
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    Log.d(LOG_TAG, "onSurfaceTextureDestroyed");
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {
  }

  private void setRotationAnimation() {
    Window window = this.getWindow();
    WindowManager.LayoutParams windowAttributes = window.getAttributes();
    windowAttributes.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
    window.setAttributes(windowAttributes);
  }

  protected String uniqueImageName() {
    Log.d(LOG_TAG, "uniqueImageName");
    return "JPEG_" + new SimpleDateFormat("yyyy-MM-dd-HHmmss")
        .format(Calendar.getInstance().getTime()) + ".jpg";
  }

  private Boolean setupPhotoFile() {
    Log.d(LOG_TAG, "setupPhotoFile");
    this.latestPhotoFile = null;
    File publicPicturesDirectory =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File icPicturesDirectory = new File(
        publicPicturesDirectory.getPath() + File.separator + getResources()
            .getString(R.string.pictures_directory));
    Log.d(LOG_TAG, "getExternalStoragePublicDirectory: " + publicPicturesDirectory.getPath());
    Log.d(LOG_TAG, "icPicturesDirectory: " + icPicturesDirectory.getPath());
    if (icPicturesDirectory.exists() || icPicturesDirectory.mkdir()) {
      this.latestPhotoFile =
          new File(icPicturesDirectory.getPath() + File.separator + this.uniqueImageName());
    }
    return this.latestPhotoFile != null;
  }

  private void saveImage(Image image) {
    Log.d(LOG_TAG, "saveImage");
    if (this.latestPhotoFile == null || image == null) return;
    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(this.latestPhotoFile);
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

  protected void takePicture() {
    Log.d(LOG_TAG, "takePicture");
    try {
      // Tell captureCallback to wait for the lock.
      this.cameraState = CameraState.WAITING_FOCUS_LOCK;
      // Lock the focus as the first step for a still image capture.
      this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
      this.captureSession.capture(this.previewRequestBuilder.build(), this.sessionCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Run the precapture sequence for capturing a still image. This method should be called when
   * we get a response in captureCallback from takePicture().
   */
  protected void runPrecaptureSequence() {
    Log.d(LOG_TAG, "runPrecaptureSequence");
    try {
      // This is how to tell the camera to trigger.
      this.previewRequestBuilder
          .set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
      // Tell captureCallback to wait for the precapture sequence to be set.
      this.cameraState = CameraState.WAITING_EXPOSURE_PRECAPTURE;
      this.captureSession
          .capture(ShootingActivity.this.previewRequestBuilder.build(), ShootingActivity.this.sessionCaptureCallback,
                   null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  protected int getDisplayRotation() {
    Log.d(LOG_TAG, "getDisplayRotation");
    return this.getWindowManager().getDefaultDisplay().getRotation();
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
   * Code taken from:
   * {@code http://stackoverflow.com/questions/5877780/orientation-from-android-accelerometer}
   * <br><br>
   * For more info, see {@code http://developer.download.nvidia.com/tegra/docs/tegra_android_accelerometer_v5f.pdf}
   *
   * @param displayRotation display rotation from {@code android.view.Display.getRotation()}
   * @param eventValues raw {@code float[3]} accelerometer values
   * @return {@code float[3]}
   */
  private float[] adjustAccelerometerValues(int displayRotation, @NonNull float[] eventValues) {
    float[] adjustedValues = new float[3];

    final int axisSwap[][] = {
        {1, -1, 0, 1},  // ROTATION_0
        {-1, -1, 1, 0}, // ROTATION_90
        {-1, 1, 0, 1},  // ROTATION_180
        {1, 1, 1, 0}    // ROTATION_270
    };

    final int[] as = axisSwap[displayRotation];
    adjustedValues[0] = (float) as[0] * eventValues[as[2]];
    adjustedValues[1] = (float) as[1] * eventValues[as[3]];
    adjustedValues[2] = eventValues[2];

    return adjustedValues;
  }

  /**
   * @param accelerometerValues raw {@code float[3]} accelerometer values
   * @return {@link android.view.Surface} rotation constant indicating the current screen orientation
   */
  private int getRotationFromAccelerometer(@NonNull float[] accelerometerValues) {
    float[] adjustedValues = this.adjustAccelerometerValues(this.getDisplayRotation(), accelerometerValues);
    if (adjustedValues[0] >= -4.9f && adjustedValues[0] <= 4.9f) {
      if (adjustedValues[1] <= 0.0f) {
        Log.v(ShootingActivity.LOG_TAG, "Surface.ROTATION_0, portrait");
        return Surface.ROTATION_0; // portrait
      }
      Log.v(ShootingActivity.LOG_TAG, "Surface.ROTATION_180, reverse-portrait");
      return Surface.ROTATION_180; // reverse-portrait
    }
    if (adjustedValues[0] <= 0.0f) {
      Log.v(ShootingActivity.LOG_TAG, "Surface.ROTATION_270, reverse-landscape");
      return Surface.ROTATION_270; // reverse-landscape
    }
    Log.v(ShootingActivity.LOG_TAG, "Surface.ROTATION_90, landscape");
    return Surface.ROTATION_90; // defaults to landscape
  }

  /**
   * Capture a still picture
   */
  protected void captureStillPicture() {
    Log.d(LOG_TAG, "captureStillPicture");
    try {
      if (this.cameraDevice == null) {
        return;
      }
      final CaptureRequest.Builder captureRequestBuilder =
          this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureRequestBuilder.addTarget(this.imageReader.getSurface());
      // Use the same AE and AF modes as the preview.
      captureRequestBuilder
          .set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

      int rotation = this.getRotationFromAccelerometer(this.accelerometerValues);
      Log.e(ShootingActivity.LOG_TAG, Integer.toString(rotation));
      captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, this.getOrientation(rotation));

      CameraCaptureSession.CaptureCallback captureCallback =
          new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
              Log.d(LOG_TAG, "captureStillPicture captureCallback.onCaptureCompleted");
              ShootingActivity.this.unlockFocus();
            }
          };

      this.captureSession.stopRepeating();
      this.captureSession.capture(captureRequestBuilder.build(), captureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void broadcastNewPicture() {
    Log.d(LOG_TAG, "broadcastNewPicture");
    Uri contentUri = Uri.fromFile(this.latestPhotoFile);
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
    this.sendBroadcast(mediaScanIntent);
  }

  /**
   * Unlock the focus. This method should be called when still image capture sequence is
   * finished.
   */
  private void unlockFocus() {
    Log.d(LOG_TAG, "unlockFocus");
    try {
      this.cameraState = CameraState.PREVIEW;
      // Reset the auto-focus trigger
      this.previewRequestBuilder
          .set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
      // After this, the camera will go back to the normal state of preview.
      this.captureSession.capture(this.previewRequest, this.sessionCaptureCallback, null);
      this.captureSession
          .setRepeatingRequest(this.previewRequest, this.sessionCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void setup() {
    Log.d(LOG_TAG, "setup");
    this.orientationManager = new OrientationManager(this);
    this.gestureDetector = new GestureDetectorCompat(this, new GestureListener(this));
    this.cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    this.textureView = (TextureView) this.findViewById(R.id.textureView);
    this.initSensors();
  }

  /**
   * Closes the current CameraDevice
   */
  private void closeCamera() {
    Log.d(LOG_TAG, "closeCamera");
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

  private void configureTransform() {
    Log.d(LOG_TAG, "configureTransform");
    float scale = (float) this.imageReader.getHeight() / this.imageReader.getWidth();
    Log.d(LOG_TAG, "scale: " + scale);
    Matrix transform = new Matrix();
    this.textureView.getTransform(transform);
    transform.setScale(1.0f, scale);
    this.textureView.setTransform(transform);
  }

  private void setupCamera() throws CameraAccessException {
    Log.d(LOG_TAG, "setupCamera");
    for (String cameraId : this.cameraManager.getCameraIdList()) {
      CameraCharacteristics characteristics = this.cameraManager.getCameraCharacteristics(cameraId);

      Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
      if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
        continue;
      }

      StreamConfigurationMap configurationMap =
          characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      if (configurationMap == null) {
        continue;
      }

      this.sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

      Size[] sizes = configurationMap.getOutputSizes(ImageFormat.JPEG);
      Size largest = Collections.max(Arrays.asList(sizes), new CompareSizesByArea());
      this.imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
      this.imageReader.setOnImageAvailableListener(this.onImageAvailableListener, null);
      this.configureTransform();

      this.cameraId = cameraId;
      break;
    }
  }

  private Boolean hasRequiredPermissions() {
    Log.d(LOG_TAG, "hasRequiredPermissions");
    for (String permission : ShootingActivity.requiredPermissions) {
      if (ActivityCompat.checkSelfPermission(this, permission)
          != PackageManager.PERMISSION_GRANTED) {
        return Boolean.FALSE;
      }
    }
    return Boolean.TRUE;
  }

  private void requestPermissions() {
    Log.d(LOG_TAG, "requestPermissions");
    PackageInfo packageInfo = null;
    List<String> missingPermissions = new ArrayList<>();
    try {
      packageInfo = this.getPackageManager()
          .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    for (String permission : packageInfo.requestedPermissions) {
      if (ActivityCompat.checkSelfPermission(this, permission)
          != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(permission);
      }
    }
    // https://shipilev.net/blog/2016/arrays-wisdom-ancients/
    String[] permissions = missingPermissions.toArray(new String[0]);
    ActivityCompat.requestPermissions(this, permissions, ShootingActivity.REQUEST_ALL_PERMISSIONS);
  }

  private void openCamera() throws CameraAccessException, SecurityException {
    Log.d(LOG_TAG, "openCamera");
    if (this.hasRequiredPermissions()) {
      this.setupCamera();
      this.cameraManager.openCamera(this.cameraId, this.cameraStateCallback, null);
    } else {
      this.requestPermissions();
    }
  }

  private void goFullscreen() {
    Log.d(LOG_TAG, "goFullscreen");
    this.textureView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    );
  }

  private void initSensors() {
    Log.d(LOG_TAG, "initSensors");
    this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    // The accuracy of this sensor is lower than the normal rotation vector sensor,
    // but the power consumption is reduced. Better for background processing
    // this.rotation = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
    this.rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
  }

  private void registerSensors() {
    Log.d(LOG_TAG, "registerSensors");
    if (this.accelerometer != null) {
      Log.d(LOG_TAG, "registerSensors: Accelerometer");
      this.sensorManager
          .registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    } else {
      Log.d(LOG_TAG, "registerSensors: Accelerometer not available!");
    }

    if (this.gyroscope != null) {
      Log.d(LOG_TAG, "registerSensors: Gyroscope");
      this.sensorManager.registerListener(this, this.gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    } else {
      Log.d(LOG_TAG, "registerSensors: Gyroscope not available!");
    }

    if (this.rotation != null) {
      Log.d(LOG_TAG, "registerSensors: Rotation vector");
      this.sensorManager.registerListener(this, this.rotation, SensorManager.SENSOR_DELAY_NORMAL);
    } else {
      Log.d(LOG_TAG, "registerSensors: Rotation vector not available!");
    }
  }

  public CameraState getCameraState() {
    return cameraState;
  }

  public void setCameraState(CameraState cameraState) {
    this.cameraState = cameraState;
  }

  public CameraDevice getCameraDevice() {
    return cameraDevice;
  }

  public void setCameraDevice(CameraDevice cameraDevice) {
    this.cameraDevice = cameraDevice;
  }

  public TextureView getTextureView() {
    return textureView;
  }

  public void setTextureView(TextureView textureView) {
    this.textureView = textureView;
  }

  public ImageReader getImageReader() {
    return imageReader;
  }

  public void setImageReader(ImageReader imageReader) {
    this.imageReader = imageReader;
  }

  public CaptureRequest.Builder getPreviewRequestBuilder() {
    return previewRequestBuilder;
  }

  public void setPreviewRequestBuilder(CaptureRequest.Builder previewRequestBuilder) {
    this.previewRequestBuilder = previewRequestBuilder;
  }

  public CameraCaptureSession getCaptureSession() {
    return captureSession;
  }

  public void setCaptureSession(CameraCaptureSession captureSession) {
    this.captureSession = captureSession;
  }

  public CaptureRequest getPreviewRequest() {
    return previewRequest;
  }

  public void setPreviewRequest(CaptureRequest previewRequest) {
    this.previewRequest = previewRequest;
  }

  public SessionCaptureCallback getSessionCaptureCallback() {
    return sessionCaptureCallback;
  }

  public SessionStateCallback getSessionStateCallback() {
    return sessionStateCallback;
  }

  public enum CameraState {
    PREVIEW // Showing camera preview
    , WAITING_FOCUS_LOCK // Waiting for the focus to be locked
    , WAITING_EXPOSURE_PRECAPTURE // Waiting for the exposure to be precapture state
    , WAITING_EXPOSURE_NON_PRECAPTURE // Waiting for the exposure state to be something other than precapture
    , PICTURE_TAKEN
  }

  /**
   * Compares two {@code Size}s based on their areas.
   */
  static class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum(
          (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }
}
