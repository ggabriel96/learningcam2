package com.gbrl.learningcam2.camera;

import android.view.OrientationEventListener;

/**
 * Created by gbrl on 11/3/16.
 */

public class OrientationManager extends OrientationEventListener {

  private static final String LOG_TAG = "OM";

  private ShootingActivity shootingActivity;
  private int lastRotationDegrees;

  public OrientationManager(ShootingActivity shootingActivity) {
    super(shootingActivity);
    this.lastRotationDegrees = 0;
    this.shootingActivity = shootingActivity;
  }

  @Override
  public void onOrientationChanged(int orientation) {
    // Log.d(LOG_TAG, "onOrientationChanged: " + orientation);
    // int dist = Math.abs(orientation - this.lastRotationDegrees);
    // dist = Math.min(dist, 360 - dist);
    // boolean isOrientationChanged = dist > 45;
    //
    // if (isOrientationChanged) {
    //   int newRoundedRotation = ((orientation + 45) / 90 * 90) % 360;
    //   Log.d(LOG_TAG, "lastRotation: " + this.lastRotationDegrees + "\nnewRotation: " + newRoundedRotation);
    //   this.configureTransform((float) newRoundedRotation);
    //   this.lastRotationDegrees = newRoundedRotation;
    // }
  }
}
