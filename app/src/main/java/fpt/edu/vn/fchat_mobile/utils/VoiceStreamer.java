package fpt.edu.vn.fchat_mobile.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

public class VoiceStreamer {
    private static final String TAG = "VoiceStreamer";
    
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHUNK_SIZE = 1024;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private String chatId;
    private String userId;
    
    public interface VoiceStreamListener {
        void onAudioDataReady(String audioData, String chatId, String userId);
    }
    
    private VoiceStreamListener streamListener;
    
    public VoiceStreamer(String chatId, String userId) {
        this.chatId = chatId;
        this.userId = userId;
    }
    
    public void setStreamListener(VoiceStreamListener listener) {
        this.streamListener = listener;
    }
    
    public void startStreaming() {
        if (isRecording) return;
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size for AudioRecord");
                return;
            }
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSize * 4
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }
            
            int playbackBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
            audioTrack = new AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT,
                playbackBufferSize * 4,
                AudioTrack.MODE_STREAM
            );
            
            audioTrack.setVolume(1.0f);
            
            isRecording = true;
            audioRecord.startRecording();
            audioTrack.play();
            isPlaying = true;
            
            new Thread(() -> {
                byte[] audioBuffer = new byte[CHUNK_SIZE];
                
                while (isRecording) {
                    int readResult = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (readResult > 0) {
                        if (hasAudioContent(audioBuffer, readResult)) {
                            String encodedAudio = Base64.encodeToString(audioBuffer, 0, readResult, Base64.NO_WRAP);
                            
                            if (streamListener != null) {
                                streamListener.onAudioDataReady(encodedAudio, chatId, userId);
                            }
                        }
                    }
                    
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Audio recording permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start voice streaming", e);
        }
    }
    
    public void playReceivedAudio(String encodedAudio) {
        if (!isPlaying || audioTrack == null) return;
        
        try {
            byte[] audioData = Base64.decode(encodedAudio, Base64.NO_WRAP);
            
            byte[] processedAudio = enhanceAudioQuality(audioData);
            
            int bytesWritten = audioTrack.write(processedAudio, 0, processedAudio.length);
            if (bytesWritten < 0) {
                Log.w(TAG, "AudioTrack write failed: " + bytesWritten);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing received audio", e);
        }
    }
    
    private byte[] enhanceAudioQuality(byte[] audioData) {
        byte[] enhanced = new byte[audioData.length];
        
        for (int i = 0; i < audioData.length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            
            sample = (short) Math.max(-32768, Math.min(32767, sample * 1.5));
            
            enhanced[i] = (byte) (sample & 0xFF);
            enhanced[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return enhanced;
    }
    
    public void stopStreaming() {
        isRecording = false;
        isPlaying = false;
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
        }
        
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio track", e);
            }
        }
        
        Log.d(TAG, "Voice streaming stopped");
    }
    
    private boolean hasAudioContent(byte[] buffer, int length) {
        int threshold = 500;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            if (Math.abs(sample) > threshold) {
                return true;
            }
        }
        return false;
    }
}
