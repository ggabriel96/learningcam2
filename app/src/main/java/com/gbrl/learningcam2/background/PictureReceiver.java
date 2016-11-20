package com.gbrl.learningcam2.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gbrl.learningcam2.ui.ImagePagerAdapter;

import java.io.File;

/**
 * Created by gbrl on 11/19/16.
 */

public class PictureReceiver extends BroadcastReceiver {

  private static final String LOG_TAG = "PR";

  private ImagePagerAdapter imagePagerAdapter;

  public PictureReceiver(ImagePagerAdapter imagePagerAdapter) {
    this.imagePagerAdapter = imagePagerAdapter;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    File newImageFile = new File(intent.getData().getPath());
    Log.d(PictureReceiver.LOG_TAG, newImageFile.getAbsolutePath());
    this.imagePagerAdapter.addImageFile(newImageFile);
  }
}
