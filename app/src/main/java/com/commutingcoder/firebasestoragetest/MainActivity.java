package com.commutingcoder.firebasestoragetest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

// TODO: authorization in Firebase consol
// TODO: define strategy for audio file storage: 2 remote files that are updated (look to metadata)
//  and download/store locally each file?
// TODO: add code for download handling: waiting, progress, etc...

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    private Button mRecordButton;
    private Button mPlayMyRecordButton;
    private Button mSendButton;
    private Button mPlayOtherRecordButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordButton = (Button) findViewById(R.id.record_button);
        mPlayMyRecordButton = (Button) findViewById(R.id.play_my_record_button);
        mSendButton = (Button) findViewById(R.id.send_button);
        mPlayOtherRecordButton = (Button) findViewById(R.id.play_other_record_button);


        // Tracking session name (could be like c1= contact 1 name, etc)
        String trackingSession = new String("tracking_c1_c2/");

        // Initialize Firebase app
        FirebaseApp.initializeApp(this);

        // Get instance of storage
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create references to top and current tracking session storage locations
        StorageReference topStorageRef = storage.getReferenceFromUrl("gs://fir-storagetest-5f200.appspot.com");

        // Upload file
        StorageReference storageRefUp = topStorageRef.child(trackingSession+"image1");

        // TODO: check for best way to handle this, particularly first run aftewe install fails!!
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        // Upload local file
        Uri file = Uri.fromFile(new File("/storage/emulated/0/DCIM/Camera/IMG_20161203_004157.jpg"));
        UploadTask uploadTask = storageRefUp.putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG,"Upload failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"Upload completed");
            }
        });

        // Download file from remote storage
        StorageReference storageReferenceDown =
                topStorageRef.child(trackingSession+"7f0b5b7e-5067-459d-8572-0e00f9ea60a3.jpg");
        try {
            // TODO: I don't like to put everything within Exc catching block
            final File localFile = File.createTempFile("image2", "jpg");
            storageReferenceDown.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG,"Download completed in " + localFile.getAbsoluteFile());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG,"Download failed");
                }
            });

        } catch (IOException ioException) {
            Log.d(TAG,"Exception: " + ioException.toString());
        }


    }
}
