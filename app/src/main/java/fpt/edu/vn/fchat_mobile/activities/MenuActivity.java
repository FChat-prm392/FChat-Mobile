package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;

public class MenuActivity extends AppCompatActivity {

    private ImageView avatarView;
    private TextView nameView;
    private Button settingsButton, logoutButton, btnFriendList, btnAddFriend, btnChatList, btnToggleTheme, btnFriendRequests, btnBlockList, btnAIChat;

    private SessionManager sessionManager;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        avatarView = findViewById(R.id.avatar);
        nameView = findViewById(R.id.name);
        settingsButton = findViewById(R.id.btn_settings);
        logoutButton = findViewById(R.id.btn_logout);
        btnFriendList = findViewById(R.id.btn_friend_list);
        btnAddFriend = findViewById(R.id.btn_add_friend);
        btnChatList = findViewById(R.id.btn_chat_list);
        btnToggleTheme = findViewById(R.id.btn_toggle_theme);
        btnFriendRequests = findViewById(R.id.btn_friend_requests);
        btnBlockList = findViewById(R.id.btn_block_list);
        btnAIChat = findViewById(R.id.btn_ai_chat);

        sessionManager = new SessionManager(this);

        if (!sessionManager.hasValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadUserData();

        logoutButton.setOnClickListener(v -> {
            String userId = sessionManager.getCurrentUserId();
            if (userId != null) {
                SocketManager.initializeSocket();
                SocketManager.emitUserLogout(userId);
            }
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        btnFriendList.setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendListActivity.class);
            startActivity(intent);
        });

        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFriendActivity.class);
            startActivity(intent);
        });

        btnChatList.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatListActivity.class);
            startActivity(intent);
        });

        btnToggleTheme.setOnClickListener(v -> {
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                btnToggleTheme.setText("Chế độ hệ thống: Sáng");
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                btnToggleTheme.setText("Chế độ hệ thống: Tối");
            }
            isDarkMode = !isDarkMode;
        });

        btnFriendRequests.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFriendRequestActivity.class);
            startActivity(intent);
        });

        btnBlockList.setOnClickListener(v -> {
            Intent intent = new Intent(this, BlockListActivity.class);
            startActivity(intent);
        });

        btnAIChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, AIChatActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        String name = sessionManager.getCurrentUserFullname();
        String avatarUrl = sessionManager.getCurrentUserAvatarUrl();

        if (name != null && !name.isEmpty()) {
            nameView.setText(name);
        }

        if (avatarUrl != null && !avatarUrl.equals("N/A")) {
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .into(avatarView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
}