package com.gbrl.learningcam2;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by gbrl on 11/1/16.
 */

public class LocationHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private GoogleApiClient googleApiClient;
  private ShootingActivity shootingActivity;

  private static final String LOG_TAG = "LH";

  public LocationHandler(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) throws SecurityException {
    Location location = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);
    if (location != null) {
      StringBuffer sb = new StringBuffer("Location:");
      sb.append("\nLatitude: ").append(location.getLatitude()).append("\nLongitude: ").append(location.getLongitude())
          .append("\nAltitude: ").append(location.getAltitude()).append("\nAccuracy: ").append(location.getAccuracy())
          .append("\nBearing: ").append(location.getBearing()).append("\nProvider: ").append(location.getProvider())
          .append("\nTime: ").append(location.getTime()).append("\nNumber of satellites: ");
      if (location.getExtras() != null) {
        sb.append(location.getExtras().get("satellites"));
      }
      Log.i(LOG_TAG, sb.toString());
    }
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  protected void build() {
    if (this.googleApiClient == null) {
      this.googleApiClient = new GoogleApiClient.Builder(this.shootingActivity)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .addApi(LocationServices.API)
          .build();
    }
  }

  protected void connect() {
    this.googleApiClient.connect();
  }

  protected void disconnect() {
    this.googleApiClient.disconnect();
  }
}
