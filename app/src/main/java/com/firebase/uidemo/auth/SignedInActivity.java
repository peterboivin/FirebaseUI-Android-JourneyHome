/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.uidemo.auth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignedInActivity extends FragmentActivity implements LocationListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerDragListener,
        SeekBar.OnSeekBarChangeListener,
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnInfoWindowCloseListener {

    @BindView(android.R.id.content)
    View mRootView;

    @BindView(R.id.user_profile_picture)
    ImageView mUserProfilePicture;

    @BindView(R.id.user_email)
    TextView mUserEmail;

    @BindView(R.id.user_display_name)
    TextView mUserDisplayName;

    private FirebaseUser currentUser;

    private static final String TAG = "SignInActivity";

    private GoogleMap googleMap;
    //    static final LatLng startingPoint = new LatLng(26.335299, -81.428290);
    static final LatLng startingPoint = new LatLng(26.335299, -81.428290);

    protected LocationManager locationManager;

    private Marker marker;

    private static final float ZOOM_LEVEL = 12;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mUserRef;
    private DatabaseReference mGpsRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int LOCATION_REFRESH_TIME = 1000;
        int LOCATION_REFRESH_DISTANCE = 15;

        Log.d(TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(AuthUiActivity.createIntent(this));
            finish();
            return;
        }

        setContentView(R.layout.signed_in_layout);
        ButterKnife.bind(this);
        populateProfile();

        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);

            if (googleMap == null) {
                googleMap = ((SupportMapFragment) getSupportFragmentManager().
                        findFragmentById(com.example.mygooglemapslib.R.id.map)).getMap();
            }
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            // Showing the current location in Google Map
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
            // Zoom in the Google Map
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL));

            marker = googleMap.addMarker(new MarkerOptions().
                    position(startingPoint).title("Starting Position"));

            // Set a listener for info window events.
            googleMap.setOnInfoWindowClickListener(this);
            googleMap.setOnMarkerClickListener(this);
            googleMap.setOnInfoWindowClickListener(this);
            googleMap.setOnMarkerDragListener(this);
            googleMap.setOnInfoWindowCloseListener(this);
            googleMap.setOnInfoWindowLongClickListener(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.sign_out)
    public void signOut() {
        Log.d(TAG, "signOut ");
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(     new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(AuthUiActivity.createIntent(SignedInActivity.this));
                            finish();
                        } else {
                            showSnackbar(R.string.sign_out_failed);
                        }
                    }
                });
    }

    @MainThread
    private void populateProfile() {
        mUserRef = mRootRef.child(currentUser.getUid());
        mUserRef.setValue(currentUser);
        Log.d(TAG, "populateProfile ");

        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .fitCenter()
                    .into(mUserProfilePicture);
        }

        mUserEmail.setText(
                TextUtils.isEmpty(currentUser.getEmail()) ? "No email" : currentUser.getEmail());
        mUserDisplayName.setText(
                TextUtils.isEmpty(currentUser.getDisplayName()) ? "No display name" : currentUser.getDisplayName());

        StringBuilder providerList = new StringBuilder();

        providerList.append("Providers used: ");

        if (currentUser.getProviders() == null || currentUser.getProviders().isEmpty()) {
            providerList.append("none");
        } else {
            Iterator<String> providerIter = currentUser.getProviders().iterator();
            while (providerIter.hasNext()) {
                String provider = providerIter.next();
                if (GoogleAuthProvider.PROVIDER_ID.equals(provider)) {
                    providerList.append("Google");
                } else if (FacebookAuthProvider.PROVIDER_ID.equals(provider)) {
                    providerList.append("Facebook");
                } else if (EmailAuthProvider.PROVIDER_ID.equals(provider)) {
                    providerList.append("Password");
                } else {
                    providerList.append(provider);
                }

                if (providerIter.hasNext()) {
                    providerList.append(", ");
                }
            }
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG)
                .show();
    }

    public static Intent createIntent(Context context) {
        Intent in = new Intent();
        in.setClass(context, SignedInActivity.class);
        return in;
    }

    @Override
    public void onLocationChanged(Location location) {
        mGpsRef = mUserRef.child("gps");
        marker.remove();
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().
                    findFragmentById(com.example.mygooglemapslib.R.id.map)).getMap();
        }
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Showing the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL));

        marker = googleMap.addMarker(new MarkerOptions().
                position(position));

        mGpsRef.push().setValue(position);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, "Info window clicked",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Set a listener for info window events.
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setOnMarkerDragListener(this);
        googleMap.setOnInfoWindowCloseListener(this);
        googleMap.setOnInfoWindowLongClickListener(this);
    }

    @Override
    public void onInfoWindowClose(Marker marker) {

    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(this, "Info window clicked",
                Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
