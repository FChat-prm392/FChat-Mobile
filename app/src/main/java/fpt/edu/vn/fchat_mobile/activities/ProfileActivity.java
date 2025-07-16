package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository.SimpleCallback;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository.UserCallback;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private TextView fullnameText, usernameText, emailText, statusText;
    private ImageView profileImage;
    private Button addFriendButton, blockButton;
    private FriendRepository friendRepository;
    private String currentUserId;
    private String profileUserId;
    private Account profileAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        fullnameText = findViewById(R.id.profileFullname);
        usernameText = findViewById(R.id.profileUsername);
        emailText = findViewById(R.id.profileEmail);
        profileImage = findViewById(R.id.profileImage);
        statusText = findViewById(R.id.statusText);
        addFriendButton = findViewById(R.id.btnAddFriend);
        blockButton = findViewById(R.id.btnBlock);

        // Initialize repository
        friendRepository = new FriendRepository();

        // Get user ID from intent
        profileUserId = getIntent().getStringExtra("userId");
        currentUserId = getIntent().getStringExtra("currentUserId");
        Log.d(TAG, "Received profileUserId: " + profileUserId + ", currentUserId: " + currentUserId);
        if (profileUserId == null || currentUserId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load profile
        loadProfile();

        // Set button listeners
        addFriendButton.setOnClickListener(v -> sendFriendRequest());
        blockButton.setOnClickListener(v -> blockUser());
    }

    private void loadProfile() {
        statusText.setText("Loading profile...");
        friendRepository.getUserById(profileUserId, new UserCallback() {
            @Override
            public void onSuccess(Account account) {
                runOnUiThread(() -> {
                    profileAccount = account;
                    fullnameText.setText(profileAccount.getFullname());
                    usernameText.setText("@" + profileAccount.getUsername());
                    emailText.setText(profileAccount.getEmail());
                    // Load image using Picasso/Glide if available
                    // Picasso.get().load(profileAccount.getAvatarUrl()).into(profileImage);
                    statusText.setText("");
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    statusText.setText("Failed to load profile: " + t.getMessage());
                    Log.e(TAG, "Load profile failed", t);
                });
            }
        });
    }

    private void sendFriendRequest() {
        Log.d(TAG, "Sending friend request from currentUserId: " + currentUserId + " to profileUserId: " + profileUserId);
        if (profileUserId.equals(currentUserId)) {
            statusText.setText("Cannot add yourself as a friend");
            return;
        }
        addFriendButton.setEnabled(false);
        statusText.setText("Checking status...");
        friendRepository.getUserById(profileUserId, new UserCallback() {
            @Override
            public void onSuccess(Account account) {
                String status = "pending";
                if ("pending".equals(status) || "accepted".equals(status)) {
                    runOnUiThread(() -> {
                        statusText.setText("Friend request already exists");
                        addFriendButton.setEnabled(true);
                    });
                } else {
                    statusText.setText("Sending friend request...");
                    friendRepository.sendFriendRequest(currentUserId, profileUserId, new SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                statusText.setText("Friend request sent");
                                addFriendButton.setText("Pending");
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            runOnUiThread(() -> {
                                String errorMsg = t.getMessage();
                                if (errorMsg != null && errorMsg.contains("Friend request already exists")) {
                                    statusText.setText("Friend request already exists");
                                } else {
                                    statusText.setText("Failed to send request: " + (errorMsg != null ? errorMsg : "Unknown error"));
                                }
                                addFriendButton.setEnabled(true);
                            });
                            Log.e(TAG, "Send friend request failed", t);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    statusText.setText("Failed to check status: " + t.getMessage());
                    addFriendButton.setEnabled(true);
                });
                Log.e(TAG, "Check status failed", t);
            }
        });
    }

    private void blockUser() {
        if (profileUserId.equals(currentUserId)) {
            statusText.setText("Cannot block yourself");
            return;
        }
        blockButton.setEnabled(false);
        statusText.setText("Blocking user...");
        friendRepository.blockUser(currentUserId, profileUserId, new SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText("User blocked");
                    addFriendButton.setVisibility(View.GONE);
                    blockButton.setText("Unblock");
                    blockButton.setEnabled(true);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    statusText.setText("Failed to block: " + t.getMessage());
                    blockButton.setEnabled(true);
                });
                Log.e(TAG, "Block user failed", t);
            }
        });
    }

    public void unblockUser() {
        blockButton.setEnabled(false);
        statusText.setText("Unblocking user...");
        friendRepository.unblockUser(currentUserId, profileUserId, new SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText("User unblocked");
                    blockButton.setText("Block");
                    addFriendButton.setVisibility(View.VISIBLE);
                    blockButton.setEnabled(true);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    statusText.setText("Failed to unblock: " + t.getMessage());
                    blockButton.setEnabled(true);
                });
                Log.e(TAG, "Unblock user failed", t);
            }
        });
    }
}