package com.gbrl.learningcam2;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by gbrl on 10/23/16.
 */

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

  private ShootingActivity shootingActivity;

  public GestureListener(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    Log.i("GESTURE", "onSingleTapConfirmed");
    this.shootingActivity.takePicture();
    return true;
  }
}
