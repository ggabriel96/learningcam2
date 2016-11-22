package com.gbrl.learningcam2.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.gbrl.learningcam2.R;
import com.gbrl.learningcam2.background.PictureReceiver;
import com.gbrl.learningcam2.camera.ShootingActivity;
import com.gbrl.learningcam2.ui.ImagePagerAdapter;

import java.io.File;

public class Home extends AppCompatActivity {

  private static final String LOG_TAG = "H";
  private SeekBar seekBar;
  private ViewPager viewPager;
  private PictureReceiver pictureReceiver;
  private ImagePagerAdapter imagePagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(Home.LOG_TAG, "onCreate");
    this.setContentView(R.layout.home_layout);

    Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
    this.setSupportActionBar(toolbar);

    File publicPicturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File picturesDirectory = new File(publicPicturesDirectory.getPath() + File.separator + getResources()
        .getString(R.string.pictures_directory));
    Log.d(LOG_TAG, "picturesDirectory: " + picturesDirectory.getPath());

    this.imagePagerAdapter = new ImagePagerAdapter(this.getSupportFragmentManager(), picturesDirectory);
    this.pictureReceiver = new PictureReceiver(this.imagePagerAdapter);
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(this.pictureReceiver, new IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));

    this.seekBar = (SeekBar) this.findViewById(R.id.seek_bar);
    // starts at zero and goes up to this max, inclusive
    this.seekBar.setMax(this.imagePagerAdapter.getCount() - 1);
    this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          Home.this.viewPager.setCurrentItem(progress);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    // Set up the ViewPager with the sections adapter.
    this.viewPager = (ViewPager) this.findViewById(R.id.pager);
    this.viewPager.setAdapter(imagePagerAdapter);
    this.viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        Home.this.seekBar.setProgress(position);
        Log.i(Home.LOG_TAG, "viewPager page changed to " + Integer.toString(position));
      }
    });

    FloatingActionButton fab = (FloatingActionButton) this.findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent camera = new Intent(Home.this, ShootingActivity.class);
        Home.this.startActivity(camera);
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(Home.LOG_TAG, "onDestroy");
    this.viewPager.clearOnPageChangeListeners();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(this.pictureReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    this.getMenuInflater().inflate(R.menu.menu_home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent settings = new Intent(this, Settings.class);
        this.startActivity(settings);
        return true;
      case R.id.about:
        Toast.makeText(this, "Toast!", Toast.LENGTH_SHORT).show();
        return true;
      // case R.id.add:
      //   RelativeLayout relativeLayout = (RelativeLayout) this.findViewById(R.id.album_layout);
      //   ImageView albumCover = new ImageView(relativeLayout.getContext());
      //   albumCover.setBackgroundResource(R.drawable.ic_add_black_24dp);
      //   relativeLayout.addView(albumCover);
      //   return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
