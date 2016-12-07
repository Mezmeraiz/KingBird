package com.mezmeraiz.kingbird;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, LocationListener, View.OnClickListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Marker mCurrentLocationMarker;
    private LocationRequest mLocationRequest;
    private TextView mDistanceTextView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final double mTargetLatitude = 55.778914190059794;
    private final double mTargetLongitude = 37.59541869163513;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initViews();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();
    }

    private void initViews(){
        mDistanceTextView = (TextView) findViewById(R.id.textView_distance);
        findViewById(R.id.fab_near).setOnClickListener(this);
        findViewById(R.id.fab_google).setOnClickListener(this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(mTargetLatitude, mTargetLongitude))
                .icon(getBitmapDescriptor(R.drawable.pin)));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            requestLocationUpdates();
        }else{
            Toast.makeText(this, "Нет разрешения", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && mMap!= null) {
            double currentLat = location.getLatitude();
            double currentLng = location.getLongitude();
            LatLng currentPosition = new LatLng(currentLat, currentLng);
            if(mCurrentLocationMarker != null)
                mCurrentLocationMarker.remove();
            mCurrentLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentPosition).icon(getBitmapDescriptor(R.drawable.info)));
            int distance = (int) (Math.acos(Math.sin(Math.toRadians(mTargetLatitude)) * Math.sin(Math.toRadians(currentLat))
                                + Math.cos(Math.toRadians(mTargetLatitude)) * Math.cos(Math.toRadians(currentLat))
                    * Math.cos(Math.toRadians(mTargetLongitude - currentLng))) * 6373d);
            mDistanceTextView.setText(String.format("%1d км", distance));
        }
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(this, id);
        int h = drawable.getIntrinsicHeight();
        int w = drawable.getIntrinsicWidth();
        drawable.setBounds(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bm);
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab_google:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("google.navigation:q=" + mTargetLatitude + "," + mTargetLongitude)));
                break;
            case R.id.fab_near:
                if(mCurrentLocationMarker != null){
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLocationMarker.getPosition())
                            .zoom(mMap.getCameraPosition().zoom > 12 ? mMap.getCameraPosition().zoom : 12)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
                break;
        }
    }
}
