package com.rehammetwally.uberandcareem;

import androidx.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rehammetwally.uberandcareem.databinding.ActivityDriverMapBinding;

import java.util.List;

public class DriverMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_USER_LOCATION_CODE = 12345;
    private GoogleMap mMap;
    private Marker currentUserMarker, customerMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double latitude, longitude;
    private DatabaseReference userReference, driverAvailabilityReference, driverWorkingReference, customerRequestReference;
    private FirebaseUser currentUser;
    private String currentUserId, customerId = "";
    private ActivityDriverMapBinding binding;
    private GeoFire geoFire;
    private static final String TAG = "DriverMapActivity";

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_driver_map);
        setSupportActionBar(binding.toolbar);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser.getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        customerRequestReference = FirebaseDatabase.getInstance().getReference().child("Customer Request");
        userReference.child(currentUserId).child("CUSTOMER_ID").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e(TAG, "onDataChange:CUSTOMER_ID " + dataSnapshot.getValue().toString());
                    customerId = dataSnapshot.getValue().toString();
                    if (customerId != null) {
                        customerRequestReference.child(customerId).child("l").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Log.e(TAG, "customer:exists");
                                    List<Object> list = (List<Object>) dataSnapshot.getValue();
                                    double latitude = (double) list.get(0);
                                    double longitude = (double) list.get(1);


                                    LatLng latLng = new LatLng(latitude, longitude);
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    markerOptions.position(latLng);
                                    markerOptions.title("Customer is here.Pickup location.");
                                    Drawable background = ContextCompat.getDrawable(DriverMapActivity.this, R.drawable.ic_customer);
                                    background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
                                    Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(bitmap);
                                    background.draw(canvas);
                                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                                   if (customerMarker != null){
                                       customerMarker.remove();
                                   }
                                    customerMarker = mMap.addMarker(markerOptions);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                    mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
//                                    binding.contentCustomerMap.callADriver.setText("Driver Found");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } else {
                        customerId = "";
                        if (customerMarker != null) {
                            customerMarker.remove();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        driverAvailabilityReference = FirebaseDatabase.getInstance().getReference().child("Drivers Availability");
        driverWorkingReference = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

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
            Drawable background = ContextCompat.getDrawable(this, R.drawable.ic_driver);
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
            geoFire = new GeoFire(driverAvailabilityReference);
            geoFire.setLocation(currentUserId, new GeoLocation(location.getLatitude(), location.getLongitude())
                    , new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null)
                                Log.e(TAG, "onComplete: " + error.getMessage());
                        }
                    });
            geoFire = new GeoFire(driverWorkingReference);
            geoFire.setLocation(currentUserId, new GeoLocation(location.getLatitude(), location.getLongitude())
                    , new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null)
                                Log.e(TAG, "onComplete: " + error.getMessage());
                        }
                    });

            Log.e(TAG, "updateMapLocation:customerId " + customerId);
            switch (customerId) {
                case "":
                    geoFire = new GeoFire(driverWorkingReference);
                    geoFire.removeLocation(currentUserId, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.e(TAG, "onComplete: " + error.getMessage());
                            }
                        }
                    });
                    break;
                default:
                    getCustomerInformation(customerId);
                    geoFire = new GeoFire(driverAvailabilityReference);
                    geoFire.removeLocation(currentUserId, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.e(TAG, "onComplete: " + error.getMessage());
                            }
                        }
                    });
                    break;
            }
        }
    }

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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsIntent=new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra("IS_CUSTOMER",false);
                startActivity(settingsIntent);
                return true;
            case R.id.logout:
                Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void getCustomerInformation(String id) {
        if (id != null) {
            Log.e(TAG, "getCustomerInformation: " );
            userReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        binding.contentDriverMap.customerLayout.setVisibility(View.VISIBLE);
                        if (dataSnapshot.child("NAME").exists()) {
                            binding.contentDriverMap.customerName.setText("Name: "+dataSnapshot.child("NAME").getValue().toString());
                        }
                        if (dataSnapshot.child("PHONE_NUMBER").exists()) {
                            binding.contentDriverMap.customerPhone.setText("Phone number: "+dataSnapshot.child("PHONE_NUMBER").getValue().toString());
                        }
                        if (dataSnapshot.child("PHOTO").exists()) {
                            Glide.with(DriverMapActivity.this).load(dataSnapshot.child("PHOTO").getValue().toString())
                                    .into(binding.contentDriverMap.customerImage);
                        }
                        binding.contentDriverMap.callCustomerPhone.setOnClickListener(new View.OnClickListener() {
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


}