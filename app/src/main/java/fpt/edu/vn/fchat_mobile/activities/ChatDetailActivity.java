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
import android.widget.LinearLayout;
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
import java.util.Map;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.MessageAdapter;
import fpt.edu.vn.fchat_mobile.items.MessageItem;
import fpt.edu.vn.fchat_mobile.models.MessageReaction;
import fpt.edu.vn.fchat_mobile.models.ReactionSummary;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.repositories.ReactionRepository;
import fpt.edu.vn.fchat_mobile.requests.SendMessageRequest;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.responses.SendMessageResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import fpt.edu.vn.fchat_mobile.utils.ReactionManager;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;
import fpt.edu.vn.fchat_mobile.views.TypingIndicatorView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity implements SocketManager.MessageStatusListener {

    private ImageView avatarView, btnSend, btnMic, btnCamera, btnCall, btnVideo;
    private TextView nameText, statusText;
    private EditText editMessage;
    private TypingIndicatorView typingIndicator;
    private LinearLayout typingIndicatorContainer;

    private final List<MessageItem> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private ReactionRepository reactionRepository;
    private String chatId;
    private String participantId; // Store the other participant's ID
    private SessionManager sessionManager;
    private static final String TAG = "ChatDetailActivity";

    // Typing functionality
    private Handler typingHandler = new Handler();
    private Runnable typingRunnable = new Runnable() {
        @Override
        public void run() {
            stopTyping();
        }
    };
    private boolean isTyping = false;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // Reaction callback implementation
    private final ReactionManager.ReactionCallback reactionCallback = new ReactionManager.ReactionCallback() {
        @Override
        public void onReactionSelected(String messageId, String emoji, boolean isAdding) {
            handleReactionSelection(messageId, emoji, isAdding);
        }

        @Override
        public void onReactionClicked(String messageId, String emoji, List<String> userNames) {
            ReactionManager.showReactionUsers(ChatDetailActivity.this, emoji, userNames);
        }
    };

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
        reactionRepository = new ReactionRepository();

        if (!sessionManager.hasValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserName = sessionManager.getCurrentUserUsername();

        avatarView = findViewById(R.id.avatar);
        nameText = findViewById(R.id.name);
        statusText = findViewById(R.id.status);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);
        btnCamera = findViewById(R.id.btn_camera);
        btnCall = findViewById(R.id.btn_call);
        btnVideo = findViewById(R.id.btn_video);

        typingIndicator = findViewById(R.id.typing_indicator);
        typingIndicatorContainer = findViewById(R.id.typing_indicator_container);

        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId");
        participantId = intent.getStringExtra("participantId");
        String name = intent.getStringExtra("name");
        String status = intent.getStringExtra("status");
        String avatarUrl = intent.getStringExtra("avatarUrl");

        nameText.setText(name);
        statusText.setText(status);

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .into(avatarView);

        SocketManager.initializeSocket();
        SocketManager.setupMessageStatusListeners(this);

        setupCallListeners();

        if (chatId != null) {
            SocketManager.joinRoom(chatId);
        }

        if (currentUserId != null) {
            SocketManager.registerUser(currentUserId);
        }

        if (chatId != null && currentUserId != null) {
            SocketManager.emitUserEnteredChat(chatId, currentUserId);
        }

        if (chatId != null && currentUserId != null) {
            SocketManager.requestChatStatusSync(chatId, currentUserId);
        }

        RecyclerView recyclerView = findViewById(R.id.message_list);
        messageAdapter = new MessageAdapter(messageList);
        messageAdapter.setCurrentUserId(sessionManager.getCurrentUserId());
        messageAdapter.setReactionCallback(reactionCallback);
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchMessages(chatId);

        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessage(content);
                editMessage.setText("");
                
                if (isTyping) {
                    stopTyping();
                }
            }
        });
        
        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            
            if (!content.isEmpty()) {
                sendMessage(content);
                editMessage.setText("");
                
                if (isTyping) {
                    stopTyping();
                }
            }
        });

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

        btnCall.setOnClickListener(v -> startVoiceCall());
        btnVideo.setOnClickListener(v -> startVideoCall());

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
        
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId != null) {
                SocketManager.requestChatStatusSync(chatId, currentUserId);
            }
        }
        
        markAllMessagesAsRead();
    }

    private void markAllMessagesAsRead() {
        if (chatId != null && messageList != null && !messageList.isEmpty()) {
            String currentUserId = sessionManager.getCurrentUserId();
            
            for (MessageItem message : messageList) {
                if (!message.isSentByUser() && message.getMessageId() != null && !"read".equals(message.getStatus())) {
                    SocketManager.emitMessageRead(message.getMessageId(), chatId, currentUserId);
                    message.setStatus("read");
                }
            }
            
            if (messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
            }
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

                        String formattedTime = formatMessageTime(msg.getCreateAt());
                        MessageItem messageItem = new MessageItem(msg.getText(), isMine, msg.getId(), formattedTime);
                        messageList.add(messageItem);

                        if (msg.getId() != null) {
                            loadMessageReactions(msg.getId());
                        }

                        if (!isMine && msg.getId() != null) {
                            Log.d(TAG, "📬 EMITTING MESSAGE-DELIVERED for message: " + msg.getId());
                            SocketManager.emitMessageDelivered(msg.getId(), chatId, currentUserId);
                        }

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
                        
                        // Also emit the message for real-time delivery to other users
                        try {
                            String currentUserName = sessionManager.getCurrentUserUsername();
                            SocketManager.emitRealtimeMessage(messageId, content, userId, currentUserName, chatId);
                            Log.d(TAG, "📡 Emitted real-time message for immediate delivery");
                        } catch (Exception e) {
                            Log.e(TAG, "Error emitting real-time message", e);
                        }
                        
                        // Add message to local list with initial "sent" status
                        String formattedTime = formatMessageTime(null); // Current time
                        MessageItem newMessage = new MessageItem(content, true, messageId, formattedTime);
                        newMessage.setStatus("sent"); // Set initial status
                        
                        runOnUiThread(() -> {
                            messageList.add(newMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            
                            // Scroll to bottom
                            RecyclerView recyclerView = findViewById(R.id.message_list);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                            
                            Log.d(TAG, "✅ Message added to local list with 'sent' status");
                        });
                    } else {
                        Log.e(TAG, "📤 Cannot emit message-sent: messageId is null or empty");
                        Log.e(TAG, "📤 This means the server response doesn't contain a valid ID field");
                    }

                    runOnUiThread(() -> {
                        editMessage.setText("");
                        Log.d(TAG, "Input field cleared");
                    });
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
            
            // Stop typing after 2 seconds of inactivity (reduced for better UX)
            typingHandler.removeCallbacks(typingRunnable);
            typingHandler.postDelayed(typingRunnable, 2000);
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
        Log.d(TAG, "📥 RECEIVED STATUS UPDATE - ID: " + messageId + " → " + status.toUpperCase());
        
        // Debug: check if this messageId exists in our current message list
        boolean found = false;
        for (MessageItem message : messageList) {
            if (messageId.equals(message.getMessageId())) {
                found = true;
                Log.d(TAG, "✅ MESSAGE FOUND IN LIST - Content: '" + 
                           (message.getContent() != null ? 
                            message.getContent().substring(0, Math.min(30, message.getContent().length())) : "null") + 
                           "', Current Status: " + message.getStatus());
                break;
            }
        }
        
        if (!found) {
            Log.w(TAG, "❌ MESSAGE NOT FOUND IN CURRENT LIST - ID: " + messageId);
            Log.d(TAG, "🔍 Current message list has " + messageList.size() + " messages:");
            for (int i = 0; i < Math.min(5, messageList.size()); i++) {
                MessageItem msg = messageList.get(i);
                Log.d(TAG, "  " + i + ": ID=" + msg.getMessageId() + ", Content='" + 
                           (msg.getContent() != null ? 
                            msg.getContent().substring(0, Math.min(20, msg.getContent().length())) : "null") + "'");
            }
        }
        
        runOnUiThread(() -> {
            if (messageAdapter != null) {
                messageAdapter.updateMessageStatus(messageId, status);
                Log.d(TAG, "✅ UI UPDATE REQUESTED - Message " + messageId + " status set to " + status);
            } else {
                Log.e(TAG, "❌ MESSAGE ADAPTER IS NULL - Cannot update status");
            }
        });
    }
    
    @Override
    public void onUserTyping(String userId, String userName, boolean isTyping) {
        Log.d(TAG, "👤 USER TYPING STATUS - " + userName + " (" + userId + ") is typing: " + isTyping);
        
        // Don't show typing indicator for our own typing
        String currentUserId = sessionManager.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            return;
        }
        
        runOnUiThread(() -> {
            if (typingIndicator != null && typingIndicatorContainer != null) {
                if (isTyping) {
                    typingIndicator.showTyping(userName);
                    typingIndicatorContainer.setVisibility(View.VISIBLE);
                    
                    // Auto-scroll to show typing indicator
                    RecyclerView recyclerView = findViewById(R.id.message_list);
                    if (recyclerView != null && messageList.size() > 0) {
                        recyclerView.smoothScrollToPosition(messageList.size() - 1);
                    }
                } else {
                    typingIndicator.hideTyping();
                    typingIndicatorContainer.setVisibility(View.GONE);
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
    public void onBulkStatusSync(int syncCount) {
        Log.d(TAG, "📥 BULK STATUS SYNC - " + syncCount + " messages updated");
        runOnUiThread(() -> {
            if (messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
                Log.d(TAG, "✅ UI REFRESHED after bulk status sync");
            }
        });
    }
    
    @Override
    public void onNewMessageReceived(String messageId, String content, String senderId, String senderName, String chatId, String timestamp) {
        Log.d(TAG, "📥 NEW MESSAGE RECEIVED - From: " + senderName + ", Content: '" + content + "'");
        
        // Only process messages for the current chat
        if (!chatId.equals(this.chatId)) {
            Log.d(TAG, "⏭️ Message not for current chat - ignoring");
            return;
        }
        
        // Don't add our own messages (they're already added when sending)
        String currentUserId = sessionManager.getCurrentUserId();
        if (senderId.equals(currentUserId)) {
            Log.d(TAG, "⏭️ Own message - ignoring");
            return;
        }
        
        // Check if message already exists in our list
        for (MessageItem existingMessage : messageList) {
            if (messageId.equals(existingMessage.getMessageId())) {
                Log.d(TAG, "⏭️ Message already exists - ignoring duplicate");
                return;
            }
        }
        
        runOnUiThread(() -> {
            try {
                // Format timestamp
                String formattedTime = formatMessageTime(timestamp);
                
                // Create new message item
                MessageItem newMessage = new MessageItem(content, false, messageId, formattedTime);
                newMessage.setStatus("delivered");
                
                // Add to message list
                messageList.add(newMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                
                // Scroll to bottom to show new message
                RecyclerView recyclerView = findViewById(R.id.message_list);
                recyclerView.scrollToPosition(messageList.size() - 1);
                
                Log.d(TAG, "✅ New message added to UI");
                
                // Automatically mark as read since user is viewing the chat
                if (messageId != null && !messageId.isEmpty()) {
                    SocketManager.emitMessageRead(messageId, this.chatId, currentUserId);
                    newMessage.setStatus("read");
                    messageAdapter.notifyItemChanged(messageList.size() - 1);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error adding new message to UI", e);
            }
        });
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
    
    private void startVoiceCall() {
        String otherParticipantId = getOtherParticipantId();
        if (otherParticipantId == null) {
            Toast.makeText(this, "Cannot start call: Participant ID not found. Please check your chat data setup.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", nameText.getText().toString());
        callIntent.putExtra("participantId", otherParticipantId);
        callIntent.putExtra("avatarUrl", getIntent().getStringExtra("avatarUrl"));
        callIntent.putExtra("isVideoCall", false);
        callIntent.putExtra("isIncomingCall", false);
        startActivity(callIntent);
    }
    
    private void startVideoCall() {
        String otherParticipantId = getOtherParticipantId();
        if (otherParticipantId == null) {
            Toast.makeText(this, "Cannot start call: Participant ID not found. Please check your chat data setup.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("chatId", chatId);
        callIntent.putExtra("participantName", nameText.getText().toString());
        callIntent.putExtra("participantId", otherParticipantId);
        callIntent.putExtra("avatarUrl", getIntent().getStringExtra("avatarUrl"));
        callIntent.putExtra("isVideoCall", true);
        callIntent.putExtra("isIncomingCall", false);
        startActivity(callIntent);
    }
    
    private String getOtherParticipantId() {
        // Return the actual participant ID passed from the chat list
        if (participantId != null && !participantId.isEmpty()) {
            return participantId;
        }
        
        // Fallback: For testing, use a different approach
        // In a real implementation, you would fetch the chat participants from the server
        Log.w("ChatDetailActivity", "No participantId found, need to fetch from server or set properly in ChatItem");
        
        // TODO: You need to either:
        // 1. Update your chat list API to include participant IDs
        // 2. Make an API call here to get chat participants
        // 3. For testing, manually replace this with a real user ID from your database
        
        // For now, return null to indicate the issue needs to be fixed
        return null;
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
    
    // Add call listener implementation
    private void setupCallListeners() {
        SocketManager.setupCallListeners(new SocketManager.CallListener() {
            @Override
            public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
                runOnUiThread(() -> {
                    Log.d(TAG, "📲 Incoming call from: " + callerName + " while in chat");
                    
                    // Start IncomingCallActivity
                    Intent incomingCallIntent = new Intent(ChatDetailActivity.this, IncomingCallActivity.class);
                    incomingCallIntent.putExtra("callId", callId);
                    incomingCallIntent.putExtra("chatId", chatId);
                    incomingCallIntent.putExtra("callerId", callerId);
                    incomingCallIntent.putExtra("callerName", callerName);
                    incomingCallIntent.putExtra("isVideoCall", isVideoCall);
                    incomingCallIntent.putExtra("timestamp", timestamp);
                    incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(incomingCallIntent);
                });
            }

            @Override
            public void onCallAnswered(String callId, long timestamp) {
                // Handle call answered
            }

            @Override
            public void onCallDeclined(String callId, long timestamp) {
                // Handle call declined
            }

            @Override
            public void onCallEnded(String callId, long timestamp) {
                // Handle call ended
            }

            @Override
            public void onCallFailed(String callId, String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatDetailActivity.this, "Call failed: " + reason, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCallMuteStatus(String callId, String userId, boolean isMuted) {
                // Handle mute status
            }

            @Override
            public void onCallVideoStatus(String callId, String userId, boolean isVideoOn) {
                // Handle video status
            }
        });
    }
    
    // 🎭 REACTION HANDLING METHODS
    private void handleReactionSelection(String messageId, String emoji, boolean isAdding) {
        try {
            String currentUserId = sessionManager.getCurrentUserId();
            
            if (currentUserId == null || messageId == null || emoji == null) {
                runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                    "Error: Cannot process reaction", Toast.LENGTH_SHORT).show());
                return;
            }
            
            boolean userAlreadyReacted = checkIfUserAlreadyReacted(messageId, currentUserId, emoji);
            
            if (userAlreadyReacted) {
                removeReaction(messageId, currentUserId, emoji);
            } else {
                addReaction(messageId, currentUserId, emoji);
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                "Error processing reaction", Toast.LENGTH_SHORT).show());
        }
    }
    
    private boolean checkIfUserAlreadyReacted(String messageId, String userId, String emoji) {
        try {
            if (messageId == null || userId == null || emoji == null) {
                return false;
            }
            
            for (MessageItem message : messageList) {
                if (messageId.equals(message.getMessageId())) {
                    Map<String, ReactionSummary> summaries = message.getReactionSummaries();
                    if (summaries != null) {
                        ReactionSummary summary = summaries.get(emoji);
                        return summary != null && summary.isCurrentUserReacted();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return false;
    }
    
    private void addReaction(String messageId, String userId, String emoji) {
        try {
            if (messageId == null || userId == null || emoji == null) {
                runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                    "Error: Missing reaction data", Toast.LENGTH_SHORT).show());
                return;
            }
            
            reactionRepository.addReaction(messageId, userId, emoji, new ReactionRepository.ReactionCallback() {
                @Override
                public void onSuccess(MessageReaction reaction) {
                    if (reaction != null) {
                        if (reaction.getUserName() == null || reaction.getUserName().isEmpty()) {
                            String currentUserName = sessionManager.getCurrentUserUsername();
                            reaction.setUserName(currentUserName);
                        }
                    }
                    
                    updateMessageReactionLocally(messageId, reaction, true);
                }

                @Override
                public void onError(Throwable error) {
                    if (error != null && error.getMessage() != null && error.getMessage().contains("REACTION_EXISTS")) {
                        removeReaction(messageId, userId, emoji);
                    } else {
                        runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                            "Failed to add reaction: " + (error != null ? error.getMessage() : "Unknown error"), 
                            Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                "Error adding reaction", Toast.LENGTH_SHORT).show());
        }
    }
    
    private void removeReaction(String messageId, String userId, String emoji) {
        try {
            if (messageId == null || userId == null || emoji == null) {
                runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                    "Error: Missing reaction data", Toast.LENGTH_SHORT).show());
                return;
            }
            
            reactionRepository.removeReaction(messageId, userId, emoji, new ReactionRepository.VoidCallback() {
                @Override
                public void onSuccess() {
                    updateMessageReactionLocally(messageId, null, false, userId, emoji);
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                        "Failed to remove reaction: " + (error != null ? error.getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(ChatDetailActivity.this, 
                "Error removing reaction", Toast.LENGTH_SHORT).show());
        }
    }
    
    private void updateMessageReactionLocally(String messageId, MessageReaction reaction, boolean isAdding) {
        updateMessageReactionLocally(messageId, reaction, isAdding, null, null);
    }
    
    private void updateMessageReactionLocally(String messageId, MessageReaction reaction, boolean isAdding, String userId, String emoji) {
        try {
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId == null) {
                return;
            }
            
            runOnUiThread(() -> {
                try {
                    for (int i = 0; i < messageList.size(); i++) {
                        MessageItem message = messageList.get(i);
                        if (messageId != null && messageId.equals(message.getMessageId())) {
                            if (isAdding && reaction != null) {
                                message.addReaction(reaction, currentUserId);
                            } else if (!isAdding && userId != null && emoji != null) {
                                message.removeReaction(userId, emoji, currentUserId);
                            }
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore UI update errors
                }
            });
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    private void loadMessageReactions(String messageId) {
        reactionRepository.getMessageReactions(messageId, new ReactionRepository.ReactionsListCallback() {
            @Override
            public void onSuccess(List<MessageReaction> reactions) {
                String currentUserId = sessionManager.getCurrentUserId();
                for (MessageItem message : messageList) {
                    if (messageId.equals(message.getMessageId())) {
                        message.setReactions(reactions, currentUserId);
                        runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
                        break;
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                // Ignore error - reactions just won't load
            }
        });
    }
}
