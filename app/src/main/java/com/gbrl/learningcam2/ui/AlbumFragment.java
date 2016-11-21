package com.gbrl.learningcam2.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gbrl.learningcam2.R;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Created by gbrl on 11/17/16.
 */

public class AlbumFragment extends Fragment {

  private static final String LOG_TAG = "AF";

  /**
   * Absolute path from File object, prefixed with <code>file:</code> for Picasso.
   */
  private String absolutePath;

  public AlbumFragment() {}

  static AlbumFragment newInstance(File imageFile) {
    Log.d(AlbumFragment.LOG_TAG, "newInstance");
    AlbumFragment af = new AlbumFragment();
    if (imageFile != null) {
      Bundle arguments = new Bundle();
      arguments.putString("absolutePath", imageFile.getAbsolutePath());
      af.setArguments(arguments);
    }
    return af;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(AlbumFragment.LOG_TAG, "onCreate");
    super.onCreate(savedInstanceState);
    Bundle arguments = this.getArguments();
    if (arguments != null) {
      this.absolutePath = "file:" + arguments.getString("absolutePath");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(AlbumFragment.LOG_TAG, "onCreateView");
    View album = inflater.inflate(R.layout.album_layout, container, false);
    if (this.absolutePath != null) {
      Log.d(AlbumFragment.LOG_TAG, "Loading " + this.absolutePath);
      ImageView albumCover = (ImageView) album.findViewById(R.id.album_cover);
      Picasso.with(album.getContext()).load(this.absolutePath).into(albumCover);
    }
    return album;
  }
}
