package com.subhamoy.ha;

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
import android.media.audiofx.Equalizer;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.subhamoy.ha.services.CustomAudioBandControl;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE=1234;
    final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    final int channelIn = AudioFormat.CHANNEL_IN_MONO;
    final int channelOut = AudioFormat.CHANNEL_OUT_MONO;
    String [] appPermissions = {
            Manifest.permission.RECORD_AUDIO
    };

    // Widgets
    Button btnListenStart, btnListenStop;
    SeekBar seekBarVol, seekBarEq, seekBarNoise;
    Spinner spinnerEqBands;

    // Others
    final static int SAMPLE_RATE = 44100;
    Thread threadAudioTrack;
    boolean recording = false;
    AudioManager audioManager;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    Equalizer equalizer;
    int currentBand;
    short [] eqbandvalues = new short[5];
    float [] noisebandvalues = new float[5];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnListenStart = findViewById(R.id.btnListenStart);
        btnListenStop = findViewById(R.id.btnListenStop);
        spinnerEqBands = findViewById(R.id.spinnerEqBands);
        seekBarVol = findViewById(R.id.seekBarVolumeControl);
        seekBarEq = findViewById(R.id.seekBarEq);
        seekBarNoise = findViewById(R.id.seekBarNoiseThresh);

        // Initialize Spinner values and listener
        initializeEqSpinner();

        // Gets all permissions
        getAllPermissions();

        // Initialize the volume control
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // TODO :: See if this is required
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        seekBarEq.setProgress(0);

        // Initialize Start button
        btnListenStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart();
            }
        });

        // Initialize Stop button
        btnListenStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop();
            }
        });

        // Seekbar Volume Control logic
        initializeSeekBarVolControlLog();


    }

    // Gets necessary permissions for the app
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
                    PERMISSION_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    // Initialize Spinner values and listener
    private void initializeEqSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1,
                new String[]{"1", "2", "3", "4", "5"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEqBands.setAdapter(adapter);
        spinnerEqBands.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentBand = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // Seekbar Volume Control logic
    private void initializeSeekBarVolControlLog() {
        seekBarVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress,
                        0
                );
                try {
                    float prog = (float) seekBarVol.getProgress();
                    float max = (float) seekBarVol.getMax();
                    float volumeLevel = (prog / max);
                    audioTrack.setVolume(volumeLevel);
                } catch (Exception e) {
                    Log.i("ollo", getLocalClassName() + " initializeSeekBarVolControlLog() :: Exception - " + e.getMessage());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Start audio streaming
     */
    public void btnStart() {
        Log.i("ollo", getCallingPackage() + " :: Audio Streaming :: START");
        btnListenStart.setEnabled(false);
        btnListenStart.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
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

                    // Frequency Band Controller Class
                    CustomAudioBandControl bandControl = new CustomAudioBandControl(audioTrack.getAudioSessionId());
                    bandControl.controller();

                    // Equalizer
                    setPreEqualizer(audioTrack.getAudioSessionId());

                    // NoiseSuppressor
                    setNoiseSuppression(audioRecord.getAudioSessionId());

                    // Piping
                    while (recording) {
                        audioRecord.read(buffer, 0, buffersize);
                        audioTrack.write(buffer, 0, buffer.length);
                    }
                }
            };
            threadAudioTrack = new Thread(runnable);
            threadAudioTrack.start();
        } catch(Exception e) {
            btnListenStart.setEnabled(true);
            btnListenStart.getBackground().setColorFilter(null);
            recording = false;
            audioTrack.release();
            audioRecord.stop();
        }
    }

    /**
     * Stop audio streaming
     */
    public void btnStop() {
        Log.i("ollo", getCallingPackage() + " :: Audio Streaming :: STOP");
        recording = false;
        threadAudioTrack.interrupt();
        btnListenStart.setEnabled(true);
        btnListenStart.getBackground().setColorFilter(null);
        audioTrack.stop();
        audioRecord.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recording = false;
        equalizer.setEnabled(false);
        equalizer.release();
        equalizer = null;
    }

    protected void setPreEqualizer(int sessionId) {
        String band1Level = "12";
        String band2Level = "12";
        String band3Level = "-22";
        String band4Level = "-22";
        String band5Level = "-22";
        equalizer = new Equalizer(0, sessionId);
        equalizer.setProperties(
                new Equalizer.Settings(
                        "Equalizer;curPreset=-1;numBands=5;" +
                        "band1Level=" + band1Level + ";band2Level=" + band2Level +
                        ";band3Level=" + band3Level + ";band4Level=" + band4Level +
                        ";band5Level=" + band5Level + ";"
                )
        );
        equalizer.setEnabled(true);
        //MALE :: 85 to 180 Hz, FEMALE :: 165 to 255 Hz

//        Integer [] eqData = {
//                0, 0, 910000, -2000001, -7000001
//        };
        //MIN Values (500 to 2000hz i.e. Band 3)
//        2020-03-10 20:40:57.383 1678-1804/com.example.as I/subha: Band 1 :: 30000
//        2020-03-10 20:40:57.384 1678-1804/com.example.as I/subha: Band 2 :: 120001
//        2020-03-10 20:40:57.384 1678-1804/com.example.as I/subha: Band 3 :: 460001
//        2020-03-10 20:40:57.385 1678-1804/com.example.as I/subha: Band 4 :: 1800001
//        2020-03-10 20:40:57.385 1678-1804/com.example.as I/subha: Band 5 :: 7000001
        // CENTER Freq
//        2020-03-10 20:49:36.805 3526-3770/com.example.as I/subha: Band 1 :: 60000
//        2020-03-10 20:49:36.805 3526-3770/com.example.as I/subha: Band 2 :: 230000
//        2020-03-10 20:49:36.805 3526-3770/com.example.as I/subha: Band 3 :: 910000
//        2020-03-10 20:49:36.806 3526-3770/com.example.as I/subha: Band 4 :: 3600000
//        2020-03-10 20:49:36.806 3526-3770/com.example.as I/subha: Band 5 :: 14000000
        // MAX Values
//        2020-03-10 20:42:00.182 1957-2000/com.example.as I/subha: Band 1 :: 120000
//        2020-03-10 20:42:00.183 1957-2000/com.example.as I/subha: Band 2 :: 460000
//        2020-03-10 20:42:00.183 1957-2000/com.example.as I/subha: Band 3 :: 1800000
//        2020-03-10 20:42:00.184 1957-2000/com.example.as I/subha: Band 4 :: 7000000
//        2020-03-10 20:42:00.184 1957-2000/com.example.as I/subha: Band 5 :: 20000000


//        Log.i("subha", "Band 1 :: " + equalizer.getBandFreqRange((short) 0)[0]);
//        Log.i("subha", "Band 1 :: " + equalizer.getBandFreqRange((short) 0)[1]);
//        equalizer.setBandLevel((short) 0, eqData[0].shortValue());
//        equalizer.setBandLevel((short) 1, eqData[1].shortValue());
//        equalizer.setBandLevel((short) 2, eqData[2].shortValue());
//        equalizer.setBandLevel((short) 3, eqData[3].shortValue());
//        equalizer.setBandLevel((short) 4, eqData[4].shortValue());
//        Log.i("subha", equalizer.getProperties().toString());

//        short bands = equalizer.getNumberOfBands();
//        for(int i = 0; i < bands ; i ++) {
//            Log.i("subha", "<=============================================>");
//            Log.i("subha", "Band : " + i);
//            Log.i("subha", "MinEQLevel: " + equalizer.getBandLevelRange()[0]);
//            Log.i("subha", "MaxEQLevel: " + equalizer.getBandLevelRange()[1]);
//            Log.i("subha", "CenterEQLevel: " + equalizer.getCenterFreq((short) i)/1000 + "Hz");
//        }

    }

    protected void setNoiseSuppression(int sessionId) {
        NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(sessionId);
        noiseSuppressor.setEnabled(true);
    }

    protected void setEqualizerBandLevel(short bandNumber, short level) {
        equalizer.setBandLevel(bandNumber, level);
    }
}
