// package com.gbrl.learningcam2;
//
// import android.hardware.camera2.CameraAccessException;
// import android.hardware.camera2.CameraDevice;
// import android.hardware.camera2.CaptureRequest;
// import android.view.Surface;
// import android.view.TextureView;
//
// import java.util.ArrayList;
// import java.util.List;
//
// /**
//  * Created by gbrl on 10/18/16.
//  */
//
// public class CameraStateCallback extends CameraDevice.StateCallback {
//
//     private ShootingActivity shootingActivity;
//     private SessionStateCallback stateCallback;
//     private CaptureRequest previewRequest, captureRequest;
//     private CaptureRequest.Builder previewRequestBuilder;
//     private CaptureRequest.Builder captureRequestBuilder;
//
//     public CameraStateCallback(ShootingActivity shootingActivity) {
//         this.shootingActivity = shootingActivity;
//         this.stateCallback = new SessionStateCallback();
//     }
//
//     @Override
//     public void onOpened(CameraDevice camera) {
//         this.shootingActivity.setCameraDevice(camera);
//         TextureView textureView = (TextureView) this.shootingActivity.findViewById(R.id.textureView);
//         Surface previewSurface = new Surface(textureView.getSurfaceTexture());
//         Surface pictureSurface = this.shootingActivity.getImageReader().getSurface();
//
//         List<Surface> outputs = new ArrayList<>();
//         outputs.add(previewSurface);
//         outputs.add(pictureSurface);
//
//         try {
//             previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//             previewRequestBuilder.addTarget(previewSurface);
//             this.previewRequest = previewRequestBuilder.build();
//
//             captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//             captureRequestBuilder.addTarget(pictureSurface);
//             this.captureRequest = captureRequestBuilder.build();
//
//             camera.createCaptureSession(outputs, this.stateCallback, null);
//         } catch (CameraAccessException e) {
//             e.printStackTrace();
//         }
//     }
//
//     @Override
//     public void onDisconnected(CameraDevice camera) {
//         camera.close();
//         this.shootingActivity.setCameraDevice(null);
//     }
//
//     @Override
//     public void onError(CameraDevice camera, int error) {
//         camera.close();
//         this.shootingActivity.setCameraDevice(null);
//         this.shootingActivity.finish();
//     }
//
// }
