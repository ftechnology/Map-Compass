package com.gapp.android.tools.compass;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gapp.android.tools.compass.Util.Utils;
import com.gapp.android.tools.compass.errorhandle.ErrorHandler;
import com.gapp.android.tools.compass.sensor.OrientaionSensorDetector;
import com.gapp.android.tools.compass.view.CustomCompassView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;

import com.sftech.tools.compassmap.R;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OrientaionSensorDetector.OrientationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    //Compass
    CustomCompassView compassView;
    private OrientaionSensorDetector orientationSensor;
    private int displayOrientaion;
    RelativeLayout rl_search;
    ImageView iv_search;
    EditText ed_search;

    Button bt_compassVisivility, bt_mapVisivility;
    TextView tv_title;
    LinearLayout ln_map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        initUI();

        ed_search.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Utils.hideSoftKeyboard(MapsActivity.this);
                    onMapSearch(ed_search.getText().toString());

                    return true;
                }
                return false;
            }
        });

        ed_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE) || actionId == EditorInfo.IME_ACTION_SEND) {
                    Utils.hideSoftKeyboard(MapsActivity.this);
                    onMapSearch(ed_search.getText().toString());
                    return true;
                }
                return false;
            }
        });

        //Compass
        setupCompass();
    }

    private void setupCompass() {
        compassView.setTransparentMode(true);
        orientationSensor = new OrientaionSensorDetector(this);
        sensorEnable();
    }


    private void initUI() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        iv_search = (ImageView) findViewById(R.id.iv_search);
        rl_search = (RelativeLayout) findViewById(R.id.rl_search);
        ed_search = (EditText) findViewById(R.id.ed_address);
        bt_compassVisivility = (Button) findViewById(R.id.bt_compass);
        bt_mapVisivility = (Button) findViewById(R.id.bt_map);
        tv_title = (TextView) findViewById(R.id.tv_title);
        ln_map = (LinearLayout) findViewById(R.id.ln_map);
        compassView = (CustomCompassView) findViewById(R.id.compassView);
    }

    public void onClickBack(View v) {
        if (rl_search.getVisibility() == View.VISIBLE) {
            Utils.hideSoftKeyboard(MapsActivity.this);
            rl_search.setVisibility(View.GONE);
            tv_title.setVisibility(View.VISIBLE);
        } else {
            onBackPressed();
        }
    }

    public void clickCompassVisibility(View v) {
        if (compassView.getVisibility() == View.GONE) {
            compassView.setVisibility(View.VISIBLE);
            bt_compassVisivility.setText("Hide Compass");
            bt_mapVisivility.setEnabled(true);
        } else {
            compassView.setVisibility(View.GONE);
            bt_compassVisivility.setText("Show Compass");
            bt_mapVisivility.setEnabled(false);
        }
    }

    public void clickMapVisibility(View v) {
        if (ln_map.getVisibility() == View.GONE) {
            ln_map.setVisibility(View.VISIBLE);
            bt_mapVisivility.setText("Hide Map");
            bt_compassVisivility.setEnabled(true);
            compassView.setTransparentMode(true);
            compassView.invalidate();
        } else {
            ln_map.setVisibility(View.GONE);
            bt_mapVisivility.setText("Show Map");
            bt_compassVisivility.setEnabled(false);
            compassView.setTransparentMode(false);
            compassView.invalidate();
        }
    }

    public void clickSearch(View v) {
        if (rl_search.getVisibility() == View.VISIBLE) {
            onMapSearch(ed_search.getText().toString());
            Utils.hideSoftKeyboard(this);
        } else {
            rl_search.setVisibility(View.VISIBLE);
            tv_title.setVisibility(View.GONE);
        }
    }

    public void onMapSearch(String location) {
        List<Address> addressList = null;

        if (location != null && !location.isEmpty()) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
                if (addressList.size() > 0) {
                    mMap.clear();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addressList.size() == 0) {
                return;
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    //Compass
    private void setOrientation() {
        if (displayOrientaion < 0)
            displayOrientaion = 180 + (180 + displayOrientaion);

    }

    //Compass
    private void sensorEnable() {
        orientationSensor.setListener(this);
    }

    //Compass
    @Override
    public void onOrientaion(double o, double p, double r) {
        displayOrientaion = (int) o;
        setOrientation();
        compassView.updatePosition(o, p, r);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Compass
        ErrorHandler.open_sensor(orientationSensor, this);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    @Override
    protected void onDestroy() {

        //Compass
        if (compassView != null) {
            compassView.destroy();
        }
        if (orientationSensor != null) {
            orientationSensor.destory();
        }

        super.onDestroy();
    }
}