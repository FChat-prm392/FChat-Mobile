package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
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
import fpt.edu.vn.fchat_mobile.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity implements SocketManager.MessageStatusListener {

    private ImageView avatarView, btnSend, btnMic, btnCamera;
    private TextView nameText, statusText, typingIndicator;
    private EditText editMessage;

    private final List<MessageItem> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String chatId;
    private SessionManager sessionManager;
    private static final String TAG = "ChatDetailActivity";

    // Typing functionality
    private Handler typingHandler = new Handler();
    private Runnable typingRunnable;
    private boolean isTyping = false;

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

        // Initialize socket and setup listeners
        SocketManager.initializeSocket();
        SocketManager.setupMessageStatusListeners(this);
        
        // Join the chat room for real-time updates
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            Log.d(TAG, "🏠 JOINED CHAT ROOM: " + chatId);
        }
        
        // Register user for online status
         currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId != null) {
            SocketManager.registerUser(currentUserId);
            Log.d(TAG, "👤 REGISTERED USER: " + currentUserId);
        }
        
        // Notify that user entered chat
        if (chatId != null && currentUserId != null) {
            SocketManager.emitUserEnteredChat(chatId, currentUserId);
            Log.d(TAG, "🚪 USER ENTERED CHAT: " + chatId);
        }

        avatarView = findViewById(R.id.avatar);
        nameText = findViewById(R.id.name);
        statusText = findViewById(R.id.status);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);
        btnCamera = findViewById(R.id.btn_camera);
        
        // Initialize typing indicator if it exists in layout
        typingIndicator = findViewById(R.id.status); // Use existing status field as typing indicator

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
            Log.d(TAG, "📤 SEND BUTTON CLICKED - Message: '" + content + "'");
            if (!content.isEmpty()) {
                sendMessage(content);
                editMessage.setText("");
                
                // Stop typing when message is sent
                if (isTyping) {
                    stopTyping();
                }
            } else {
                Log.w(TAG, "Attempted to send empty message");
            }
        });
        
        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            Log.d(TAG, "📤 SEND BUTTON CLICKED - Message: '" + content + "'");
            
            if (!content.isEmpty()) {
                sendMessage(content);
                editMessage.setText("");
                
                // Stop typing when message is sent
                if (isTyping) {
                    stopTyping();
                }
            } else {
                Log.w(TAG, "Attempted to send empty message");
            }
        });
        
        // DEBUG: Long press send button to test status update
        btnSend.setOnLongClickListener(v -> {
            testStatusUpdate();
            return true;
        });
        
        // Add typing indicator for text input
        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && !isTyping) {
                    startTyping();
                } else if (s.length() == 0 && isTyping) {
                    stopTyping();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Debug socket connection
        Log.d(TAG, "🔌 SOCKET CONNECTION STATUS: " + (SocketManager.getSocket() != null && SocketManager.getSocket().connected()));
        
        // Mark all unread messages as read when user enters/returns to the chat
        markAllMessagesAsRead();
    }
    
    /**
     * DEBUG: Test status update functionality
     */
    private void testStatusUpdate() {
        Log.d(TAG, "🧪 TESTING STATUS UPDATE");
        
        // Test 1: Show current messages
        Log.d(TAG, "🧪 Current messages count: " + messageList.size());
        int userMessages = 0;
        int otherMessages = 0;
        
        for (int i = 0; i < messageList.size(); i++) {
            MessageItem msg = messageList.get(i);
            Log.d(TAG, String.format("🧪 Message %d: ID=%s, isSentByUser=%s, status=%s, content='%s'", 
                i, msg.getMessageId(), msg.isSentByUser(), msg.getStatus(), 
                msg.getContent() != null ? msg.getContent().substring(0, Math.min(20, msg.getContent().length())) : "null"));
            
            if (msg.isSentByUser()) {
                userMessages++;
            } else {
                otherMessages++;
            }
        }
        
        Log.d(TAG, String.format("🧪 Message breakdown: %d user messages, %d other messages", userMessages, otherMessages));
        
        // Test 2: Try to find any message to update (even user messages for testing)
        MessageItem testMessage = null;
        for (MessageItem msg : messageList) {
            if (msg.getMessageId() != null && !"read".equals(msg.getStatus())) {
                testMessage = msg;
                break;
            }
        }
        
        if (testMessage != null) {
            Log.d(TAG, "🧪 Found test message: " + testMessage.getMessageId() + " with status: " + testMessage.getStatus());
            String oldStatus = testMessage.getStatus();
            testMessage.setStatus("read");
            runOnUiThread(() -> {
                messageAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Test: Updated " + oldStatus + " → read", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.d(TAG, "🧪 No messages with messageId found or all already read");
            Toast.makeText(this, "All messages are read or missing IDs", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllMessagesAsRead() {
        if (chatId != null && messageList != null && !messageList.isEmpty()) {
            String currentUserId = sessionManager.getCurrentUserId();
            Log.d(TAG, "👁️ MARKING MESSAGES AS READ - Chat: " + chatId + ", User: " + currentUserId);
            Log.d(TAG, "📋 TOTAL MESSAGES IN LIST: " + messageList.size());
            
            int markedCount = 0;
            for (int i = 0; i < messageList.size(); i++) {
                MessageItem message = messageList.get(i);
                Log.d(TAG, "🔍 Message " + i + " - ID: " + message.getMessageId() + 
                           ", IsMine: " + message.isSentByUser() + 
                           ", Status: " + message.getStatus() + 
                           ", Content: '" + message.getContent() + "'");
                
                // Only mark messages that are not mine and not already read
                if (!message.isSentByUser() && message.getMessageId() != null && !"read".equals(message.getStatus())) {
                    SocketManager.emitMessageRead(message.getMessageId(), chatId, currentUserId);
                    message.setStatus("read"); // Update local status
                    markedCount++;
                    Log.d(TAG, "📖 MARKED AS READ - ID: " + message.getMessageId());
                } else {
                    String reason = "";
                    if (message.isSentByUser()) reason = "is my own message";
                    else if (message.getMessageId() == null) reason = "no messageId";
                    else if ("read".equals(message.getStatus())) reason = "already read";
                    Log.d(TAG, "⏭️ SKIPPED - " + reason);
                }
            }
            
            Log.d(TAG, "✅ MARKED " + markedCount + " MESSAGES AS READ");
            
            // Update UI
            if (markedCount > 0 && messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
            }
        } else {
            Log.w(TAG, "❌ CANNOT MARK AS READ - chatId: " + chatId + ", messageList: " + 
                       (messageList == null ? "null" : "size=" + messageList.size()));
        }
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

                    messages.sort((msg1, msg2) -> {
                        try {
                            String time1 = msg1.getCreateAt();
                            String time2 = msg2.getCreateAt();
                            
                            if (time1 == null && time2 == null) return 0;
                            if (time1 == null) return -1;
                            if (time2 == null) return 1;

                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            try {
                                Date date1 = isoFormat.parse(time1);
                                Date date2 = isoFormat.parse(time2);
                                return date1.compareTo(date2);
                            } catch (Exception e) {
                                Log.d(TAG, "Date parsing failed, using string comparison: " + e.getMessage());
                                return time1.compareTo(time2);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sorting messages by date", e);
                            return 0;
                        }
                    });

                    messageList.clear();
                    String currentUserId = sessionManager.getCurrentUserId();
                    Log.d(TAG, "Current user ID for comparison: " + currentUserId);
                    
                    for (MessageResponse msg : messages) {
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

                        // Format timestamp from server
                        String formattedTime = formatMessageTime(msg.getCreateAt());
                        MessageItem messageItem = new MessageItem(msg.getText(), isMine, msg.getId(), formattedTime);
                        messageList.add(messageItem);
                        
                        // Emit message delivered for messages that are not mine
                        if (!isMine && msg.getId() != null) {
                            Log.d(TAG, "📬 EMITTING MESSAGE-DELIVERED for message: " + msg.getId());
                            SocketManager.emitMessageDelivered(msg.getId(), chatId, currentUserId);
                        }
                        
                        // Mark messages as read when viewing them
                        if (!isMine && msg.getId() != null) {
                            Log.d(TAG, "📖 EMITTING MESSAGE-READ for message: " + msg.getId());
                            SocketManager.emitMessageRead(msg.getId(), chatId, currentUserId);
                            Log.d(TAG, "👁️ MARKED MESSAGE AS READ - ID: " + msg.getId());
                        }
                    }
                    
                    messageAdapter.notifyDataSetChanged();

                    RecyclerView recyclerView = findViewById(R.id.message_list);
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    
                    Log.d(TAG, "Successfully loaded and sorted " + messageList.size() + " messages");
                    
                    // Mark messages as read after loading them
                    markAllMessagesAsRead();
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
                Log.d(TAG, "📋 RAW RESPONSE HEADERS: " + response.headers().toString());
                
                // Log raw response body if possible
                try {
                    if (response.raw() != null && response.raw().request() != null) {
                        Log.d(TAG, "📋 REQUEST URL: " + response.raw().request().url().toString());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "📋 Could not log request details");
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    SendMessageResponse messageResponse = response.body();

                    String messageId = messageResponse.getId();
                    Log.d(TAG, "📋 MessageId from getId(): " + messageId);

                    if (messageId == null || messageId.isEmpty()) {
                        Log.w(TAG, "📋 MessageId is null/empty, checking response for other ID fields");
                        Log.w(TAG, "📋 This means the server is NOT returning an '_id' field in the response");
                        Log.w(TAG, "📋 Check your server's message creation endpoint - it should return the created message with _id");
                    }

                    // Emit socket event for message sent
                    Log.d(TAG, "📤 About to emit message-sent - MessageId: " + messageId + ", ChatId: " + chatId + ", UserId: " + userId);
                    Log.d(TAG, "📤 Socket connected: " + (SocketManager.isConnected()));
                    
                    if (messageId != null && !messageId.isEmpty()) {
                        SocketManager.emitMessageSent(messageId, chatId, userId);
                        Log.d(TAG, "📤 Emitted message-sent event for message: " + messageId);
                    } else {
                        Log.e(TAG, "📤 Cannot emit message-sent: messageId is null or empty");
                        Log.e(TAG, "📤 This means the server response doesn't contain a valid ID field");
                    }

                    runOnUiThread(() -> {
                        editMessage.setText("");
                        Log.d(TAG, "Input field cleared");
                    });

                    new android.os.Handler().postDelayed(() -> {
                        Log.d(TAG, "Refreshing messages after successful send");
                        fetchMessages(chatId);
                    }, 500);
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
    
    // Typing indicator methods
    private void startTyping() {
        if (!isTyping && chatId != null) {
            isTyping = true;
            String currentUserId = sessionManager.getCurrentUserId();
            String currentUserName = sessionManager.getCurrentUserUsername();
            if (currentUserId != null && currentUserName != null) {
                SocketManager.emitTypingStart(chatId, currentUserId, currentUserName);
            }
            
            // Stop typing after 3 seconds of inactivity
            typingHandler.removeCallbacks(typingRunnable);
            typingHandler.postDelayed(typingRunnable, 3000);
        }
    }
    
    private void stopTyping() {
        if (isTyping && chatId != null) {
            isTyping = false;
            String currentUserId = sessionManager.getCurrentUserId();
            String currentUserName = sessionManager.getCurrentUserUsername();
            if (currentUserId != null && currentUserName != null) {
                SocketManager.emitTypingStop(chatId, currentUserId);
            }
            
            typingHandler.removeCallbacks(typingRunnable);
        }
    }
    
    // MessageStatusListener implementation
    @Override
    public void onMessageStatusChanged(String messageId, String status) {
        Log.d(TAG, "� RECEIVED STATUS UPDATE - ID: " + messageId + " → " + status.toUpperCase());
        runOnUiThread(() -> {
            if (messageAdapter != null) {
                messageAdapter.updateMessageStatus(messageId, status);
                Log.d(TAG, "✅ UI UPDATED - Message " + messageId + " status set to " + status);
                
                // Find and log the message in our list
                for (MessageItem message : messageList) {
                    if (messageId.equals(message.getMessageId())) {
                        Log.d(TAG, "📝 MESSAGE FOUND - Content: '" + message.getContent() + "', Old Status: " + message.getStatus() + ", New Status: " + status);
                        break;
                    }
                }
            } else {
                Log.e(TAG, "❌ MESSAGE ADAPTER IS NULL - Cannot update status");
            }
        });
    }
    
    @Override
    public void onUserTyping(String userId, String userName, boolean isTyping) {
        Log.d(TAG, "👤 USER TYPING STATUS - " + userName + " (" + userId + ") is typing: " + isTyping);
        runOnUiThread(() -> {
            if (typingIndicator != null) {
                if (isTyping) {
                    typingIndicator.setText(userName + " is typing...");
                    typingIndicator.setVisibility(View.VISIBLE);
                } else {
                    typingIndicator.setVisibility(View.GONE);
                }
            }
        });
    }
    
    @Override
    public void onUserPresenceChanged(String userId, boolean isInChat) {
        Log.d(TAG, "User presence changed: " + userId + " is in chat: " + isInChat);
        // You can update UI to show user presence if needed
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up typing
        if (isTyping) {
            stopTyping();
        }
        
        // Notify that user left chat
        if (chatId != null) {
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId != null) {
                SocketManager.emitUserLeftChat(chatId, currentUserId);
            }
        }
        
        // Clean up handlers
        if (typingHandler != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
    }
    
    private String formatMessageTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            // Return current time if no timestamp
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(new Date());
        }
        
        try {
            // Parse ISO format from server: "2023-12-07T15:30:00.000Z"
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(timestamp);
            
            // Format to readable time: "3:30 PM"
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing timestamp: " + timestamp, e);
            // Return current time as fallback
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(new Date());
        }
    }
}
