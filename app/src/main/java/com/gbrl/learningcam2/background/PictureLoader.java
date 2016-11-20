package com.gbrl.learningcam2.background;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * Created by gbrl on 11/19/16.
 */

public class PictureLoader extends AsyncTask<String, Void, Bitmap> {

  private ImageView imageView;

  public PictureLoader(ImageView imageView) {
    this.imageView = imageView;
  }

  @Override
  protected Bitmap doInBackground(String... params) {
    return BitmapFactory.decodeFile(params[0]);
  }

  @Override
  protected void onPostExecute(Bitmap bitmap) {
    this.imageView.setImageBitmap(bitmap);
  }

}
