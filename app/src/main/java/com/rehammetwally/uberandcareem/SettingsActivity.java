package com.rehammetwally.uberandcareem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.rehammetwally.uberandcareem.databinding.ActivitySettingsBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE = 1234;
    private Boolean isCustomer;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private DatabaseReference userReference;
    private FirebaseStorage imageStorage;
    private StorageReference imageStorageReference;
    private Uri selectedImage;
    private ActivitySettingsBinding binding;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        currentUserId = currentUser.getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        imageStorage = FirebaseStorage.getInstance();
        imageStorageReference = imageStorage.getReference();
        setSupportActionBar(binding.toolbar);
        if (getIntent() != null) {
            isCustomer = getIntent().getExtras().getBoolean("IS_CUSTOMER");
            Log.e(TAG, "onCreate:isCustomer " + isCustomer);
            if (isCustomer) {
                binding.toolbar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                binding.changeImage.setTextColor(getResources().getColor(R.color.colorAccent));
                binding.carNumber.setVisibility(View.GONE);
            } else {
                binding.toolbar.setBackgroundColor(getResources().getColor(R.color.colorPink));
                binding.changeImage.setTextColor(getResources().getColor(R.color.colorPink));
                binding.carNumber.setVisibility(View.VISIBLE);
            }
            binding.save.setOnClickListener(this);
            binding.close.setOnClickListener(this);
            binding.changeImage.setOnClickListener(this);
        }

        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(currentUserId).exists()) {
                    if (dataSnapshot.child(currentUserId).child("PHOTO").exists()) {
                        String photo = dataSnapshot.child(currentUserId).child("PHOTO").getValue().toString();
                        if (photo != null) {
                            Log.e(TAG, "onDataChange: " + photo);
                            binding.imageProgress.setVisibility(View.GONE);
                            Glide.with(SettingsActivity.this).load(photo)
                                    .into(binding.profileImage);
                        }
                    }
                    if (dataSnapshot.child(currentUserId).child("NAME").exists())
                        binding.name.setText(dataSnapshot.child(currentUserId).child("NAME").getValue().toString());
                    if (dataSnapshot.child(currentUserId).child("PHONE_NUMBER").exists())
                        binding.phone.setText(dataSnapshot.child(currentUserId).child("PHONE_NUMBER").getValue().toString());
                    if (!isCustomer)
                        if (dataSnapshot.child(currentUserId).child("CAR_NUMBER").exists())
                            binding.carNumber.setText(dataSnapshot.child(currentUserId).child("CAR_NUMBER").getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.change_image:
                changeImage();
                break;
            case R.id.close:
                finish();
                break;
            case R.id.save:
                String name = binding.name.getText().toString();
                String phoneNumber = binding.phone.getText().toString();
                String carNumber = binding.carNumber.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please,fill name field", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "Please,fill phone number field", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isCustomer) {
                    if (carNumber.isEmpty()) {
                        Toast.makeText(this, "Please,fill car number field", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                saveData(name, phoneNumber, carNumber);
                break;
        }
    }

    private void saveData(final String name, final String phoneNumber, final String carNumber) {

        imageStorageReference.child(currentUserId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("NAME", name);
                hashMap.put("PHONE_NUMBER", phoneNumber);
                if (uri != null) {
                    Log.e(TAG, "onSuccess: " + uri.toString());
                    hashMap.put("PHOTO", uri.toString());
                }
                if (!isCustomer)
                    hashMap.put("CAR_NUMBER", carNumber);
                userReference.child(currentUserId).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e(TAG, "onComplete ");
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.e(TAG, "onFailure: " + exception.getMessage());
            }
        });
    }

    private void changeImage() {
        binding.imageProgress.setVisibility(View.VISIBLE);
        if (isCustomer) {
            binding.imageProgress.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            binding.imageProgress.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPink), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        openGallery();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GO TO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && data != null) {
            selectedImage = data.getData();
            if (selectedImage != null) {
                Log.e(TAG, "onActivityResult: " + selectedImage.toString());

                UploadTask uploadTask = imageStorageReference.child(currentUserId).putFile(selectedImage);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "onFailure: " + exception.getMessage());
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        binding.imageProgress.setVisibility(View.GONE);
                        Log.e(TAG, "onSuccess: ");
                        Glide.with(SettingsActivity.this)
                                .setDefaultRequestOptions(new RequestOptions())
                                .load(selectedImage)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(binding.profileImage);
                    }
                });
            }

        }
    }
}