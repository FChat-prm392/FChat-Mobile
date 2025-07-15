package fpt.edu.vn.fchat_mobile.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class AudioUtils {
    private static final String TAG = "AudioUtils";
    
    // Voice activity detection
    private static AudioRecord audioRecord;
    private static boolean isRecording = false;
    private static VoiceActivityListener voiceActivityListener;
    private static Handler voiceHandler = new Handler(Looper.getMainLooper());
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final double VOICE_THRESHOLD = 1000.0;
    
    public interface VoiceActivityListener {
        void onVoiceActivityChanged(boolean isSpeaking);
    }
    
    public static void setVoiceActivityListener(VoiceActivityListener listener) {
        voiceActivityListener = listener;
    }
    
    public static void logAudioState(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        
        Log.d(TAG, "=== Audio State Debug ===");
        Log.d(TAG, "Audio Mode: " + audioManager.getMode());
        Log.d(TAG, "Speakerphone On: " + audioManager.isSpeakerphoneOn());
        Log.d(TAG, "Microphone Muted: " + audioManager.isMicrophoneMute());
        Log.d(TAG, "Music Active: " + audioManager.isMusicActive());
        Log.d(TAG, "Wired Headset On: " + audioManager.isWiredHeadsetOn());
        Log.d(TAG, "Bluetooth SCO On: " + audioManager.isBluetoothScoOn());
        Log.d(TAG, "Volume (Voice Call): " + audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        Log.d(TAG, "Volume (Music): " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.d(TAG, "=========================");
    }
    
    public static void setupForVoiceCall(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        
        int result = audioManager.requestAudioFocus(
            null, 
            AudioManager.STREAM_VOICE_CALL, 
            AudioManager.AUDIOFOCUS_GAIN
        );
        
        logAudioState(context);
        
        startVoiceActivityDetection();
    }
    
    public static void startVoiceActivityDetection() {
        if (isRecording) return;
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size for AudioRecord");
                return;
            }
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }
            
            isRecording = true;
            audioRecord.startRecording();
            
            new Thread(() -> {
                short[] audioBuffer = new short[bufferSize];
                boolean wasSpeaking = false;
                
                while (isRecording) {
                    int readResult = audioRecord.read(audioBuffer, 0, bufferSize);
                    if (readResult > 0) {
                        double amplitude = calculateAmplitude(audioBuffer, readResult);
                        boolean isSpeaking = amplitude > VOICE_THRESHOLD;
                        
                        if (isSpeaking != wasSpeaking) {
                            wasSpeaking = isSpeaking;
                            
                            if (voiceActivityListener != null) {
                                voiceHandler.post(() -> voiceActivityListener.onVoiceActivityChanged(isSpeaking));
                            }
                        }
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Audio recording permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start voice activity detection", e);
        }
    }
    
    private static double calculateAmplitude(short[] audioBuffer, int length) {
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Math.abs(audioBuffer[i]);
        }
        return sum / length;
    }
    
    // Method to adjust sensitivity for testing
    public static void adjustVoiceSensitivity(double newThreshold) {
        Log.d(TAG, "Voice sensitivity adjusted from " + VOICE_THRESHOLD + " to " + newThreshold);
        // Note: VOICE_THRESHOLD is final, so this is just for logging
        // You can modify the constant above if needed during testing
    }
    
    public static void stopVoiceActivityDetection() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping voice activity detection", e);
            }
        }
    }
    
    public static void cleanupAfterCall(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        
        stopVoiceActivityDetection();
        
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);
        audioManager.abandonAudioFocus(null);
        
        logAudioState(context);
    }
}
