package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;

public class MenuActivity extends AppCompatActivity {

    ImageView avatarView;
    TextView nameView;
    Button settingsButton;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        avatarView = findViewById(R.id.avatar);
        nameView = findViewById(R.id.name);
        settingsButton = findViewById(R.id.btn_settings);
        
        sessionManager = new SessionManager(this);
        
        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        
        // Check if user is logged in
        if (!sessionManager.hasValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadUserData();

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        String fullname = sessionManager.getCurrentUserFullname();
        String username = sessionManager.getCurrentUserUsername();
        
        // Use fullname if available, otherwise use username
        String displayName = (fullname != null && !fullname.isEmpty()) ? fullname : username;
        if (displayName == null) {
            displayName = "User";
        }
        
        nameView.setText(displayName);
        
        // You can add avatar loading logic here if you store avatar URLs in session
        // For now, keep the default avatar
    }
}
