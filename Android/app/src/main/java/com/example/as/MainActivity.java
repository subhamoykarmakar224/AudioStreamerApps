package com.example.as;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // List of all permissions required
    String [] appPermissions = {
            Manifest.permission.RECORD_AUDIO
    };

    // Widgets
    Button btnStart;
    Button btnStop;
    SeekBar seekBarVolControl;
    TextView textViewVolLvl;

    // Others
    Thread threadAudioTrack;
    boolean recording = false;
    final static int SAMPLE_RATE = 44100; // 44100, 22050, 11025, 16000, 8000
    AudioManager audioManager;
    AudioTrack audioTrack;
    AudioRecord audioRecord;
    String audioRecordID = "Record-EGF-012";
    private static final int PERMISSIONS_REQUEST_CODE=1240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialization
        initApp();

    }

    private boolean getAllPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for(String perms : appPermissions) {
            if(ContextCompat.checkSelfPermission(MainActivity.this, perms) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perms);
            }
        }
        if(!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    public void initApp() {
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        seekBarVolControl = findViewById(R.id.seekBarVolumeControl);
        textViewVolLvl = findViewById(R.id.textViewVolumeLevel);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        seekBarVolControl.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekBarVolControl.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        textViewVolLvl.setText("Level : " + String.valueOf(seekBarVolControl.getProgress()));

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart();
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop();
            }
        });

        seekBarVolControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress,
                        0
                );
                try{
                    float prog = (float) seekBarVolControl.getProgress();
                    float max = (float) seekBarVolControl.getMax();
                    float volumeLevel = (prog / max);
                    audioTrack.setVolume(volumeLevel);
                } catch(Exception e) {
                    e.getStackTrace();
                }
                textViewVolLvl.setText("Level : " + String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        getAllPermissions();
    }

    private void btnStart() {
        Log.i("subha", "BTN-START-CLICKED");
        btnStart.setEnabled(false);
        btnStart.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        final int channelIn = AudioFormat.CHANNEL_IN_MONO;
        final int channelOut = AudioFormat.CHANNEL_OUT_MONO;
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    recording = true;
                    int buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelIn, audioEncoding);

                    audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE,
                            channelIn,
                            audioEncoding,
                            buffersize
                    );
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            SAMPLE_RATE,
                            channelOut,
                            audioEncoding,
                            buffersize,
                            AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);

                    audioTrack.setPlaybackRate(SAMPLE_RATE);

                    float prog = (float) seekBarVolControl.getProgress();
                    float max = (float) seekBarVolControl.getMax();
                    float volumeLevel = (prog / max);
                    audioTrack.setVolume(volumeLevel);

                    byte[] buffer = new byte[buffersize];
                    try {
                        audioRecord.startRecording();
                    }catch (IllegalStateException e) {
                        audioRecord = new AudioRecord(
                                MediaRecorder.AudioSource.MIC,
                                SAMPLE_RATE,
                                channelIn,
                                audioEncoding,
                                buffersize
                        );
                    }
                    try{
                        audioTrack.play();
                    }catch (IllegalStateException e) {
                        audioTrack = new AudioTrack(
                                AudioManager.STREAM_VOICE_CALL,
                                SAMPLE_RATE,
                                channelOut,
                                audioEncoding,
                                buffersize,
                                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
                    }

                    while (recording) {
                        audioRecord.read(buffer, 0, buffersize);
                        for(byte a : buffer){
                            Log.i("subha", String.valueOf(a));
                        }
                        Log.i("subha", String.valueOf("=============================================="));
                        audioTrack.write(buffer, 0, buffer.length);
                    }
                }
            };
            threadAudioTrack = new Thread(runnable);
            threadAudioTrack.start();
        } catch(Exception e) {
            btnStart.setEnabled(true);
            btnStart.getBackground().setColorFilter(null);
            recording = false;
            audioTrack.release();
            audioRecord.stop();
        }
    }

    private void btnStop() {
        Log.i("subha", "BTN-STOP-CLICKED");
        recording = false;
        threadAudioTrack.interrupt();
        btnStart.setEnabled(true);
        btnStart.getBackground().setColorFilter(null);
        audioTrack.stop();
        audioRecord.release();
    }

    private AudioManager noiseControlInTrack(AudioManager audioManager) {
//        audioManager.setParameters("noise_suppression=on");
//        ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).setParameters("noise_suppression=on");
        return audioManager;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recording = false;
    }
}
