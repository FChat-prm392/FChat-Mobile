package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.SurfaceView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.utils.AudioUtils;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;
import fpt.edu.vn.fchat_mobile.utils.VoiceStreamer;
import fpt.edu.vn.fchat_mobile.utils.VideoStreamer;
import fpt.edu.vn.fchat_mobile.utils.VideoRenderer;

public class CallActivity extends AppCompatActivity implements SocketManager.CallListener, AudioUtils.VoiceActivityListener, VoiceStreamer.VoiceStreamListener, VideoStreamer.VideoStreamListener {

    private ImageView avatarView, btnEndCall, btnMute, btnSpeaker, btnVideo, btnSwitchCamera, remoteVideoView;
    private TextView nameText, statusText, callDurationText, cameraStatusText;
    private View videoContainer, speakingIndicator;
    private SurfaceView localCameraPreview;
    
    private String chatId;
    private String participantName;
    private String participantId;
    private String avatarUrl;
    private boolean isVideoCall;
    private boolean isIncomingCall;
    private boolean isCallConnected = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isVideoOn = true;
    
    private AudioManager audioManager;
    private MediaPlayer ringtone;
    private Handler callDurationHandler = new Handler();
    private Handler callTimeoutHandler = new Handler(); // For handling call timeouts
    private long callStartTime;
    private SessionManager sessionManager;
    private VoiceStreamer voiceStreamer;
    private VideoStreamer videoStreamer;
    private VideoRenderer videoRenderer;
    
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 200;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 201;
    
    private final Runnable updateDurationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCallConnected) {
                long duration = System.currentTimeMillis() - callStartTime;
                int seconds = (int) (duration / 1000) % 60;
                int minutes = (int) (duration / (1000 * 60)) % 60;
                int hours = (int) (duration / (1000 * 60 * 60));
                
                String timeString;
                if (hours > 0) {
                    timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                }
                callDurationText.setText(timeString);
                callDurationHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        
        sessionManager = new SessionManager(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        // Set up voice activity listener
        AudioUtils.setVoiceActivityListener(this);
        
        checkAudioPermissions();
        
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId");
        participantName = intent.getStringExtra("participantName");
        participantId = intent.getStringExtra("participantId");
        avatarUrl = intent.getStringExtra("avatarUrl");
        isVideoCall = intent.getBooleanExtra("isVideoCall", false);
        isIncomingCall = intent.getBooleanExtra("isIncomingCall", false);
        boolean startMuted = intent.getBooleanExtra("startMuted", false);
        boolean isCallAnswered = intent.getBooleanExtra("isCallAnswered", false);
        
        if (isVideoCall) {
            checkCameraPermissions();
        }
        
        // Initialize voice streamer AFTER getting chatId
        String currentUserId = sessionManager.getCurrentUserId();
        if (chatId != null && currentUserId != null) {
            voiceStreamer = new VoiceStreamer(chatId, currentUserId);
            voiceStreamer.setStreamListener(this);
            
            if (isVideoCall) {
                videoStreamer = new VideoStreamer(this, chatId, currentUserId);
                videoStreamer.setStreamListener(this);
            }
        }
        
        // Set initial muted state if specified
        if (startMuted) {
            isMuted = true;
        }
        
        initViews();
        setupCallUI();
        setupClickListeners();
        setupCallListeners();
        
        if (isIncomingCall && isCallAnswered) {
            // Call was already answered in IncomingCallActivity, connect immediately
            connectCall();
        } else if (isIncomingCall) {
            handleIncomingCall();
        } else {
            initiateCall();
        }
    }
    
    private void checkAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            setupAudioManager();
        }
    }
    
    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAudioManager();
            } else {
                Toast.makeText(this, "Audio permission required for voice calls", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Camera permission is required for video calls", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void setupAudioManager() {
        if (audioManager != null) {
            AudioUtils.setupForVoiceCall(this);
            AudioUtils.logAudioState(this);
        }
    }
    
    private void initViews() {
        avatarView = findViewById(R.id.call_avatar);
        nameText = findViewById(R.id.call_name);
        statusText = findViewById(R.id.call_status);
        callDurationText = findViewById(R.id.call_duration);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnMute = findViewById(R.id.btn_mute);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnVideo = findViewById(R.id.btn_video);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        videoContainer = findViewById(R.id.video_container);
        speakingIndicator = findViewById(R.id.speaking_indicator);
        
        // Initialize video views for video calls
        if (isVideoCall) {
            remoteVideoView = findViewById(R.id.remote_video_view);
            localCameraPreview = findViewById(R.id.local_camera_preview);
            cameraStatusText = findViewById(R.id.camera_status_text);
            
            if (remoteVideoView != null) {
                videoRenderer = new VideoRenderer(remoteVideoView);
            }
            
            if (localCameraPreview != null && videoStreamer != null) {
                videoStreamer.setSurfaceView(localCameraPreview);
            }
        }
    }
    
    private void setupCallUI() {
        nameText.setText(participantName);
        
        // Load avatar
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .into(avatarView);
        
        // Setup UI based on call type
        if (isVideoCall) {
            btnVideo.setVisibility(View.VISIBLE);
            btnSwitchCamera.setVisibility(View.VISIBLE);
            videoContainer.setVisibility(View.VISIBLE);
            avatarView.setVisibility(View.GONE);
        } else {
            btnVideo.setVisibility(View.GONE);
            btnSwitchCamera.setVisibility(View.GONE);
            videoContainer.setVisibility(View.GONE);
            avatarView.setVisibility(View.VISIBLE);
        }
        
        // Initial status
        if (isIncomingCall) {
            statusText.setText("Incoming " + (isVideoCall ? "video" : "voice") + " call...");
        } else {
            statusText.setText("Calling...");
        }
        
        callDurationText.setVisibility(View.GONE);
        
        // Update mute button UI if starting muted
        updateMuteButtonUI();
    }
    
    private void setupClickListeners() {
        btnEndCall.setOnClickListener(v -> endCall());
        
        btnMute.setOnClickListener(v -> toggleMute());
        
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        
        btnVideo.setOnClickListener(v -> toggleVideo());
        
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
    }
    
    private void setupCallListeners() {
        SocketManager.setupCallListeners(this);
        SocketManager.setupVoiceListeners(this);
    }
    
    private void handleIncomingCall() {
        // Show incoming call UI
        statusText.setText("Incoming " + (isVideoCall ? "video" : "voice") + " call...");
        
        // Join the socket room for real-time communication
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            Log.d("CallActivity", "Joined socket room for incoming call: " + chatId);
        }
        
        // Wait for user to manually answer the call
        // No auto-answer - user must use IncomingCallActivity to accept
    }
    
    private void initiateCall() {
        statusText.setText("Preparing call...");
        
        String currentUserId = sessionManager.getCurrentUserId();
        Log.d("CallActivity", "🚀 INITIATE CALL STARTED - User: " + currentUserId);
        
        if (currentUserId == null) {
            Log.e("CallActivity", "❌ No user ID available");
            statusText.setText("Authentication failed");
            android.widget.Toast.makeText(this, "Please log in again.", android.widget.Toast.LENGTH_LONG).show();
            endCall();
            return;
        }
        
        // Print complete socket state for debugging
        Log.d("CallActivity", "🔍 Printing socket state before readiness check:");
        SocketManager.printSocketState();
        
        // First check if we're ready for calls
        boolean ready = SocketManager.isReadyForCalls();
        Log.d("CallActivity", "📋 Call readiness result: " + ready);
        
        if (!ready) {
            Log.w("CallActivity", "⚠️ Not ready for calls, running diagnostics...");
            statusText.setText("Connecting...");
            
            // Run comprehensive diagnostics
            SocketManager.performConnectionDiagnostics(currentUserId);
            
            // Try to fix connection issues
            SocketManager.ensureConnection();
            SocketManager.forceRegistrationCheck();
            
            // Wait longer and retry with more detailed feedback
            new android.os.Handler().postDelayed(() -> {
                Log.d("CallActivity", "🔁 Retry check after 5 seconds:");
                SocketManager.printSocketState();
                
                if (SocketManager.isReadyForCalls()) {
                    Log.d("CallActivity", "✅ Connection restored, proceeding with call");
                    proceedWithCallInitiation();
                } else {
                    Log.e("CallActivity", "❌ Connection diagnostics failed after retry");
                    runOnUiThread(() -> {
                        statusText.setText("Connection failed");
                        
                        // Show detailed error message
                        String errorMsg;
                        if (!SocketManager.isConnected()) {
                            errorMsg = "Unable to connect to server. Please check your internet connection and try again.";
                            Log.e("CallActivity", "❌ Socket not connected");
                        } else if (!SocketManager.isRegistrationVerified()) {
                            errorMsg = "Registration failed. Please restart the app and try again.";
                            Log.e("CallActivity", "❌ Registration not verified");
                        } else {
                            errorMsg = "Connection error. Please check your internet and try again.";
                            Log.e("CallActivity", "❌ Unknown connection error");
                        }
                        
                        android.widget.Toast.makeText(CallActivity.this, errorMsg, android.widget.Toast.LENGTH_LONG).show();
                        endCall();
                    });
                }
            }, 5000); // Wait 5 seconds for connection to establish
            return;
        }
        
        Log.d("CallActivity", "✅ Ready for calls, proceeding immediately");
        proceedWithCallInitiation();
    }
    
    private void proceedWithCallInitiation() {
        statusText.setText("Calling...");
        
        // Join the socket room for real-time communication
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            Log.d("CallActivity", "Joined socket room for call initiation: " + chatId);
        }
        
        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserName = sessionManager.getCurrentUserFullname();
        
        // MESSENGER-STYLE: Just initiate call directly without checking online status
        // The server will handle whether the user is available or not
        String callId = chatId + "_" + System.currentTimeMillis();
        
        Log.d("CallActivity", "� MESSENGER-STYLE: Initiating call directly without status check");
        Log.d("CallActivity", "  • Call ID: " + callId);
        Log.d("CallActivity", "  • Caller: " + currentUserName + " (" + currentUserId + ")");
        Log.d("CallActivity", "  • Receiver: " + participantId);
        Log.d("CallActivity", "  • Video Call: " + isVideoCall);
        
        // Emit call initiation using SocketManager
        SocketManager.emitCallInitiate(callId, chatId, currentUserId, participantId, currentUserName, isVideoCall);
        
        Log.d("CallActivity", "✅ Call initiated - waiting for response from receiver");
        
        // Set timeout for call response (like Messenger does)
        callTimeoutHandler.postDelayed(() -> {
            runOnUiThread(() -> {
                if (!isCallConnected) {
                    Log.w("CallActivity", "⏰ Call timeout - no response from receiver");
                    statusText.setText("No answer");
                    android.widget.Toast.makeText(CallActivity.this, 
                        "No answer. Try calling again later.", 
                        android.widget.Toast.LENGTH_SHORT).show();
                    
                    // End call after timeout
                    callTimeoutHandler.postDelayed(() -> {
                        endCall();
                    }, 2000);
                }
            });
        }, 30000); // 30 second timeout like most messaging apps
        
        // Wait for receiver to answer - no auto-connection
        // Connection will happen when onCallAnswered() is triggered
    }
    
    private void answerCall() {
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Emit call answer using SocketManager
        SocketManager.emitCallAnswer(callId, participantId, currentUserId);
        connectCall();
    }
    
    private void connectCall() {
        isCallConnected = true;
        callStartTime = System.currentTimeMillis();
        
        statusText.setText("Connected");
        callDurationText.setVisibility(View.VISIBLE);
        callDurationText.setText("00:00");
        
        setupAudioManager();
        
        // Join the socket room for real-time communication
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            Log.d("CallActivity", "Joined socket room: " + chatId);
        }
        
        // Start voice streaming
        if (voiceStreamer != null) {
            voiceStreamer.startStreaming();
        }
        
        if (isVideoCall && videoStreamer != null) {
            videoStreamer.startStreaming();
        }
        
        callDurationHandler.postDelayed(updateDurationRunnable, 1000);
        
        Toast.makeText(this, "Call connected - Voice streaming active", Toast.LENGTH_SHORT).show();
    }
    
    private void endCall() {
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        
        SocketManager.emitCallEnd(callId, currentUserId, participantId);
        
        callDurationHandler.removeCallbacks(updateDurationRunnable);
        
        // Stop voice streaming
        if (voiceStreamer != null) {
            voiceStreamer.stopStreaming();
        }
        
        if (videoStreamer != null) {
            videoStreamer.releaseCamera();
        }
        
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone.release();
        }
        
        if (audioManager != null) {
            AudioUtils.cleanupAfterCall(this);
        }
        
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        updateMuteButtonUI();
        
        if (audioManager != null) {
            audioManager.setMicrophoneMute(isMuted);
            AudioUtils.logAudioState(this);
        }
        
        // Hide speaking indicator when muted
        if (isMuted && speakingIndicator != null) {
            speakingIndicator.clearAnimation();
            speakingIndicator.setVisibility(View.GONE);
        }
        
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        SocketManager.emitCallMuteToggle(callId, currentUserId, participantId, isMuted);
        
        Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
    }
    
    private void updateMuteButtonUI() {
        btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_new);
        btnMute.setColorFilter(isMuted ? getColor(R.color.red) : getColor(R.color.gray));
    }
    
    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        btnSpeaker.setImageResource(isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        btnSpeaker.setColorFilter(isSpeakerOn ? getColor(R.color.blue) : getColor(R.color.gray));
        
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeakerOn);
            AudioUtils.logAudioState(this);
        }
        
        Toast.makeText(this, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleVideo() {
        if (!isVideoCall) return;
        
        isVideoOn = !isVideoOn;
        btnVideo.setImageResource(isVideoOn ? R.drawable.ic_video : R.drawable.ic_video_off);
        btnVideo.setColorFilter(isVideoOn ? getColor(R.color.blue) : getColor(R.color.red));
        
        if (isVideoOn) {
            if (remoteVideoView != null) {
                remoteVideoView.setVisibility(View.VISIBLE);
            }
            if (localCameraPreview != null) {
                localCameraPreview.setVisibility(View.VISIBLE);
            }
            avatarView.setVisibility(View.GONE);
            if (videoStreamer != null && videoStreamer.isCameraReady()) {
                videoStreamer.startStreaming();
            }
        } else {
            if (remoteVideoView != null) {
                remoteVideoView.setVisibility(View.GONE);
            }
            if (localCameraPreview != null) {
                localCameraPreview.setVisibility(View.GONE);
            }
            avatarView.setVisibility(View.VISIBLE);
            if (videoStreamer != null) {
                videoStreamer.stopStreaming();
            }
        }
        
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        SocketManager.emitCallVideoToggle(callId, currentUserId, participantId, isVideoOn);
        
        Toast.makeText(this, isVideoOn ? "Video on" : "Video off", Toast.LENGTH_SHORT).show();
    }
    
    private void switchCamera() {
        if (!isVideoCall || videoStreamer == null) return;
        
        videoStreamer.switchCamera();
        Toast.makeText(this, "Switching camera...", Toast.LENGTH_SHORT).show();
    }
    
    private Object createCallData() {
        return new Object() {
            public final String callId = CallActivity.this.chatId + "_" + System.currentTimeMillis();
            public final String chatId = CallActivity.this.chatId;
            public final String callerId = sessionManager.getCurrentUserId();
            public final String receiverId = participantId;
            public final boolean isVideoCall = CallActivity.this.isVideoCall;
            public final String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
        };
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        callDurationHandler.removeCallbacks(updateDurationRunnable);
        if (ringtone != null) {
            ringtone.release();
        }
        if (audioManager != null) {
            AudioUtils.cleanupAfterCall(this);
        }
        if (videoStreamer != null) {
            videoStreamer.releaseCamera();
        }
    }
    
    @Override
    public void onBackPressed() {
        // During an active call, prevent accidental hangup via back button
        if (isCallConnected) {
            Toast.makeText(this, "Use the end call button to hang up", Toast.LENGTH_SHORT).show();
            return;
        }
        // If call is not connected yet, allow normal back behavior
        super.onBackPressed();
    }

    // CallListener interface implementations
    @Override
    public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
        // Not needed in CallActivity - handled by IncomingCallActivity
    }

    @Override
    public void onCallAnswered(String callId, long timestamp) {
        runOnUiThread(() -> {
            connectCall();
        });
    }

    @Override
    public void onCallDeclined(String callId, long timestamp) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Call declined", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallEnded(String callId, long timestamp) {
        runOnUiThread(() -> {
            callDurationHandler.removeCallbacks(updateDurationRunnable);
            
            // Stop ringtone if playing
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                ringtone.release();
            }
            
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallFailed(String callId, String reason) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "[MESSENGER-STYLE] Call failed - CallId: " + callId + ", Reason: " + reason);
            
            // Stop any ongoing ringtone
            if (ringtone != null && ringtone.isPlaying()) {
                Log.d("CallActivity", "[MESSENGER-STYLE] Stopping ringtone due to call failure");
                ringtone.stop();
                ringtone.release();
            }
            
            // Clear any pending timeout handlers
            if (callTimeoutHandler != null) {
                callTimeoutHandler.removeCallbacksAndMessages(null);
                Log.d("CallActivity", "[MESSENGER-STYLE] Cleared timeout handlers due to call failure");
            }
            
            // Show appropriate message based on reason
            String userMessage;
            if (reason.toLowerCase().contains("no answer") || reason.toLowerCase().contains("timeout")) {
                userMessage = "No answer - try again later";
            } else if (reason.toLowerCase().contains("busy")) {
                userMessage = "User is busy";
            } else if (reason.toLowerCase().contains("declined") || reason.toLowerCase().contains("rejected")) {
                userMessage = "Call declined";
            } else if (reason.toLowerCase().contains("offline") || reason.toLowerCase().contains("unavailable")) {
                userMessage = "User unavailable";
            } else {
                userMessage = "Call failed: " + reason;
            }
            
            Log.d("CallActivity", "[MESSENGER-STYLE] Showing user message: " + userMessage);
            Toast.makeText(this, userMessage, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallMuteStatus(String callId, String userId, boolean isMuted) {
        runOnUiThread(() -> {
        });
    }

    @Override
    public void onCallVideoStatus(String callId, String userId, boolean isVideoOn) {
        runOnUiThread(() -> {
        });
    }
    
    @Override
    public void onCallForceTerminated(String callId, String reason) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "🚫 Call force terminated - ID: " + callId + ", Reason: " + reason);
            // Immediately end the call
            endCall();
        });
    }
    
    // VoiceActivityListener implementation
    @Override
    public void onVoiceActivityChanged(boolean isSpeaking) {
        runOnUiThread(() -> {
            if (speakingIndicator != null) {
                if (isSpeaking && !isMuted && isCallConnected) {
                    speakingIndicator.setVisibility(View.VISIBLE);
                    Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.speaking_pulse);
                    speakingIndicator.startAnimation(pulseAnimation);
                } else {
                    speakingIndicator.clearAnimation();
                    speakingIndicator.setVisibility(View.GONE);
                }
            }
        });
    }
    
    @Override
    public void onAudioDataReady(String audioData, String chatId, String userId) {
        SocketManager.emitVoiceData(audioData, chatId, userId);
    }
    
    @Override
    public void onVoiceDataReceived(String audioData, String senderId) {
        String currentUserId = sessionManager.getCurrentUserId();
        if (!senderId.equals(currentUserId) && voiceStreamer != null) {
            voiceStreamer.playReceivedAudio(audioData);
        }
    }
    
    @Override
    public void onVideoDataReceived(String videoData, String senderId) {
        Log.d("CallActivity", "Video data received from: " + senderId + ", Size: " + videoData.length());
        String currentUserId = sessionManager.getCurrentUserId();
        if (!senderId.equals(currentUserId) && videoRenderer != null) {
            Log.d("CallActivity", "Rendering video frame from different user");
            videoRenderer.renderFrame(videoData);
        } else {
            Log.d("CallActivity", "Ignoring own video data or renderer is null");
        }
    }
    
    @Override
    public void onVideoDataReady(String videoData, String chatId, String userId) {
        Log.d("CallActivity", "Video data ready - Chat: " + chatId + ", User: " + userId + ", Size: " + videoData.length());
        SocketManager.emitVideoData(videoData, chatId, userId);
    }
    
    @Override
    public void onCameraReady() {
        runOnUiThread(() -> {
            Log.d("CallActivity", "Camera is ready");
            if (cameraStatusText != null) {
                cameraStatusText.setVisibility(View.GONE);
            }
            // Auto-start video streaming if call is connected and video is on
            if (isCallConnected && isVideoOn && videoStreamer != null) {
                Log.d("CallActivity", "Auto-starting video streaming since call is connected");
                videoStreamer.startStreaming();
            }
        });
    }
    
    @Override
    public void onCameraError(String error) {
        runOnUiThread(() -> {
            Log.e("CallActivity", "Camera error: " + error);
            if (cameraStatusText != null) {
                cameraStatusText.setText("Camera Error");
                cameraStatusText.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "Camera error: " + error, Toast.LENGTH_LONG).show();
        });
    }
}
