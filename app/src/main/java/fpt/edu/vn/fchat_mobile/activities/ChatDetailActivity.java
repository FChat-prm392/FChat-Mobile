package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
    private String participantId;
    private SessionManager sessionManager;
    private static final String TAG = "ChatDetailActivity";

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
        
        // Set up registration callback to proceed once verified
        SocketManager.setRegistrationCallback(new SocketManager.RegistrationCallback() {
            @Override
            public void onRegistrationVerified(String userId, boolean isVerified) {
                Log.d(TAG, "🔔 CALLBACK: Registration verification received - User: " + userId + ", Verified: " + isVerified + ", Expected: " + currentUserId);
                if (isVerified && userId.equals(currentUserId)) {
                    Log.d(TAG, "🎉 VERIFIED: Registration verified via callback, proceeding with chat setup");
                    runOnUiThread(() -> {
                        Log.d(TAG, "🏃 EXECUTING: Chat setup on UI thread");
                        proceedWithChatSetup();
                    });
                } else {
                    Log.w(TAG, "⚠️ MISMATCH: Registration callback received but conditions not met - Verified: " + isVerified + ", UserMatch: " + userId.equals(currentUserId));
                }
            }
        });
        
        // Add debug logging
        fpt.edu.vn.fchat_mobile.utils.SocketDebugger.enableDebugLogging();
        fpt.edu.vn.fchat_mobile.utils.SocketDebugger.checkSocketStatus();

        // Register user first and wait for verification before proceeding
        if (currentUserId != null) {
            SocketManager.registerUser(currentUserId);
            
            // Wait for registration verification before joining room and other operations
            new Handler().postDelayed(() -> {
                // Ensure we're registered before proceeding
                if (!SocketManager.isRegistrationVerified()) {
                    Log.w(TAG, "⚠️ Registration not verified yet, force checking...");
                    SocketManager.forceRegistrationCheck();
                    
                    // Wait a bit more for verification
                    new Handler().postDelayed(() -> {
                        if (SocketManager.isRegistrationVerified()) {
                            proceedWithChatSetup();
                        } else {
                            Log.w(TAG, "⚠️ Still not verified after force check, proceeding anyway...");
                            proceedWithChatSetup();
                        }
                    }, 2000);
                } else {
                    proceedWithChatSetup();
                }
            }, 1500); // Wait 1.5 seconds for initial registration
        } else {
            // No user ID, proceed without registration
            proceedWithChatSetup();
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
    
    private void proceedWithChatSetup() {
        String currentUserId = sessionManager.getCurrentUserId();
        
        Log.d(TAG, "🚀 Proceeding with chat setup - User: " + currentUserId + ", Verified: " + SocketManager.isRegistrationVerified());
        
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            Log.d(TAG, "🏠 Joined room: " + chatId);
        }

        if (chatId != null && currentUserId != null) {
            SocketManager.emitUserEnteredChat(chatId, currentUserId);
            Log.d(TAG, "👤 Emitted user entered chat");
        }

        if (chatId != null && currentUserId != null) {
            SocketManager.requestChatStatusSync(chatId, currentUserId);
            Log.d(TAG, "🔄 Requested chat status sync");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Force sync when resuming (handles auto-login/token issues)
        if (currentUserId != null) {
            SocketManager.forceSyncOnResume(currentUserId);
        }
        
        // Check socket status and reconnect if needed
        fpt.edu.vn.fchat_mobile.utils.SocketDebugger.checkSocketStatus();
        SocketManager.ensureConnection();
        
        if (chatId != null) {
            SocketManager.joinRoom(chatId);
            
            if (currentUserId != null) {
                SocketManager.requestChatStatusSync(chatId, currentUserId);
                
                // Test connection periodically
                SocketManager.testConnection(currentUserId, chatId);
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
            return;
        }

        ApiService apiService = ApiClient.getService();
        Call<List<MessageResponse>> call = apiService.getMessagesByChatId(chatId, 100);
        call.enqueue(new Callback<List<MessageResponse>>() {
            @Override
            public void onResponse(Call<List<MessageResponse>> call, Response<List<MessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MessageResponse> messages = response.body();

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
                                return time1.compareTo(time2);
                            }
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                    messageList.clear();
                    String currentUserId = sessionManager.getCurrentUserId();
                    
                    for (MessageResponse msg : messages) {
                        String senderId = "";
                        if (msg.getSenderID() != null) {
                            if (msg.getSenderID().getId() != null) {
                                senderId = msg.getSenderID().getId();
                            }
                        }
                        
                        boolean isMine = senderId.equals(currentUserId);

                        String formattedTime = formatMessageTime(msg.getCreateAt());
                        MessageItem messageItem = new MessageItem(msg.getText(), isMine, msg.getId(), formattedTime);
                        messageList.add(messageItem);
                        
                        if (msg.getId() != null) {
                            loadMessageReactions(msg.getId());
                        }
                        
                        if (!isMine && msg.getId() != null) {
                            SocketManager.emitMessageDelivered(msg.getId(), chatId, currentUserId);
                        }

                        if (!isMine && msg.getId() != null) {
                            SocketManager.emitMessageRead(msg.getId(), chatId, currentUserId);
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
                    Toast.makeText(ChatDetailActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MessageResponse>> call, Throwable t) {
                Toast.makeText(ChatDetailActivity.this, "Error loading messages: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String content) {
        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Error: Chat not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getCurrentUserId();
        
        // Ensure user is registered before sending message
        if (!SocketManager.isRegistrationVerified()) {
            Log.w(TAG, "⚠️ Sending message but user not verified, ensuring registration...");
            SocketManager.forceRegistrationCheck();
            
            // Retry sending after a short delay
            new Handler().postDelayed(() -> {
                if (SocketManager.isRegistrationVerified()) {
                    sendMessageInternal(content, userId);
                } else {
                    Log.e(TAG, "❌ User still not verified after retry, sending anyway...");
                    sendMessageInternal(content, userId);
                }
            }, 1000);
            return;
        }
        
        sendMessageInternal(content, userId);
    }
    
    private void sendMessageInternal(String content, String userId) {

        SendMessageRequest request = new SendMessageRequest(userId, chatId, content);

        ApiService apiService = ApiClient.getService();
        Call<SendMessageResponse> call = apiService.sendMessage(request);
        call.enqueue(new Callback<SendMessageResponse>() {
            @Override
            public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SendMessageResponse messageResponse = response.body();

                    String messageId = messageResponse.getId();
                    
                    if (messageId != null && !messageId.isEmpty()) {
                        Log.d(TAG, "🚀 SENDING MESSAGE - ID: " + messageId + ", Content: '" + content + "', Chat: " + chatId);
                        
                        SocketManager.emitMessageSent(messageId, chatId, userId);
                        
                        try {
                            String currentUserName = sessionManager.getCurrentUserUsername();
                            Log.d(TAG, "🚀 EMITTING REALTIME MESSAGE - User: " + currentUserName);
                            SocketManager.emitRealtimeMessage(messageId, content, userId, currentUserName, chatId);
                        } catch (Exception e) {
                            Log.e(TAG, "Error emitting realtime message", e);
                        }
                        
                        String formattedTime = formatMessageTime(null);
                        MessageItem newMessage = new MessageItem(content, true, messageId, formattedTime);
                        newMessage.setStatus("sent");
                        
                        runOnUiThread(() -> {
                            messageList.add(newMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            
                            RecyclerView recyclerView = findViewById(R.id.message_list);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        });
                    }

                    runOnUiThread(() -> {
                        editMessage.setText("");
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
                    
                    Toast.makeText(ChatDetailActivity.this, "Failed to send message: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SendMessageResponse> call, Throwable t) {
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
    
    private void startTyping() {
        if (!isTyping && chatId != null) {
            isTyping = true;
            String currentUserId = sessionManager.getCurrentUserId();
            String currentUserName = sessionManager.getCurrentUserUsername();
            if (currentUserId != null && currentUserName != null) {
                SocketManager.emitTypingStart(chatId, currentUserId, currentUserName);
            }
            
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
    
    @Override
    public void onMessageStatusChanged(String messageId, String status) {
        runOnUiThread(() -> {
            if (messageAdapter != null) {
                messageAdapter.updateMessageStatus(messageId, status);
            }
        });
    }
    
    @Override
    public void onUserTyping(String userId, String userName, boolean isTyping) {
        String currentUserId = sessionManager.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            return;
        }
        
        runOnUiThread(() -> {
            if (typingIndicator != null && typingIndicatorContainer != null) {
                if (isTyping) {
                    typingIndicator.showTyping(userName);
                    typingIndicatorContainer.setVisibility(View.VISIBLE);
                    
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
    }
    
    @Override
    public void onBulkStatusSync(int syncCount) {
        runOnUiThread(() -> {
            if (messageAdapter != null) {
                messageAdapter.notifyDataSetChanged();
            }
        });
    }
    
    @Override
    public void onNewMessageReceived(String messageId, String content, String senderId, String senderName, String chatId, String timestamp) {
        Log.d(TAG, "📥 RECEIVED MESSAGE - ID: " + messageId + ", From: " + senderName + " (" + senderId + "), Chat: " + chatId + ", ThisChat: " + this.chatId);
        
        if (!chatId.equals(this.chatId)) {
            Log.d(TAG, "📥 MESSAGE NOT FOR THIS CHAT - Ignoring");
            return;
        }
        
        String currentUserId = sessionManager.getCurrentUserId();
        if (senderId.equals(currentUserId)) {
            Log.d(TAG, "📥 MESSAGE FROM SELF - Ignoring");
            return;
        }
        
        for (MessageItem existingMessage : messageList) {
            if (messageId.equals(existingMessage.getMessageId())) {
                Log.d(TAG, "📥 DUPLICATE MESSAGE DETECTED - ID: " + messageId + " already exists");
                return;
            }
        }
        
        Log.d(TAG, "📥 PROCESSING NEW MESSAGE - Adding to list");
        
        runOnUiThread(() -> {
            try {
                String formattedTime = formatMessageTime(timestamp);
                
                MessageItem newMessage = new MessageItem(content, false, messageId, formattedTime);
                newMessage.setStatus("delivered");
                
                messageList.add(newMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                
                RecyclerView recyclerView = findViewById(R.id.message_list);
                recyclerView.scrollToPosition(messageList.size() - 1);
                
                if (messageId != null && !messageId.isEmpty()) {
                    SocketManager.emitMessageRead(messageId, this.chatId, currentUserId);
                    newMessage.setStatus("read");
                    messageAdapter.notifyItemChanged(messageList.size() - 1);
                }
                
            } catch (Exception e) {
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (isTyping) {
            stopTyping();
        }
        
        if (chatId != null) {
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId != null) {
                SocketManager.emitUserLeftChat(chatId, currentUserId);
            }
        }
        
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
        if (participantId != null && !participantId.isEmpty()) {
            return participantId;
        }
        
        return null;
    }
    
    private String formatMessageTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(new Date());
        }
        
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(timestamp);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(date);
        } catch (Exception e) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(new Date());
        }
    }
    
    private void setupCallListeners() {
        SocketManager.setupCallListeners(new SocketManager.CallListener() {
            @Override
            public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
                runOnUiThread(() -> {
                    
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
            }

            @Override
            public void onCallDeclined(String callId, long timestamp) {
            }

            @Override
            public void onCallEnded(String callId, long timestamp) {
            }

            @Override
            public void onCallFailed(String callId, String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatDetailActivity.this, "Call failed: " + reason, Toast.LENGTH_SHORT).show();
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
            
            @Override
            public void onCallForceTerminated(String callId, String reason) {
                // Handle force termination if needed
            }
        });
    }
    
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
                        return summary != null && summary.isCurrentUserReacted();                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking user reaction", e);
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
                    
                    String currentUserName = sessionManager.getCurrentUserUsername();
                    if (currentUserName != null && chatId != null) {
                        SocketManager.emitReactionAdded(messageId, chatId, userId, currentUserName, emoji);
                    }
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
                    
                    String currentUserName = sessionManager.getCurrentUserUsername();
                    if (currentUserName != null && chatId != null) {
                        SocketManager.emitReactionRemoved(messageId, chatId, userId, currentUserName, emoji);
                    }
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
                    Log.e(TAG, "Error updating reaction locally", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateMessageReactionLocally", e);
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
                Log.e(TAG, "Error loading reactions", error);
            }
        });
    }

    @Override
    public void onReactionAdded(String messageId, String userId, String userName, String emoji) {
        String currentUserId = sessionManager.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            return;
        }
        
        runOnUiThread(() -> {
            try {
                MessageReaction reaction = new MessageReaction();
                reaction.setMessageId(messageId);
                reaction.setUserId(userId);
                reaction.setUserName(userName);
                reaction.setEmoji(emoji);
                
                updateMessageReactionLocally(messageId, reaction, true);
            } catch (Exception e) {
                Log.e(TAG, "Error processing incoming reaction", e);
            }
        });
    }
    
    @Override
    public void onReactionRemoved(String messageId, String userId, String userName, String emoji) {
        String currentUserId = sessionManager.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            return;
        }
        
        runOnUiThread(() -> {
            try {
                updateMessageReactionLocally(messageId, null, false, userId, emoji);
            } catch (Exception e) {
                Log.e(TAG, "Error processing reaction removal", e);
            }
        });
    }
}
