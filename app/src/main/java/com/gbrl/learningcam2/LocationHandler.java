package com.gbrl.learningcam2;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by gbrl on 11/1/16.
 * https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi
 * https://developers.google.com/android/reference/com/google/android/gms/common/api/PendingResult
 * https://developers.google.com/android/reference/com/google/android/gms/common/api/Status
 */

public class LocationHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

  private Location lastLocation;
  private GoogleApiClient googleApiClient;
  private ShootingActivity shootingActivity;
  private Boolean requestLocationUpdates, isRequestingLocationUpdates;

  private static final String LOG_TAG = "LH";

  public LocationHandler(ShootingActivity shootingActivity) {
    this.shootingActivity = shootingActivity;
    this.requestLocationUpdates = Boolean.FALSE;
    this.isRequestingLocationUpdates = Boolean.FALSE;
  }

  protected void build() {
    Log.d(LOG_TAG, "build");
    if (this.googleApiClient == null) {
      this.googleApiClient = new GoogleApiClient.Builder(this.shootingActivity)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .addApi(LocationServices.API)
          .build();
    }
  }

  /**
   * Calls the connect method on GoogleApiClient
   *
   * @param requestLocationUpdates if location updates should be requested upon successful connection
   */
  protected void connect(Boolean requestLocationUpdates) {
    Log.d(LOG_TAG, "connect");
    this.requestLocationUpdates = requestLocationUpdates;
    this.googleApiClient.connect();
  }

  protected void disconnect() {
    Log.d(LOG_TAG, "disconnect");
    this.googleApiClient.disconnect();
  }

  protected Boolean isRequestingLocationUpdates() {
    return this.isRequestingLocationUpdates;
  }

  /**
   * Attempts to stop location updates if the GoogleApiClient is connected and
   * <code>isRequestingLocationUpdates</code> is <code>Boolean.FALSE</code>. Upon
   * the result of the request, sets <code>isRequestingLocationUpdates</code> to
   * the result status (currently, the only way of knowing the attempt was
   * successful is later checking if this variable is <code>Boolean.TRUE</code>).
   *
   * @return <code>Boolean.TRUE</code> if the GoogleApiClient is connected and this
   * wasn't already requesting location updates. <code>Boolean.FALSE</code> otherwise
   * @throws SecurityException if the user hasn't given the app the location permission
   * @TODO provide a method that receives a ResultCallback
   * @TODO check for location permission just as I do with camera permission
   */
  protected Boolean startLocationUpdates() throws SecurityException {
    Log.d(LOG_TAG, "startLocationUpdates");
    Log.d(LOG_TAG, "startLocationUpdates isConnected: " + Boolean.toString(this.googleApiClient.isConnected()));
    Log.d(LOG_TAG, "startLocationUpdates isRequestingLocationUpdates: " + Boolean.toString(this.isRequestingLocationUpdates));
    if (this.googleApiClient.isConnected() && !this.isRequestingLocationUpdates) {
      PendingResult<Status> pendingStatus = LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), this);
      pendingStatus.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
          LocationHandler.this.isRequestingLocationUpdates = status.isSuccess();
        }
      });
      Log.d(LOG_TAG, "startLocationUpdates returned Boolean.TRUE");
      return Boolean.TRUE;
    }
    Log.d(LOG_TAG, "startLocationUpdates returned Boolean.FALSE");
    return Boolean.FALSE;
  }

  /**
   * Attempts to stop location updates if the GoogleApiClient is connected and
   * <code>isRequestingLocationUpdates</code> is <code>Boolean.TRUE</code>. Upon
   * the result of therequest, sets <code>isRequestingLocationUpdates</code> to the
   * result status (currently, the only way of knowing the attempt was successful
   * is later checking if this variable is <code>Boolean.FALSE</code>).
   */
  protected void stopLocationUpdates() {
    Log.d(LOG_TAG, "stopLocationUpdates");
    if (this.googleApiClient.isConnected() && this.isRequestingLocationUpdates) {
      PendingResult<Status> pendingStatus = LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
      pendingStatus.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
          LocationHandler.this.isRequestingLocationUpdates = !status.isSuccess();
        }
      });
    }
  }

  private void logLocation(Location location) {
    StringBuffer sb = new StringBuffer("Location:");
    sb.append("\nLatitude: ").append(location.getLatitude()).append("\nLongitude: ").append(location.getLongitude())
        .append("\nAltitude: ").append(location.getAltitude()).append("\nAccuracy: ").append(location.getAccuracy())
        .append("\nBearing: ").append(location.getBearing()).append("\nProvider: ").append(location.getProvider())
        .append("\nTime: ").append(location.getTime()).append("\nNumber of satellites: ");
    if (location.getExtras() != null) {
      sb.append(location.getExtras().get("satellites"));
    }
    Log.d(LOG_TAG, sb.toString());
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) throws SecurityException {
    Log.d(LOG_TAG, "onConnected");
    this.lastLocation = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);
    if (this.lastLocation != null) {
      this.logLocation(this.lastLocation);
    }
    if (this.requestLocationUpdates) {
      this.startLocationUpdates();
    }
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.w(LOG_TAG, "onConnectionSuspended: " + Integer.toString(i));
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.e(LOG_TAG, "onConnectionSuspended: " + connectionResult.getErrorMessage());
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d(LOG_TAG, "onLocationChanged");
    this.lastLocation = location;
    this.logLocation(this.lastLocation);
  }
}
