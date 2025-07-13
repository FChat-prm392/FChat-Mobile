package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;

public class CallActivity extends AppCompatActivity implements SocketManager.CallListener {

    private ImageView avatarView, btnEndCall, btnMute, btnSpeaker, btnVideo;
    private TextView nameText, statusText, callDurationText;
    private View videoContainer;
    
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
    
    private MediaPlayer ringtone;
    private Handler callDurationHandler = new Handler();
    private long callStartTime;
    private SessionManager sessionManager;
    
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
        
        // Get intent data
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId");
        participantName = intent.getStringExtra("participantName");
        participantId = intent.getStringExtra("participantId");
        avatarUrl = intent.getStringExtra("avatarUrl");
        isVideoCall = intent.getBooleanExtra("isVideoCall", false);
        isIncomingCall = intent.getBooleanExtra("isIncomingCall", false);
        boolean startMuted = intent.getBooleanExtra("startMuted", false);
        boolean isCallAnswered = intent.getBooleanExtra("isCallAnswered", false);
        
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
    
    private void initViews() {
        avatarView = findViewById(R.id.call_avatar);
        nameText = findViewById(R.id.call_name);
        statusText = findViewById(R.id.call_status);
        callDurationText = findViewById(R.id.call_duration);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnMute = findViewById(R.id.btn_mute);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnVideo = findViewById(R.id.btn_video);
        videoContainer = findViewById(R.id.video_container);
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
            videoContainer.setVisibility(View.VISIBLE);
            avatarView.setVisibility(View.GONE);
        } else {
            btnVideo.setVisibility(View.GONE);
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
    }
    
    private void setupCallListeners() {
        // Setup SocketManager call listeners to handle call-ended events
        SocketManager.setupCallListeners(this);
    }
    
    private void handleIncomingCall() {
        // Show incoming call UI
        statusText.setText("Incoming " + (isVideoCall ? "video" : "voice") + " call...");
        
        // Wait for user to manually answer the call
        // No auto-answer - user must use IncomingCallActivity to accept
    }
    
    private void initiateCall() {
        statusText.setText("Calling...");
        
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserName = sessionManager.getCurrentUserFullname();
        
        // Emit call initiation using SocketManager
        SocketManager.emitCallInitiate(callId, chatId, currentUserId, participantId, currentUserName, isVideoCall);
        
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
        
        // Start duration timer
        callDurationHandler.postDelayed(updateDurationRunnable, 1000);
        
        Toast.makeText(this, "Call connected", Toast.LENGTH_SHORT).show();
    }
    
    private void endCall() {
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Emit call end using SocketManager
        SocketManager.emitCallEnd(callId, currentUserId, participantId);
        
        // Stop duration timer
        callDurationHandler.removeCallbacks(updateDurationRunnable);
        
        // Stop ringtone if playing
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone.release();
        }
        
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        updateMuteButtonUI();
        
        // Emit mute status using SocketManager
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
        Toast.makeText(this, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleVideo() {
        if (!isVideoCall) return;
        
        isVideoOn = !isVideoOn;
        btnVideo.setImageResource(isVideoOn ? R.drawable.ic_video : R.drawable.ic_video_off);
        btnVideo.setColorFilter(isVideoOn ? getColor(R.color.blue) : getColor(R.color.red));
        
        if (isVideoOn) {
            videoContainer.setVisibility(View.VISIBLE);
            avatarView.setVisibility(View.GONE);
        } else {
            videoContainer.setVisibility(View.GONE);
            avatarView.setVisibility(View.VISIBLE);
        }
        
        // Emit video status using SocketManager
        String callId = chatId + "_" + System.currentTimeMillis();
        String currentUserId = sessionManager.getCurrentUserId();
        SocketManager.emitCallVideoToggle(callId, currentUserId, participantId, isVideoOn);
        
        Toast.makeText(this, isVideoOn ? "Video on" : "Video off", Toast.LENGTH_SHORT).show();
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
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button during call - user must use end call button
        Toast.makeText(this, "Use the end call button to hang up", Toast.LENGTH_SHORT).show();
    }

    // CallListener interface implementations
    @Override
    public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
        // Not needed in CallActivity - handled by IncomingCallActivity
    }

    @Override
    public void onCallAnswered(String callId, long timestamp) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "Call answered: " + callId);
            connectCall();
        });
    }

    @Override
    public void onCallDeclined(String callId, long timestamp) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "Call declined: " + callId);
            Toast.makeText(this, "Call declined", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallEnded(String callId, long timestamp) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "Call ended by other participant: " + callId);
            
            // Stop duration timer
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
            Log.d("CallActivity", "Call failed: " + callId + ", reason: " + reason);
            Toast.makeText(this, "Call failed: " + reason, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallMuteStatus(String callId, String userId, boolean isMuted) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "User " + userId + " mute status: " + isMuted);
            // Update UI to show other participant's mute status if needed
        });
    }

    @Override
    public void onCallVideoStatus(String callId, String userId, boolean isVideoOn) {
        runOnUiThread(() -> {
            Log.d("CallActivity", "User " + userId + " video status: " + isVideoOn);
            // Update UI to show other participant's video status if needed
        });
    }
}
