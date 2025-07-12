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

public class MenuActivity extends AppCompatActivity {

    ImageView avatarView;
    TextView nameView;
    Button settingsButton;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        avatarView = findViewById(R.id.avatar);
        nameView = findViewById(R.id.name);
        settingsButton = findViewById(R.id.btn_settings);

        prefs = getSharedPreferences("user", MODE_PRIVATE);
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
        String username = prefs.getString("username", "admin");
        String avatarUri = prefs.getString("avatar_uri", null);

        nameView.setText(username);
        if (avatarUri != null) {
            avatarView.setImageURI(Uri.parse(avatarUri));
        } else {
            avatarView.setImageResource(R.drawable.ic_avatar);
        }
    }
}
