package com.gbrl.learningcam2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.gbrl.learningcam2.R;
import com.gbrl.learningcam2.camera.ShootingActivity;
import com.gbrl.learningcam2.ui.PagerAdapter;

public class Home extends AppCompatActivity {

  private static final String LOG_TAG = "H";
  private SeekBar seekBar;
  private ViewPager viewPager;
  private PagerAdapter pagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.home_layout);

    Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
    this.setSupportActionBar(toolbar);

    this.pagerAdapter = new PagerAdapter(this.getSupportFragmentManager());

    this.seekBar = (SeekBar) this.findViewById(R.id.seek_bar);
    // starts at zero and goes up to this max, inclusive
    this.seekBar.setMax(this.pagerAdapter.getCount() - 1);
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
    this.viewPager.setAdapter(pagerAdapter);
    this.viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        Home.this.seekBar.setProgress(position);
        Log.i(Home.LOG_TAG, Integer.toString(position));
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
    this.viewPager.clearOnPageChangeListeners();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    this.getMenuInflater().inflate(R.menu.menu_home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      Intent settings = new Intent(this, Settings.class);
      this.startActivity(settings);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
