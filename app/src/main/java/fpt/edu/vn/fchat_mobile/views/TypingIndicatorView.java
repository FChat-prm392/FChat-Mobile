package fpt.edu.vn.fchat_mobile.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import fpt.edu.vn.fchat_mobile.R;

public class TypingIndicatorView extends LinearLayout {
    
    private TextView userNameText;
    private View dot1, dot2, dot3;
    private ObjectAnimator dot1Animator, dot2Animator, dot3Animator;
    private boolean isAnimating = false;
    
    public TypingIndicatorView(Context context) {
        super(context);
        init();
    }
    
    public TypingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public TypingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.typing_indicator, this, true);
        
        userNameText = findViewById(R.id.typing_user_name);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        
        setupAnimations();
    }
    
    private void setupAnimations() {
        // Create bouncing animations for each dot
        dot1Animator = ObjectAnimator.ofFloat(dot1, "translationY", 0f, -20f, 0f);
        dot1Animator.setDuration(600);
        dot1Animator.setRepeatCount(ObjectAnimator.INFINITE);
        
        dot2Animator = ObjectAnimator.ofFloat(dot2, "translationY", 0f, -20f, 0f);
        dot2Animator.setDuration(600);
        dot2Animator.setRepeatCount(ObjectAnimator.INFINITE);
        dot2Animator.setStartDelay(200);
        
        dot3Animator = ObjectAnimator.ofFloat(dot3, "translationY", 0f, -20f, 0f);
        dot3Animator.setDuration(600);
        dot3Animator.setRepeatCount(ObjectAnimator.INFINITE);
        dot3Animator.setStartDelay(400);
    }
    
    public void showTyping(String userName) {
        userNameText.setText(userName + " is typing");
        setVisibility(VISIBLE);
        startAnimation();
    }
    
    public void hideTyping() {
        setVisibility(GONE);
        stopAnimation();
    }
    
    private void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            dot1Animator.start();
            dot2Animator.start();
            dot3Animator.start();
        }
    }
    
    private void stopAnimation() {
        if (isAnimating) {
            isAnimating = false;
            dot1Animator.cancel();
            dot2Animator.cancel();
            dot3Animator.cancel();
            
            // Reset dot positions
            dot1.setTranslationY(0f);
            dot2.setTranslationY(0f);
            dot3.setTranslationY(0f);
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
