package fpt.edu.vn.fchat_mobile.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;

public class IncomingCallActivity extends AppCompatActivity {

    private ImageView callerAvatar, backgroundBlur, callTypeIcon, answerIcon;
    private TextView callerName, callType, callStatus;
    private View ripple1, ripple2, ripple3;
    private RelativeLayout btnAnswer, btnDecline;
    private LinearLayout btnMute, btnMessage;
    
    private String callId;
    private String chatId;
    private String callerId;
    private String callerNameStr;
    private String avatarUrl;
    private boolean isVideoCall;
    private long timestamp;
    
    private MediaPlayer ringtone;
    private SessionManager sessionManager;
    private Handler animationHandler;
    private Runnable rippleRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make it a full screen activity like Messenger
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        setContentView(R.layout.activity_incoming_call);
        
        sessionManager = new SessionManager(this);
        animationHandler = new Handler();
        
        // Get intent data
        Intent intent = getIntent();
        callId = intent.getStringExtra("callId");
        chatId = intent.getStringExtra("chatId");
        callerId = intent.getStringExtra("callerId");
        callerNameStr = intent.getStringExtra("callerName");
        avatarUrl = intent.getStringExtra("avatarUrl");
        isVideoCall = intent.getBooleanExtra("isVideoCall", false);
        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        
        initViews();
        setupUI();
        setupClickListeners();
        startRippleAnimation();
        startRingtone();
    }
    
    private void initViews() {
        callerAvatar = findViewById(R.id.caller_avatar);
        backgroundBlur = findViewById(R.id.background_blur);
        callerName = findViewById(R.id.caller_name);
        callType = findViewById(R.id.call_type);
        callStatus = findViewById(R.id.call_status);
        callTypeIcon = findViewById(R.id.call_type_icon);
        answerIcon = findViewById(R.id.answer_icon);
        
        ripple1 = findViewById(R.id.ripple_1);
        ripple2 = findViewById(R.id.ripple_2);
        ripple3 = findViewById(R.id.ripple_3);
        
        btnAnswer = findViewById(R.id.btn_answer);
        btnDecline = findViewById(R.id.btn_decline);
        btnMute = findViewById(R.id.btn_mute);
        btnMessage = findViewById(R.id.btn_message);
    }
    
    private void setupUI() {
        callerName.setText(callerNameStr != null ? callerNameStr : "Unknown Caller");
        
        if (isVideoCall) {
            callType.setText("Video call");
            callTypeIcon.setImageResource(R.drawable.ic_video_call);
            answerIcon.setImageResource(R.drawable.ic_video_call);
        } else {
            callType.setText("Voice call");
            callTypeIcon.setImageResource(R.drawable.ic_call);
            answerIcon.setImageResource(R.drawable.ic_call);
        }
        
        // Load caller avatar with Glide
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(callerAvatar);
                    
            // Also set blurred background
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(backgroundBlur);
        } else {
            callerAvatar.setImageResource(R.drawable.ic_avatar);
            backgroundBlur.setImageResource(R.drawable.ic_avatar);
        }
    }
    
    private void startRippleAnimation() {
        rippleRunnable = new Runnable() {
            @Override
            public void run() {
                // Animate ripple effects
                animateRipple(ripple1, 0);
                animateRipple(ripple2, 200);
                animateRipple(ripple3, 400);
                
                // Repeat animation
                animationHandler.postDelayed(this, 2000);
            }
        };
        animationHandler.post(rippleRunnable);
    }
    
    private void animateRipple(View view, long delay) {
        animationHandler.postDelayed(() -> {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.5f, 0.2f, 0.5f);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY, alpha);
            animatorSet.setDuration(1500);
            animatorSet.start();
        }, delay);
    }
    
    private void setupClickListeners() {
        btnAnswer.setOnClickListener(v -> answerCall());
        btnDecline.setOnClickListener(v -> declineCall());
        btnMute.setOnClickListener(v -> answerCallMuted());
        btnMessage.setOnClickListener(v -> sendQuickMessage());
        
        // Add button press animations
        setupButtonAnimation(btnAnswer);
        setupButtonAnimation(btnDecline);
        setupButtonAnimation(btnMute);
        setupButtonAnimation(btnMessage);
    }
    
    private void setupButtonAnimation(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
    
    private void startRingtone() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = MediaPlayer.create(this, ringtoneUri);
            if (ringtone != null) {
                ringtone.setLooping(true);
                ringtone.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopRingtone() {
        if (ringtone != null) {
            try {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
                ringtone.release();
                ringtone = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void stopAnimations() {
        if (animationHandler != null && rippleRunnable != null) {
            animationHandler.removeCallbacks(rippleRunnable);
        }
    }
    
    private void answerCall() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Emit call answer
        SocketManager.emitCallAnswer(callId, callerId, currentUserId);
        
        // Start CallActivity
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", callerNameStr);
        callIntent.putExtra("participantId", callerId);
        callIntent.putExtra("avatarUrl", avatarUrl);
        callIntent.putExtra("isVideoCall", isVideoCall);
        callIntent.putExtra("isIncomingCall", true);
        callIntent.putExtra("callId", callId);
        startActivity(callIntent);
        
        // Add smooth transition
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void answerCallMuted() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Emit call answer
        SocketManager.emitCallAnswer(callId, callerId, currentUserId);
        
        // Start CallActivity with muted state
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", callerNameStr);
        callIntent.putExtra("participantId", callerId);
        callIntent.putExtra("avatarUrl", avatarUrl);
        callIntent.putExtra("isVideoCall", isVideoCall);
        callIntent.putExtra("isIncomingCall", true);
        callIntent.putExtra("callId", callId);
        callIntent.putExtra("startMuted", true); // Start with muted microphone
        startActivity(callIntent);
        
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void sendQuickMessage() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Decline the call first
        SocketManager.emitCallDecline(callId, callerId, currentUserId);
        
        // Open chat with the caller
        Intent chatIntent = new Intent(this, ChatDetailActivity.class);
        chatIntent.putExtra("chatId", chatId);
        chatIntent.putExtra("participantName", callerNameStr);
        chatIntent.putExtra("participantId", callerId);
        chatIntent.putExtra("avatarUrl", avatarUrl);
        startActivity(chatIntent);
        
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void declineCall() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Emit call decline
        SocketManager.emitCallDecline(callId, callerId, currentUserId);
        
        // Add decline animation
        btnDecline.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(0.5f)
                .setDuration(200)
                .withEndAction(() -> {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                })
                .start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        stopAnimations();
    }
    
    @Override
    public void onBackPressed() {
        // In Messenger style, back button declines the call
        declineCall();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Don't pause the activity - keep it running like Messenger
    }
}
