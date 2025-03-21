package com.ishaanbhela.meetingapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String outputFile;
    private Button recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        EditText editText = findViewById(R.id.conferenceName);
        recordButton = findViewById(R.id.recordButton);

        // Set up the record button
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                recordButton.setText("Start Recording");
            } else {
                if (checkPermissions()) {
                    startRecording();
                    recordButton.setText("Stop Recording");
                } else {
                    requestPermissions();
                }
            }
        });
    }

    public void onButtonClick(View view) {
        // Initialize EditText
        EditText editText = findViewById(R.id.conferenceName);

        // Get the room name from the EditText
        String text = editText.getText().toString();

        // Check if the user has entered a room name
        if (text.length() > 0) {
            // Create JitsiMeetConferenceOptions with recording enabled
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setRoom(text) // Set the room name
                    .setFeatureFlag("welcomepage.enabled", false) // Disable welcome page
                    .setFeatureFlag("recording.enabled", true) // Enable recording
                    .setFeatureFlag("recording.local.enabled", true) // Enable local recording
                    .setAudioMuted(true) // Mute audio by default
                    .setVideoMuted(true) // Mute video by default
                    .build();

            // Launch the Jitsi Meet activity with the configured options
            JitsiMeetActivity.launch(this, options);
        } else {
            // Show a toast if the room name is empty
            Toast.makeText(this, "Please enter a room name", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, check MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else {
            // For Android 10 and below, check WRITE_EXTERNAL_STORAGE and RECORD_AUDIO
            return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, request MANAGE_EXTERNAL_STORAGE
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            // For Android 10 and below, request WRITE_EXTERNAL_STORAGE and RECORD_AUDIO
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
                recordButton.setText("Stop Recording");
            } else {
                Toast.makeText(this, "Permissions denied. Cannot record.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore API for Android 10 and above
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, "recording.mp4");
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try {
                    // Open the URI and get a FileDescriptor
                    ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "w");
                    if (pfd != null) {
                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setOutputFile(pfd.getFileDescriptor());

                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        isRecording = true;
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to open file descriptor", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("StartRecording", "Failed to start recording: " + e.getMessage());
                    Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to create recording file", Toast.LENGTH_SHORT).show();
            }
        } else {
            // For Android 9 and below, use the old method
            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp4";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFile);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("StartRecording", "Failed to start recording: " + e.getMessage());
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            Toast.makeText(this, "Recording saved", Toast.LENGTH_LONG).show();
        }
    }
}