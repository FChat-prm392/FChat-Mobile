package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.MessageAdapter;
import fpt.edu.vn.fchat_mobile.items.MessageItem;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.requests.SendMessageRequest;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.responses.SendMessageResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private ImageView avatarView, btnSend, btnMic, btnCamera;
    private TextView nameText, statusText;
    private EditText editMessage;

    private final List<MessageItem> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String chatId;
    private SessionManager sessionManager;
    private static final String TAG = "ChatDetailActivity";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    messageList.add(new MessageItem(photo));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.hasValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Debug session info
        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserName = sessionManager.getCurrentUserUsername();
        Log.d(TAG, "Session info - User ID: '" + currentUserId + "', Username: '" + currentUserName + "'");

        avatarView = findViewById(R.id.avatar);
        nameText = findViewById(R.id.name);
        statusText = findViewById(R.id.status);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);
        btnCamera = findViewById(R.id.btn_camera);

        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId");
        String name = intent.getStringExtra("name");
        String status = intent.getStringExtra("status");
        String avatarUrl = intent.getStringExtra("avatarUrl");

        nameText.setText(name);
        statusText.setText(status);

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .into(avatarView);

        RecyclerView recyclerView = findViewById(R.id.message_list);
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchMessages(chatId);

        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            Log.d(TAG, "Send button clicked, message content: '" + content + "'");
            if (!content.isEmpty()) {
                sendMessage(content);
            } else {
                Log.w(TAG, "Attempted to send empty message");
            }
        });

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                openCamera();
            }
        });

        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    btnSend.setVisibility(ImageView.GONE);
                    btnMic.setVisibility(ImageView.VISIBLE);
                } else {
                    btnSend.setVisibility(ImageView.VISIBLE);
                    btnMic.setVisibility(ImageView.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchMessages(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Cannot fetch messages: chatId is null or empty");
            return;
        }

        Log.d(TAG, "Fetching messages for chat ID: " + chatId);

        ApiService apiService = ApiClient.getService();
        retrofit2.Call<List<MessageResponse>> call = apiService.getMessagesByChatId(chatId, 100); // limit to 100 messages
        call.enqueue(new retrofit2.Callback<List<MessageResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<MessageResponse>> call, retrofit2.Response<List<MessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MessageResponse> messages = response.body();
                    Log.d(TAG, "Received " + messages.size() + " messages");
                    
                    // Sort messages by creation time (oldest first)
                    messages.sort((msg1, msg2) -> {
                        try {
                            String time1 = msg1.getCreateAt();
                            String time2 = msg2.getCreateAt();
                            
                            if (time1 == null && time2 == null) return 0;
                            if (time1 == null) return -1;
                            if (time2 == null) return 1;
                            
                            // Try to parse as ISO date first (most common API format)
                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            try {
                                Date date1 = isoFormat.parse(time1);
                                Date date2 = isoFormat.parse(time2);
                                return date1.compareTo(date2);
                            } catch (Exception e) {
                                // If ISO parsing fails, try simple string comparison
                                Log.d(TAG, "Date parsing failed, using string comparison: " + e.getMessage());
                                return time1.compareTo(time2);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sorting messages by date", e);
                            return 0;
                        }
                    });

                    // Update message list with proper sender identification
                    messageList.clear();
                    String currentUserId = sessionManager.getCurrentUserId();
                    Log.d(TAG, "Current user ID for comparison: " + currentUserId);
                    
                    for (MessageResponse msg : messages) {
                        // Check if I'm the sender by comparing IDs properly
                        String senderId = "";
                        if (msg.getSenderID() != null) {
                            if (msg.getSenderID().getId() != null) {
                                senderId = msg.getSenderID().getId();
                            }
                            Log.d(TAG, "Sender info - ID: '" + senderId + "', Username: '" + msg.getSenderID().getUsername() + "', Fullname: '" + msg.getSenderID().getFullname() + "'");
                        } else {
                            Log.w(TAG, "SenderID is null for message: " + msg.getText());
                        }
                        
                        boolean isMine = senderId.equals(currentUserId);
                        Log.d(TAG, "Message: '" + msg.getText() + "' | Sender ID: '" + senderId + "' | Current User ID: '" + currentUserId + "' | IDs equal: " + senderId.equals(currentUserId) + " | Is mine: " + isMine);
                        
                        // Create MessageItem from MessageResponse
                        MessageItem messageItem = new MessageItem(msg.getText(), isMine);
                        messageList.add(messageItem);
                    }
                    
                    messageAdapter.notifyDataSetChanged();
                    
                    // Scroll to bottom to show latest messages
                    RecyclerView recyclerView = findViewById(R.id.message_list);
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    
                    Log.d(TAG, "Successfully loaded and sorted " + messageList.size() + " messages");
                } else {
                    Log.e(TAG, "Failed to fetch messages: " + response.code() + " " + response.message());
                    Toast.makeText(ChatDetailActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<MessageResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching messages: " + t.getMessage(), t);
                Toast.makeText(ChatDetailActivity.this, "Error loading messages: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String content) {
        Log.d(TAG, "Preparing to send message: '" + content + "'");
        
        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Cannot send message: chatId is null or empty");
            Toast.makeText(this, "Error: Chat not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getCurrentUserId();
        Log.d(TAG, "Preparing to send message - Chat ID: " + chatId + ", User ID: " + userId + ", Content: " + content);

        SendMessageRequest request = new SendMessageRequest(userId, chatId, content);
        Log.d(TAG, "SendMessageRequest created: " + request.toString());

        ApiService apiService = ApiClient.getService();
        retrofit2.Call<SendMessageResponse> call = apiService.sendMessage(request);
        call.enqueue(new retrofit2.Callback<SendMessageResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SendMessageResponse> call, retrofit2.Response<SendMessageResponse> response) {
                Log.d(TAG, "Send message response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    SendMessageResponse messageResponse = response.body();
                    Log.d(TAG, "Message sent successfully: " + messageResponse.toString());
                    
                    // Clear the input field on UI thread
                    runOnUiThread(() -> {
                        editMessage.setText("");
                        Log.d(TAG, "Input field cleared");
                    });
                    
                    // Refresh the message list after a short delay to ensure server has processed
                    new android.os.Handler().postDelayed(() -> {
                        Log.d(TAG, "Refreshing messages after successful send");
                        fetchMessages(chatId);
                    }, 500);
                    
                    Toast.makeText(ChatDetailActivity.this, "Message sent!", Toast.LENGTH_SHORT).show();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    
                    Log.e(TAG, "Failed to send message: " + response.code() + " " + response.message() + ", Error body: " + errorBody);
                    Toast.makeText(ChatDetailActivity.this, "Failed to send message: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SendMessageResponse> call, Throwable t) {
                Log.e(TAG, "Error sending message: " + t.getMessage(), t);
                Toast.makeText(ChatDetailActivity.this, "Error sending message: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
