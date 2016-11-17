package com.gbrl.learningcam2.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gbrl.learningcam2.R;

/**
 * Created by gbrl on 11/17/16.
 */

public class AlbumFragment extends Fragment {

  private int position;

  public AlbumFragment() {}

  static AlbumFragment newInstance(int position) {
    AlbumFragment a = new AlbumFragment();
    Bundle args = new Bundle();
    args.putInt("position", position);
    a.setArguments(args);
    return a;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.position = this.getArguments() != null ? this.getArguments().getInt("position") : 1;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View album = inflater.inflate(R.layout.album_layout, container, false);
    TextView albumName = (TextView) album.findViewById(R.id.album_title);
    albumName.setText("Album #" + this.position);
    return album;
  }

}
