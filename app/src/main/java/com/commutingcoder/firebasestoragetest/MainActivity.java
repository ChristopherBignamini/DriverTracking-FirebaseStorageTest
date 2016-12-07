package com.commutingcoder.firebasestoragetest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
    private static final int MY_PERMISSIONS_RECORD_AUDIO = 2;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 3;
    private Button mRecordButton;
    private Button mPlayMyRecordButton;
    private Button mSendButton;
    private Button mPlayOtherRecordButton;
    private Button mReceiveButton;
    private boolean mIsMyAudioAvailable;
    private MediaRecorder mMediaRecorder;
    private final String mMyAudioFileName = "my_audio";
    private final String mOtherAudioFileName = "other_audio";
    private final String audioFileStoragePath = "/data/data/com.bignerdranch.android.firebasestoragetest/databases";
    private final String mMyAudioFileFullName = audioFileStoragePath+"/"+mMyAudioFileName;
    private final String mOtherAudioFileFullName = audioFileStoragePath+"/"+mOtherAudioFileName;
    private MediaPlayer mMediaPlayer;// TODO: is it better to use local variable or data members?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup UI elements
        mRecordButton = (Button) findViewById(R.id.record_button);
        mPlayMyRecordButton = (Button) findViewById(R.id.play_my_record_button);
        mSendButton = (Button) findViewById(R.id.send_button);
        mPlayOtherRecordButton = (Button) findViewById(R.id.play_other_record_button);
        mReceiveButton = (Button) findViewById(R.id.receive_button);

        // Firebase initialization and reference retrieval stuff
        FirebaseApp.initializeApp(this);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference topStorageRef = storage.getReferenceFromUrl("gs://fir-storagetest-5f200.appspot.com");
        String trackingSession = new String("tracking_c1_c2/");
        final StorageReference storageRefUp = topStorageRef.child(trackingSession+"audio_c1");
        final StorageReference storageRefDown = topStorageRef.child(trackingSession+"audio_c2");

        // Upload/Download file path and names
        // TODO: store audio files in /data/data/ com.bignerdranch.android.firebasestoragetest
        final String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        // We set this to true every time we record a new message to be shared
        mIsMyAudioAvailable = false;

        // Ask required permissions
        // TODO: these should be asked only when needed, find better design solution
        // TODO: check for best way to handle this, particularly first run aftewe install fails!!
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.RECORD_AUDIO)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            }
        }
        // TODO: check for best way to handle this, particularly first run aftewe install fails!!
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getParent(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(getParent(),
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

            }
        }
        // TODO: check for best way to handle this, particularly first run aftewe install fails!!
        // TODO: can we unify permission requests?
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }


        mRecordButton.setOnClickListener(new View.OnClickListener() {

            private boolean mIsRecordingActive = false;

            @Override
            public void onClick(View v) {
                if(mIsRecordingActive == false) {

                    // TODO: isn't file creation performed by mediarecorder?
                    final File myAudioFile = new File(audioFileStoragePath, mMyAudioFileName);
                    if (!myAudioFile.exists()) {
                        try {
                            myAudioFile.createNewFile();
                        } catch (IOException ioException) {
                            Log.e(TAG, "Record exception (file creation): " + ioException.toString());
                        }
                    }

                    // TODO: don't know why but we need to set mediarecorder to null and reinstantiate it to avoid segfault
                    // TODO: which format and encoder?
                    mMediaRecorder = new MediaRecorder();
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mMediaRecorder.setOutputFile(mMyAudioFileFullName);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    try {
                        Log.d(TAG, "Prepare record");
                        // TODO: when use prepare?
                        mMediaRecorder.prepare();
                        Log.d(TAG, "Start record");
                        mMediaRecorder.start();
                        Log.d(TAG, "Record started");
                    } catch (IOException ioException) {
                        Log.e(TAG, "Record exception: " + ioException.toString());
                    } catch (IllegalStateException isException)  {
                        Log.e(TAG, "Record exception: " + isException.toString());
                    }
                    mIsRecordingActive = true;
                } else {
                    Log.d(TAG, "Stop record");
                    mIsRecordingActive = false;
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    Log.d(TAG, "Record stopped");
                    mMediaRecorder = null;
                    mIsMyAudioAvailable = true;
                }
            }
        });


        mPlayMyRecordButton.setOnClickListener(new View.OnClickListener() {

            private boolean mIsPlayingActive = false;

            @Override
            public void onClick(View v) {

                if (mIsPlayingActive == false) {

                    File file = new File(mMyAudioFileFullName);
                    if (file.exists()) {
                        Log.d(TAG, "File exist, setup reproduction");
                        mMediaPlayer = new MediaPlayer();
                        try {
                            mMediaPlayer.setDataSource(mMyAudioFileFullName);
                            mMediaPlayer.prepare();// TODO: have a look to documentation for asyncronous preparation step
                        } catch (IOException ioException) {
                            Log.e(TAG, "Play my audio exception: " + ioException.toString());
                        }
                        Log.d(TAG, "Start reproduction");
                        mMediaPlayer.start();
                        Log.d(TAG, "Reproduction completed");
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "My audio file not available", Toast.LENGTH_LONG).show();
                    }
                    mIsPlayingActive = true;
                } else {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                    mIsPlayingActive = false;
                }
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mIsMyAudioAvailable == true) {

                    Uri file = Uri.fromFile(new File(mMyAudioFileFullName));
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


                    mIsMyAudioAvailable = false;

                } else {
                    Toast.makeText(getApplicationContext(),"No new audio available", Toast.LENGTH_LONG);
                    Log.d(TAG,"Send audio: no new audio available");
                }
            }
        });

        mPlayOtherRecordButton.setOnClickListener(new View.OnClickListener() {

            // TODO: factorize playing part ina a general onClickListener
            private boolean mIsPlayingActive = false;

            @Override
            public void onClick(View v) {

                // TODO: add check for file availability?
                if (mIsPlayingActive == false) {

                    File file = new File(mOtherAudioFileFullName);
                    if (file.exists()) {
                        Log.d(TAG, "File exist, setup reproduction");
                        mMediaPlayer = new MediaPlayer();
                        try {
                            mMediaPlayer.setDataSource(mOtherAudioFileFullName);
                            mMediaPlayer.prepare();// TODO: have a look to documentation for asyncronous preparation step
                        } catch (IOException ioException) {
                            Log.e(TAG, "Play my audio exception: " + ioException.toString());
                        }
                        Log.d(TAG, "Start reproduction");
                        mMediaPlayer.start();
                        Log.d(TAG, "Reproduction completed");
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "My audio file not available", Toast.LENGTH_LONG).show();
                    }
                    mIsPlayingActive = true;
                } else {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                    mIsPlayingActive = false;
                }


            }
        });

        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // TODO: add check for remote file availability?
                try {
                    // TODO: I don't like to put everything within Exc catching block
                    // TODO use uri instead?
                    final File otherAudioFile = new File(audioFileStoragePath, mOtherAudioFileName);
                    if (!otherAudioFile.exists()) {
                        otherAudioFile.createNewFile();
                    }

                    storageRefDown.getFile(otherAudioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG,"Download completed in " + otherAudioFile.getAbsoluteFile());
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
        });

        // TODO: debug, remove!
//        // Upload local file
//        Uri file = Uri.fromFile(new File("/storage/emulated/0/DCIM/Camera/IMG_20161203_004157.jpg"));
//        UploadTask uploadTask = storageRefUp.putFile(file);
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d(TAG,"Upload failed");
//            }
//        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.d(TAG,"Upload completed");
//            }
//        });
//
//        // Download file from remote storage
//        StorageReference storageReferenceDown =
//                topStorageRef.child(trackingSession+"7f0b5b7e-5067-459d-8572-0e00f9ea60a3.jpg");
//        try {
//            // TODO: I don't like to put everything within Exc catching block
//            final File localFile = File.createTempFile("image2", "jpg");
//            storageReferenceDown.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                    Log.d(TAG,"Download completed in " + localFile.getAbsoluteFile());
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Log.d(TAG,"Download failed");
//                }
//            });
//
//        } catch (IOException ioException) {
//            Log.d(TAG,"Exception: " + ioException.toString());
//        }


    }
}
