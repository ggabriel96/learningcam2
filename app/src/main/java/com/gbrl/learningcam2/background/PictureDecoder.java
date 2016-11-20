package com.gbrl.learningcam2.background;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * Created by gbrl on 11/19/16.
 */

public class PictureDecoder extends AsyncTask<byte[], Void, Bitmap> {

  private ImageView imageView;

  public PictureDecoder(ImageView imageView) {
    this.imageView = imageView;
  }

  @Override
  protected Bitmap doInBackground(byte[]... params) {
    return BitmapFactory.decodeByteArray(params[0], 0, params[0].length);
  }

  @Override
  protected void onPostExecute(Bitmap bitmap) {
    this.imageView.setImageBitmap(bitmap);
  }
}
