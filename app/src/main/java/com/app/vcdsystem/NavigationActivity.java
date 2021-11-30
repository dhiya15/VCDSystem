package com.app.vcdsystem;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.studioidan.httpagent.HttpAgent;
import com.studioidan.httpagent.JsonArrayCallback;
import com.studioidan.httpagent.JsonCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import models.Bus;
import models.Chauffeur;
import models.Receveur;
import models.VoyageTrack;
import tools.Tools;

public class NavigationActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    Marker userLocationMarker;
    Circle userLocationAccuracyCircle;

    LocationManager loc_mgr;
    TextView speedValue, speedMaxValue, distanceText, temp, infos;

    final int update_interval = 500; // in milliseconds
    float speed = 0.0f;
    Location location;
    double distance = 0;
    ArrayList<Float> speeds;
    float temperature = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        checkPermessions();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        speedValue = findViewById(R.id.speedText);
        speedMaxValue = findViewById(R.id.speedMaxText);
        distanceText = findViewById(R.id.distanceText);
        temp = findViewById(R.id.tempInt);
        infos = findViewById(R.id.infos);

        speeds = new ArrayList<>();
        update_speed(0.0f);

        loc_mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        loc_mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, update_interval, 0.0f, this);
        location = loc_mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        infos.setText(
                "Bus : " + Bus.matricule + "\n" +
                "Chauffeur : " + Chauffeur.nomComplet + "\n" +
                "Receveur : " + Receveur.nomComplet + "\n" +
                "Départ : " + VoyageTrack.depart + ", Arrivé : " + VoyageTrack.arrive + "\n" +
                "Voyage de : " + VoyageTrack.jour + " à " +VoyageTrack.heur

        );


    }

    public void checkPermessions() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (mMap != null) {
                setUserLocationMarker(locationResult.getLastLocation());
            }
        }
    };

    private void setUserLocationMarker(Location location) {


            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());


            if (userLocationMarker == null) {
                //Create a new marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car2));
                markerOptions.rotation(location.getBearing());
                markerOptions.anchor((float) 0.5, (float) 0.5);
                userLocationMarker = mMap.addMarker(markerOptions);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                //use the previously created marker
                userLocationMarker.setPosition(latLng);
                userLocationMarker.setRotation(location.getBearing());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            if (userLocationAccuracyCircle == null) {
                CircleOptions circleOptions = new CircleOptions();
                circleOptions.center(latLng);
                circleOptions.strokeWidth(4);
                circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
                circleOptions.fillColor(Color.argb(32, 255, 0, 0));
                circleOptions.radius(location.getAccuracy());
                userLocationAccuracyCircle = mMap.addCircle(circleOptions);
            } else {
                userLocationAccuracyCircle.setCenter(latLng);
                userLocationAccuracyCircle.setRadius(location.getAccuracy());
            }


    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            // you need to request permissions...
        }
    }

    public boolean checkPermission(String permission) {
        int check = ContextCompat.checkSelfPermission(NavigationActivity.this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());


                if (userLocationMarker == null) {
                    //Create a new marker
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car2));
                    markerOptions.rotation(location.getBearing());
                    markerOptions.anchor((float) 0.5, (float) 0.5);
                    userLocationMarker = mMap.addMarker(markerOptions);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                } else {
                    //use the previously created marker
                    userLocationMarker.setPosition(latLng);
                    userLocationMarker.setRotation(location.getBearing());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }

                if (userLocationAccuracyCircle == null) {
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(latLng);
                    circleOptions.strokeWidth(4);
                    circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
                    circleOptions.fillColor(Color.argb(32, 255, 0, 0));
                    circleOptions.radius(location.getAccuracy());
                    userLocationAccuracyCircle = mMap.addCircle(circleOptions);
                } else {
                    userLocationAccuracyCircle.setCenter(latLng);
                    userLocationAccuracyCircle.setRadius(location.getAccuracy());
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
       // Tools.hideSystemUI(NavigationActivity.this);
    }

    @Override
    public void onLocationChanged(@NonNull Location loc) {
        if (loc == null) {
            speedValue.setText("No GPS here.");
            return;
        }
        if (! loc.hasSpeed()) {
            return;
        }
        update_speed(loc.getSpeed());
        if(location != null){
            distance += Tools.distance(location.getLatitude(), location.getLongitude(),
                    loc.getLatitude(), loc.getLongitude());
            String dist = String.format("%.0f", distance);
            distanceText.setText(dist);



        }

        sendLastLocation(loc.getLatitude(), loc.getLongitude());

        location = loc;

        updateWeather(loc);

    }

    private void sendLastLocation(double latitude, double longitude) {
        HttpAgent.get("https://t-gestion.herokuapp.com/t-gestion/api/v1/track/ajouter?voyage="+VoyageTrack.code+"&latitude="+latitude+"&longitude="+longitude)
                .goJsonArray(new JsonArrayCallback() {
            @Override
            protected void onDone(boolean success, JSONArray jsonArray) {
System.out.println("Location updated !---------------------------------");
            }
        });
    }

    void update_speed(float x) {
        speed = x;

        speeds.add(x * 3.6f);

        String s = String.format("%.0f", speed * 3.6f);
        String s_avg = String.format("%.0f", speed_avg());
        speedValue.setText(s);
        speedMaxValue.setText(s_avg);
    }

    public float speed_avg(){
        float speed_avg = 0.0f;
        for(Float s : speeds){
            speed_avg += s;
        }
        speed_avg = speed_avg / speeds.size();
        return speed_avg;
    }

    public void updateWeather(Location loc){
        HttpAgent.get("https://api.openweathermap.org/data/2.5/weather?lat="+loc.getLatitude()+"&lon="+loc.getLongitude()+"&appid=8720af42814a166fa77425487330e005")
            .goJson(new JsonCallback() {
                    @Override
                    protected void onDone(boolean success, JSONObject jsonObject) {
                if(success){
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("weather");
                        String weatherState = jsonArray.getJSONObject(0).getString("main");
                        JSONObject jsonObject1 = jsonObject.getJSONObject("main");
                        temperature = ((float) jsonObject1.getDouble("temp") - 273.15f);
                        temp.setText(String.format("%.0f", temperature) + " °C");
                       // weather.setText(weatherState);
                    } catch (JSONException e) {
                                e.printStackTrace();
                    }
                }
            }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }



    public void alert(){
/*
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(gsm, null,
                msg, null, null);
        Toast.makeText(getApplicationContext(),
                "Demmande Envoyée!", Toast.LENGTH_LONG).show();*/

    }
}