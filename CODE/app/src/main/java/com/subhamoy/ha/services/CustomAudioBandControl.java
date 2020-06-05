package com.subhamoy.ha.services;

import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.DynamicsProcessing.Config;
import android.media.audiofx.DynamicsProcessing.Mbc;
import android.media.audiofx.DynamicsProcessing.MbcBand;
import android.util.Log;


public class CustomAudioBandControl {

    private int sessionId;

    public CustomAudioBandControl(int sessionId) {
        this.sessionId = sessionId;
    }

    public void controller() {
        Config.Builder builder =
                new Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        1,
                        true, 5,
                        true, 5,
                        true, 5,
                        true
                );
        builder.setPreferredFrameDuration(10); // in millisecond
        Config config = builder.build();
        Mbc mbc = config.getChannelByChannelIndex(0).getMbc();

        int attackTime = 1, releaseTime = 10;
        float [] ratio = {4.0f, 6.0f, 4.0f, 4.0f, 4.0f}; // infi:1
        float [] threshold = {-8.2f, -8.2f, -60.0f, -58.2f, -58.2f}; // 0.0 to -60.0 dB
        float [] knee = {100, 100, 100, 100, 100}; // Left 0%=Soft, Right 100%=Hard
        float [] noise_threshold = {-80.0f, -50.0f, -0.0f, -50.0f, -50.0f};
        float [] expander_ratio = {0.25f, 0.25f, 0.25f, 0.25f, 0.25f};
        float [] pre_gain = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        float [] post_gain = {2.0f, 2.0f, -20.0f, -20.0f, -20.0f};


        // MBC :: Band 1
        MbcBand band = mbc.getBand(0);
        band.setAttackTime(attackTime);
        band.setReleaseTime(releaseTime);
        band.setRatio(ratio[0]);
        band.setThreshold(threshold[0]);
        band.setKneeWidth(knee[0]);
        band.setNoiseGateThreshold(noise_threshold[0]);
        band.setExpanderRatio(expander_ratio[0]);
        band.setPreGain(pre_gain[0]);
        band.setPostGain(post_gain[0]);

        // MBC :: Band 2
        band = mbc.getBand(1);
        band.setAttackTime(attackTime);
        band.setReleaseTime(releaseTime);
        band.setRatio(ratio[1]);
        band.setThreshold(threshold[1]);
        band.setKneeWidth(knee[1]);
        band.setNoiseGateThreshold(noise_threshold[1]);
        band.setExpanderRatio(expander_ratio[1]);
        band.setPreGain(pre_gain[1]);
        band.setPostGain(post_gain[1]);

        // MBC :: Band 3
        band = mbc.getBand(2);
        band.setAttackTime(attackTime);
        band.setReleaseTime(releaseTime);
        band.setRatio(ratio[2]);
        band.setThreshold(threshold[2]);
        band.setKneeWidth(knee[2]);
        band.setNoiseGateThreshold(noise_threshold[2]);
        band.setExpanderRatio(expander_ratio[2]);
        band.setPreGain(pre_gain[2]);
        band.setPostGain(post_gain[2]);

        // MBC :: Band 4
        band = mbc.getBand(3);
        band.setAttackTime(attackTime);
        band.setReleaseTime(releaseTime);
        band.setRatio(ratio[3]);
        band.setThreshold(threshold[3]);
        band.setKneeWidth(knee[3]);
        band.setNoiseGateThreshold(noise_threshold[3]);
        band.setExpanderRatio(expander_ratio[3]);
        band.setPreGain(pre_gain[3]);
        band.setPostGain(post_gain[3]);

        // MBC :: Band 5
        band = mbc.getBand(4);
        band.setAttackTime(attackTime);
        band.setReleaseTime(releaseTime);
        band.setRatio(ratio[4]);
        band.setThreshold(threshold[4]);
        band.setKneeWidth(knee[4]);
        band.setNoiseGateThreshold(noise_threshold[4]);
        band.setExpanderRatio(expander_ratio[4]);
        band.setPreGain(pre_gain[4]);
        band.setPostGain(post_gain[4]);


        DynamicsProcessing myDPE = new DynamicsProcessing(
                0,
                sessionId,
                config
        );

        myDPE.setEnabled(true);

//        Equalizer equalizer = new Equalizer(0, sessionId);
//        equalizer.setEnabled(true);
//        Log.i("subha", "No of equalizer bands :: " + equalizer.getNumberOfBands());

        // Inbuilt Presets
//        String [] music_styles = new String[equalizer.getNumberOfBands()];
//        for(int k=0; k < equalizer.getNumberOfBands() ; k++) {
//            music_styles[k] = equalizer.getPresetName((short) k);
//            Log.i("subha", "Present Name :: " + music_styles[k]);
//        }
//        equalizer.usePreset((short) 1); // x = 0 to 9

    }
}
