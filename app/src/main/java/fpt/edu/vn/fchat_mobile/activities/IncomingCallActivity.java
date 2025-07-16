package fpt.edu.vn.fchat_mobile.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

public class IncomingCallActivity extends AppCompatActivity implements SocketManager.CallListener {

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
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        setContentView(R.layout.activity_incoming_call);
        
        sessionManager = new SessionManager(this);
        animationHandler = new Handler();
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
        setupCallListeners();
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
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(callerAvatar);
                    
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
                animateRipple(ripple1, 0);
                animateRipple(ripple2, 200);
                animateRipple(ripple3, 400);
                
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
    
    private void stopRippleAnimation() {
        if (animationHandler != null && rippleRunnable != null) {
            animationHandler.removeCallbacks(rippleRunnable);
        }
        
        if (ripple1 != null) {
            ripple1.clearAnimation();
            ripple1.animate().cancel();
        }
        if (ripple2 != null) {
            ripple2.clearAnimation();
            ripple2.animate().cancel();
        }
        if (ripple3 != null) {
            ripple3.clearAnimation();
            ripple3.animate().cancel();
        }
    }
    
    private void setupClickListeners() {
        btnAnswer.setOnClickListener(v -> answerCall());
        btnDecline.setOnClickListener(v -> declineCall());
        btnMute.setOnClickListener(v -> answerCallMuted());
        btnMessage.setOnClickListener(v -> sendQuickMessage());
        
        setupButtonAnimation(btnAnswer);
        setupButtonAnimation(btnDecline);
        setupButtonAnimation(btnMute);
        setupButtonAnimation(btnMessage);
    }
    
    private void setupCallListeners() {
        SocketManager.setupCallListeners(this);
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
        
        SocketManager.emitCallAnswer(callId, callerId, currentUserId);
        
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", callerNameStr);
        callIntent.putExtra("participantId", callerId);
        callIntent.putExtra("avatarUrl", avatarUrl);
        callIntent.putExtra("isVideoCall", isVideoCall);
        callIntent.putExtra("isIncomingCall", true);
        callIntent.putExtra("isCallAnswered", true);
        callIntent.putExtra("callId", callId);
        startActivity(callIntent);
        
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void answerCallMuted() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        SocketManager.emitCallAnswer(callId, callerId, currentUserId);
        
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", callerNameStr);
        callIntent.putExtra("participantId", callerId);
        callIntent.putExtra("avatarUrl", avatarUrl);
        callIntent.putExtra("isVideoCall", isVideoCall);
        callIntent.putExtra("isIncomingCall", true);
        callIntent.putExtra("isCallAnswered", true);
        callIntent.putExtra("callId", callId);
        callIntent.putExtra("startMuted", true);
        startActivity(callIntent);
        
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void sendQuickMessage() {
        stopRingtone();
        stopAnimations();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        SocketManager.emitCallDecline(callId, callerId, currentUserId);
        
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
        
        SocketManager.emitCallDecline(callId, callerId, currentUserId);
        
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
        declineCall();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
    }

    @Override
    public void onCallAnswered(String callId, long timestamp) {
    }

    @Override
    public void onCallDeclined(String callId, long timestamp) {
        runOnUiThread(() -> {
            stopRingtone();
            stopRippleAnimation();
            finish();
        });
    }

    @Override
    public void onCallEnded(String callId, long timestamp) {
        runOnUiThread(() -> {
            stopRingtone();
            stopRippleAnimation();
            finish();
        });
    }

    @Override
    public void onCallFailed(String callId, String reason) {
        runOnUiThread(() -> {
            stopRingtone();
            stopRippleAnimation();
            finish();
        });
    }

    @Override
    public void onCallMuteStatus(String callId, String userId, boolean isMuted) {
    }

    @Override
    public void onCallVideoStatus(String callId, String userId, boolean isVideoOn) {
    }
    
    @Override
    public void onVoiceDataReceived(String audioData, String senderId) {
    }
    
    @Override
    public void onVideoDataReceived(String videoData, String senderId) {
    }
}
