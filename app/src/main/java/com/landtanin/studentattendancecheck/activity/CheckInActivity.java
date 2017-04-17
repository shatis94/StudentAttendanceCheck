package com.landtanin.studentattendancecheck.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.landtanin.studentattendancecheck.R;
import com.landtanin.studentattendancecheck.dao.StudentModuleDao;
import com.landtanin.studentattendancecheck.databinding.ActivityCheckInBinding;
import com.landtanin.studentattendancecheck.util.TodayModule;

import io.realm.RealmResults;

public class CheckInActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    ActivityCheckInBinding b;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private double douMyLat, moduleLat, douMyLng, moduleLng;
    private LatLngBounds checkInBounds;
    private PopupWindow mPopupWindow;

    private MarkerOptions marker;
    private GoogleMap map;
    private String className = "class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_check_in);

        marker = new MarkerOptions();

        initInstance();

    }

    private void initInstance() {

        setSupportActionBar(b.checkInActivityToolbar);

        TodayModule todayModule = new TodayModule();
        RealmResults<StudentModuleDao> studentModuleDao = todayModule.getTodayModule();

        int targetingModule = getIntent().getExtras().getInt("moduleItem");
        moduleLat = studentModuleDao.get(targetingModule).getLocLat();
        moduleLng = studentModuleDao.get(targetingModule).getLocLng();
        className = studentModuleDao.get(targetingModule).getRoom();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        b.clickToAddModuleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Log.w("MY_LAT", String.valueOf(douMyLat));
                Log.w("MY_LNG", String.valueOf(douMyLng));
                Log.w("MO_LAT", String.valueOf(moduleLat));
                Log.w("MO_LNG", String.valueOf(moduleLng));

                LatLng moduleLocation = new LatLng(moduleLat, moduleLng);
                LatLng studentLocation = new LatLng(douMyLat, douMyLng);
                checkInBounds = toBounds(moduleLocation, 432);
                // 432 is the smallest radius from house to Coates

                Log.w("module Location", String.valueOf(moduleLocation));
                Log.w("student Location", String.valueOf(studentLocation));

//                if (checkInBounds.contains(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))) {
                if (checkInBounds.contains(studentLocation)) {

                    Toast.makeText(CheckInActivity.this, "INBOUND", Toast.LENGTH_SHORT).show();
                    initiatePopupWindow();


                } else {

                    Toast.makeText(CheckInActivity.this, "OUTBOUND", Toast.LENGTH_SHORT).show();

                }

            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void addEvents() {

        marker.title(className);
        marker.position(new LatLng(moduleLat, moduleLng));
        map.addMarker(marker);

    }

    private void initiatePopupWindow() {

        final LayoutInflater inflater = (LayoutInflater) CheckInActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.popup, (ViewGroup) findViewById(R.id.popupElement));
        layout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.zoom_fade_in));

        mPopupWindow = new PopupWindow(layout, 800, 1200, true);

//        mPopupWindow.setAnimationStyle();
        mPopupWindow.setTouchable(true);
        mPopupWindow.setFocusable(true);


//        View parent = inflater.inflate(R.layout.activity_check_in, null);
//        mPopupWindow.setAnimationStyle(R.anim.zoom_fade_in);
        mPopupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CheckInActivity.this, MainActivity.class);
                intent.putExtra("checked", true);
                startActivity(intent);

            }
        });

    }

    // we define this ourself
    public LatLngBounds toBounds(LatLng center, double radius) {

        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);

        Log.w("southwest", String.valueOf(southwest));
        Log.w("northeast", String.valueOf(northeast));

        // return the boundary
        return new LatLngBounds(southwest, northeast);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.w("LOCATION", "CONNECTED");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            if (ContextCompat.checkSelfPermission(CheckInActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(CheckInActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    Log.w("CheckInActivity onMapReady: ", "show an explanation");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(CheckInActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            12345);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }

            Log.w("CheckInActivity onConnected: ", "permission request");
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);


        if (mLastLocation != null) {

            douMyLat = mLastLocation.getLatitude();
            douMyLng = mLastLocation.getLongitude();

        }

        Log.d("MyLatLng", String.valueOf(douMyLat) + " " + douMyLng);
        updateLocation();
        addEvents();
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.w("LOCATION", "SUSPENDED");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.w("LOCATION", "FAILED");

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding

            if (ContextCompat.checkSelfPermission(CheckInActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(CheckInActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    Log.w("CheckInActivity onMapReady: ", "show an explanation");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(CheckInActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            12345);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }

//               public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                                      int[] grantResults)

            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.w("CheckInActivity onMapReady: ", "need permission");
            return;
        }

        Log.w("CheckInActivity onMapReady: ", "updateLocation in onmapready");
        updateLocation();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 12345: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(this, "permission is granted", Toast.LENGTH_SHORT).show();
                    updateLocation();
                    addEvents();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission is denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }


    private void updateLocation() {

        if (mGoogleApiClient.isConnected()) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (ContextCompat.checkSelfPermission(CheckInActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(CheckInActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {

                        Log.w("CheckInActivity onMapReady: ", "show an explanation");
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(CheckInActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                12345);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }

                return;
            }

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (map != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 17));
                Log.w("CheckInActivity updateLocation: ", "updateLocation");
            }

        }

        map.setMyLocationEnabled(true);

    }
}
