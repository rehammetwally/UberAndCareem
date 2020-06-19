package com.rehammetwally.uberandcareem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rehammetwally.uberandcareem.databinding.ActivityCustomerBinding;

import java.util.HashMap;

public class CustomerActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityCustomerBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private DatabaseReference userReference;
    private static final String TAG = "CustomerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_customer);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser != null)
            currentUserId = currentUser.getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        binding.customerDoNotHaveAccount.setOnClickListener(this);
        binding.customerLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.customer_do_not_have_account:
                binding.customerTitle.setText(getResources().getString(R.string.customer_register));
                binding.customerLogin.setText(getResources().getString(R.string.register));
                break;
            case R.id.customer_login:
                String email = binding.customerEmail.getText().toString();
                String password = binding.customerPassword.getText().toString();
                if (email.isEmpty()) {
                    Toast.makeText(this, getResources().getString(R.string.email) + " should'nt be empty.Please , write " + getResources().getString(R.string.email) + ".", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.isEmpty()) {
                    Toast.makeText(this, getResources().getString(R.string.password) + " should'nt be empty.Please , write " + getResources().getString(R.string.password) + ".", Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress(true);
                if (binding.customerLogin.getText().toString().equals(getResources().getString(R.string.register))) {
                    registerWithEmailAndPassword(email, password);
                } else if (binding.customerLogin.getText().toString().equals(getResources().getString(R.string.login))) {
                    loginWithEmailAndPassword(email, password);
                }
                break;
        }
    }

    private void loginWithEmailAndPassword(String email, String password) {

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                           // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "signInWithEmail:success");
                            final FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                if (user.getUid() != null) {
                                    userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.child(user.getUid()).exists()) {
                                                Boolean isCustomer = (Boolean) dataSnapshot.child(user.getUid()).child("IS_CUSTOMER").getValue();
                                                Boolean isDriver = (Boolean) dataSnapshot.child(user.getUid()).child("IS_DRIVER").getValue();
                                                if (isCustomer && !isDriver) {
                                                    showProgress(false);
                                                    Toast.makeText(CustomerActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(CustomerActivity.this, CustomerMapActivity.class));
                                                } else if (!isCustomer && isDriver) {
                                                    showProgress(false);
                                                    Toast.makeText(CustomerActivity.this, "You are not a customer.you are a driver.go to driver login please. ", Toast.LENGTH_LONG).show();
                                                }
                                                Log.e(TAG, "onDataChange:isCustomer " + isCustomer);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        } else {
                            showProgress(false);
                            // If sign in fails, display a message to the user.
                            Log.e(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(CustomerActivity.this, "Authentication failed. " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void registerWithEmailAndPassword(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            showProgress(false);
                            Toast.makeText(CustomerActivity.this, "Register successfully.", Toast.LENGTH_SHORT).show();
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "createUserWithEmail:success");
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                binding.customerTitle.setText(getResources().getString(R.string.customer_login));
                                binding.customerLogin.setText(getResources().getString(R.string.login));
                                HashMap<String, Object> registerHashMap = new HashMap<>();
                                registerHashMap.put("NAME", user.getDisplayName());
                                registerHashMap.put("EMAIL", user.getEmail());
                                registerHashMap.put("IS_DRIVER", false);
                                registerHashMap.put("IS_CUSTOMER", true);
                                userReference.child(user.getUid()).updateChildren(registerHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.e(TAG, "onComplete:Success ");
                                        } else {
                                            Log.e(TAG, "onComplete:Failed " + task.getException().getMessage());
                                        }
                                    }
                                });
//                                startActivity(new Intent(CustomerActivity.this,MainActivity.class));
                            }
                        } else {
                            showProgress(false);
                            // If sign in fails, display a message to the user.
                            Log.e(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CustomerActivity.this, "Authentication failed. " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
//                            updateUI(null);
                        }
                    }
                });
    }

    private void showProgress(boolean show) {
        if (show) {
            binding.progress.setVisibility(View.VISIBLE);
            binding.customerLogin.setEnabled(false);
            binding.customerEmail.setEnabled(false);
            binding.customerPassword.setEnabled(false);
            binding.customerDoNotHaveAccount.setEnabled(false);
        } else {
            binding.progress.setVisibility(View.GONE);
            binding.customerLogin.setEnabled(true);
            binding.customerEmail.setEnabled(true);
            binding.customerPassword.setEnabled(true);
            binding.customerDoNotHaveAccount.setEnabled(true);
        }
    }
}