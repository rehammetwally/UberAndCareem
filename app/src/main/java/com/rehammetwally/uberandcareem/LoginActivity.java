package com.rehammetwally.uberandcareem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rehammetwally.uberandcareem.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private DatabaseReference userReference;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser != null)
            currentUserId = currentUser.getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        binding.driver.setOnClickListener(this);
        binding.customer.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.driver:
                if (currentUser != null) {
                    Log.e(TAG, "onStart:UID " + currentUser.getUid());
                    userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child(currentUser.getUid()).exists()) {
                                Boolean isDriver = (Boolean) dataSnapshot.child(currentUser.getUid()).child("IS_DRIVER").getValue();
                                Log.e(TAG, "onDataChange: " + dataSnapshot.child(currentUser.getUid()).child("IS_DRIVER").getValue().toString());
                                if (isDriver) {
                                    startActivity(new Intent(LoginActivity.this, DriverMapActivity.class));
                                } else {
                                    Intent driverIntent = new Intent(LoginActivity.this, DriverActivity.class);
                                    startActivity(driverIntent);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }else {
                    Intent driverIntent = new Intent(LoginActivity.this, DriverActivity.class);
                    startActivity(driverIntent);
                }
                break;
            case R.id.customer:
                if (currentUser != null) {
                    userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child(currentUser.getUid()).exists()) {
                                Boolean isCustomer = (Boolean) dataSnapshot.child(currentUser.getUid()).child("IS_CUSTOMER").getValue();
                                Log.e(TAG, "onDataChange: " + dataSnapshot.child(currentUser.getUid()).child("IS_CUSTOMER").getValue().toString());
                                if (isCustomer) {
                                    startActivity(new Intent(LoginActivity.this,CustomerMapActivity.class));
                                } else {
                                    Intent customerIntent = new Intent(LoginActivity.this, CustomerActivity.class);
                                    startActivity(customerIntent);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }else {
                    Intent customerIntent = new Intent(this, CustomerActivity.class);
                    startActivity(customerIntent);
                }

                break;
        }
    }
}