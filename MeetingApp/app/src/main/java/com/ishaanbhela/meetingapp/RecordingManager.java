package com.ishaanbhela.meetingapp;
import android.media.MediaRecorder;
import android.os.Environment;
import java.io.IOException;

public class RecordingManager {
    private MediaRecorder mediaRecorder;
    private String outputFile;

    public RecordingManager() {
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp4";
        mediaRecorder = new MediaRecorder();
    }

    public void startRecording() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
    }
}