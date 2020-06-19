package com.rehammetwally.uberandcareem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rehammetwally.uberandcareem.databinding.ActivityCustomerMapBinding;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserId, driverId;
    private Boolean isDriverFound = false, isRequest = false;
    private GoogleMap mMap;
    private GeoFire geoFire;
    private Marker currentUserMarker, driverMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double latitude, longitude;
    private int radius = 1;
    private DatabaseReference userReference, customerRequestReference, driverAvailabilityReference, driverWorkingReference;
    private ActivityCustomerMapBinding binding;
    private static final int REQUEST_USER_LOCATION_CODE = 12345;
    private static final String TAG = "CustomerMapActivity";

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            initLocationTracking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_customer_map);
        setSupportActionBar(binding.toolbar);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        currentUserId = currentUser.getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        customerRequestReference = FirebaseDatabase.getInstance().getReference().child("Customer Request");
        driverWorkingReference = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        driverAvailabilityReference = FirebaseDatabase.getInstance().getReference().child("Drivers Availability");
        geoFire = new GeoFire(customerRequestReference);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        binding.contentCustomerMap.callADriver.setOnClickListener(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    private void initLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else if (!statusCheck()) {
            buildAlertMessageNoGps();
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    updateMapLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                new LocationRequest(),
                locationCallback,
                null);
    }

    private void initMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else if (!statusCheck()) {
            buildAlertMessageNoGps();
        }
        mMap.setMyLocationEnabled(true);
        initLocationTracking();

    }

    private void updateMapLocation(Location location) {
        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("You are here");
            Drawable background = ContextCompat.getDrawable(this, R.drawable.ic_customer);
            background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
            Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            background.draw(canvas);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
            if (currentUserMarker != null) {
                currentUserMarker.remove();
            }
            currentUserMarker = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(18));


        }
    }

//    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
//
//        return BitmapDescriptorFactory.fromBitmap(bitmap);
//    }

    public boolean statusCheck() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_USER_LOCATION_CODE);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            initMap();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_USER_LOCATION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_USER_LOCATION_CODE:
                if (permissions.length == 1 &&
                        permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    } else if (!statusCheck()) {
                        buildAlertMessageNoGps();
                    }
                    mMap.setMyLocationEnabled(true);

                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maps_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra("IS_CUSTOMER", true);
                startActivity(settingsIntent);
                break;
            case R.id.logout:
                Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call_a_driver:
                Log.e(TAG, "onClick: " + isRequest);
                if (isRequest) {
                    isRequest = false;
                    isDriverFound = false;
                    radius = 1;
                    geoFire = new GeoFire(customerRequestReference);
                    geoFire.removeLocation(currentUserId, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.e(TAG, "onComplete:removeLocation " + error.getMessage());
                            }
                        }
                    });

                    userReference.child(driverId).child("CUSTOMER_ID")
                            .removeValue(new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Log.e(TAG, "onComplete: " + databaseError.getMessage());
                                    }
                                }
                            });
                    if (driverMarker != null) {
                        driverMarker.remove();
                    }
                    binding.contentCustomerMap.callADriver.setText(getResources().getString(R.string.call_a_driver));
                    binding.contentCustomerMap.driverLayout.setVisibility(View.GONE);
                } else {
                    isRequest = true;
//                    isDriverFound = true;
                    Log.e(TAG, "onClick: " + latitude);
                    Log.e(TAG, "onClick: " + longitude);
                    geoFire = new GeoFire(customerRequestReference);
                    geoFire.setLocation(currentUserId, new GeoLocation(latitude, longitude)
                            , new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null)
                                        Log.e(TAG, "onComplete: " + error.getMessage());
                                }
                            });
                    binding.contentCustomerMap.callADriver.setText("Getting driver...");
                    getNearestDrivers();
                }
                break;
        }
    }

    private void getNearestDrivers() {
        Log.e(TAG, "getNearestDrivers: ");
        Log.e(TAG, "getNearestDrivers: " + isRequest);
        Log.e(TAG, "getNearestDrivers: " + isDriverFound);
        geoFire = new GeoFire(driverWorkingReference);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latitude, longitude), radius);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound) {
                    isDriverFound = true;
                    driverId = key;
                    getDriverLocation();
                    binding.contentCustomerMap.callADriver.setText("Looking for driver location...");

                    getDriverInformation(driverId);
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("CUSTOMER_ID", currentUserId);
                    userReference.child(driverId).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.e(TAG, "onComplete ");
                            }
                        }
                    });


                    Log.e(TAG, "onKeyEntered:DriverKey " + key);
                    Log.e(TAG, "onKeyEntered:DriverLatitude " + location.latitude);
                    Log.e(TAG, "onKeyEntered:DriverLongitude " + location.longitude);
                }
//                    binding.contentCustomerMap.callADriver.setText("Driver Found");
//

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound && isRequest) {
                    radius = radius + 1;
                    getNearestDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "onGeoQueryError: " + error);
            }
        });
    }

    private void getDriverInformation(String id) {
        if (id != null) {
            userReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        binding.contentCustomerMap.driverLayout.setVisibility(View.VISIBLE);
                        if (dataSnapshot.child("NAME").exists()) {
                            binding.contentCustomerMap.driverName.setText("Name: "+dataSnapshot.child("NAME").getValue().toString());
                        }
                        if (dataSnapshot.child("PHONE_NUMBER").exists()) {
                            binding.contentCustomerMap.driverPhone.setText("Phone number: "+dataSnapshot.child("PHONE_NUMBER").getValue().toString());
                        }
                        if (dataSnapshot.child("CAR_NUMBER").exists()) {
                            binding.contentCustomerMap.driverCarNumber.setText("Car number: "+dataSnapshot.child("CAR_NUMBER").getValue().toString());
                        }
                        if (dataSnapshot.child("PHOTO").exists()) {
                            Glide.with(CustomerMapActivity.this).load(dataSnapshot.child("PHOTO").getValue().toString())
                                    .into(binding.contentCustomerMap.driverImage);
                        }
                        binding.contentCustomerMap.callDriverPhone.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String phone = dataSnapshot.child("PHONE_NUMBER").getValue().toString();


                                if (!TextUtils.isEmpty(phone)) {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:"+ phone));
                                    startActivity(intent);
                                } else {
                                    Log.e(TAG, "empty " );
//                                    Toast.makeText(CustomerMapActivity.this, "Enter a phone number", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    }

    private void getDriverLocation() {
        Log.e(TAG, "getDriverLocation: " + isRequest);
        driverWorkingReference.child(driverId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && isRequest) {
                    Log.e(TAG, "onDataChange:exists");
                    List<Object> list = (List<Object>) dataSnapshot.getValue();
                    double driverLatitude = (double) list.get(0);
                    double driverLongitude = (double) list.get(1);

                    LatLng latLng = new LatLng(driverLatitude, driverLongitude);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Driver is here");
                    Drawable background = ContextCompat.getDrawable(CustomerMapActivity.this, R.drawable.ic_driver);
                    background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
                    Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    background.draw(canvas);
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    driverMarker = mMap.addMarker(markerOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(driverLatitude);
                    driverLocation.setLongitude(driverLongitude);
                    Location customerLocation = new Location("");
                    customerLocation.setLatitude(latitude);
                    customerLocation.setLongitude(longitude);

                    float distance = customerLocation.distanceTo(driverLocation);
                    Log.e(TAG, "onDataChange: " + distance);
                    if (distance < 50) {
                        binding.contentCustomerMap.callADriver.setText("Driver is reached");
                    } else {
                        binding.contentCustomerMap.callADriver.setText("Driver Found. Distance is (" + distance + ")....");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}