package com.gbrl.learningcam2.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by gbrl on 11/17/16.
 */

public class ImagePagerAdapter extends FragmentPagerAdapter {

  private static final String LOG_TAG = "IPA";

  private List<File> imageFiles;

  public ImagePagerAdapter(FragmentManager fm, File imagesDirectory) {
    super(fm);
    /**
     * @TODO check storage permission, throws NPE here
     */
    File[] files = imagesDirectory.listFiles();
    if (files != null)
      this.imageFiles = new ArrayList<>(Arrays.asList(imagesDirectory.listFiles()));
    else this.imageFiles = new ArrayList<>();
  }

  @Override
  public Fragment getItem(int position) {
    Log.d(ImagePagerAdapter.LOG_TAG, "getItem");
    if (!this.imageFiles.isEmpty()) {
      return AlbumFragment.newInstance(this.imageFiles.get(position));
    } else {
      Log.d(ImagePagerAdapter.LOG_TAG, "imageFiles is empty");
      return new AlbumFragment();
    }
  }

  @Override
  public int getCount() {
    return this.imageFiles.size();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return this.imageFiles.get(position).getName();
  }

  /**
   * @TODO implement a content observer
   */
  public void addImageFile(File imageFile) {
    this.imageFiles.add(imageFile);
  }
}
