package com.gbrl.learningcam2.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by gbrl on 11/17/16.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {

  private static final int MAX_ITEMS = 10;

  public PagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int position) {
    return AlbumFragment.newInstance(position);
  }

  @Override
  public int getCount() {
    return PagerAdapter.MAX_ITEMS;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return "Album " + Integer.toString(position + 1);
  }
}
