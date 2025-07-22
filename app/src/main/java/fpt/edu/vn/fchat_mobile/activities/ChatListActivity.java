package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.ChatAdapter;
import fpt.edu.vn.fchat_mobile.items.ChatItem;
import fpt.edu.vn.fchat_mobile.models.UserStatus;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.responses.ChatResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatListActivity extends AppCompatActivity implements SocketManager.ChatListListener {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatItem> chatList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;
    private Socket socket;
    private final Map<String, String> participantChatMap = new HashMap<>();
    private static final String TAG = "ChatListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set Vietnamese locale
        Locale desiredLocale = new Locale("vi", "VN");
        LocaleListCompat localeList = LocaleListCompat.create(desiredLocale);
        AppCompatDelegate.setApplicationLocales(localeList);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        sessionManager = new SessionManager(this);

        if (!sessionManager.hasValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupApiService();
        setupSocket();
        setupTabs();
        fetchChatsFromApi();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(this, chatList);
        recyclerView.setAdapter(chatAdapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) {
                return true;
            } else if (id == R.id.nav_menu) {
                startActivity(new Intent(this, MenuActivity.class));
                return true;
            }
            return false;
        });

        EditText searchInput = findViewById(R.id.search_input);
        // Add TextWatcher if needed
    }

    private void setupApiService() {
        apiService = ApiClient.getService();
    }

    private void setupSocket() {
        socket = SocketManager.getSocket();
        if (!SocketManager.isConnected()) {
            SocketManager.initializeSocket();
            socket = SocketManager.getSocket();
        }

        // Add debug logging for chat list
        Log.d(TAG, "🔧 CHAT LIST: Setting up socket listeners");
        fpt.edu.vn.fchat_mobile.utils.SocketDebugger.checkSocketStatus();

        // Setup chat list listeners for real-time updates
        SocketManager.setupChatListListeners(this);
        
        // Setup call listeners for incoming calls
        setupCallListeners();
        
        // CRITICAL: Always register user on socket setup to handle auto-login scenarios
        String currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId != null) {
            Log.d(TAG, "🔑 AUTO-LOGIN: Setting up user registration for auto-login: " + currentUserId);
            SocketManager.registerUserForAutoLogin(currentUserId);
            
            // Set up registration callback for auto-login
            SocketManager.setRegistrationCallback(new SocketManager.RegistrationCallback() {
                @Override
                public void onRegistrationVerified(String verifiedUserId, boolean isVerified) {
                    if (isVerified && verifiedUserId.equals(currentUserId)) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "🎉 AUTO-LOGIN: User registration verified successfully");
                            Toast.makeText(ChatListActivity.this, "Online status restored", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.w(TAG, "⚠️ AUTO-LOGIN: Registration verification failed - Verified: " + isVerified + ", UserMatch: " + (verifiedUserId != null ? verifiedUserId.equals(currentUserId) : "null"));
                    }
                }
            });
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    String userId = sessionManager.getCurrentUserId();
                    if (userId != null) {
                        Log.d(TAG, "🔄 RECONNECT: Socket reconnected, ensuring user registration: " + userId);
                        
                        // Check if user is already registered, if not, register
                        if (!SocketManager.isRegistrationVerified()) {
                            SocketManager.registerUser(userId);
                            Log.d(TAG, "📝 RECONNECT: User not registered, registering now");
                        } else {
                            Log.d(TAG, "✅ RECONNECT: User already registered, verifying status");
                            SocketManager.verifyRegistration();
                        }
                        
                        // Update registration callback for reconnection scenarios
                        SocketManager.setRegistrationCallback(new SocketManager.RegistrationCallback() {
                            @Override
                            public void onRegistrationVerified(String verifiedUserId, boolean isVerified) {
                                if (isVerified && verifiedUserId.equals(userId)) {
                                    runOnUiThread(() -> {
                                        Log.d(TAG, "🎉 RECONNECT: User registration verified after reconnection");
                                        Toast.makeText(ChatListActivity.this, "Reconnected to server", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });
                    }
                });
            }
        });

        socket.on("user-status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String userId = data.getString("userId");
                    boolean isOnline = data.getBoolean("isOnline");
                    String lastOnline = data.isNull("lastOnline") ? null : data.getString("lastOnline");
                    Log.d(TAG, "Received user-status: userId=" + userId + ", isOnline=" + isOnline + ", lastOnline=" + lastOnline);
                    runOnUiThread(() -> updateUserStatus(userId, isOnline, lastOnline));
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing user-status event", e);
                }
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Socket disconnected");
                    Toast.makeText(ChatListActivity.this, "Disconnected from server", Toast.LENGTH_SHORT).show();
                });
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Socket connection error: " + args[0].toString());
                    Toast.makeText(ChatListActivity.this, "Connection error: " + args[0], Toast.LENGTH_LONG).show();
                });
            }
        });

        // Add typing event listeners for chat list
        socket.on("typing-start", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String chatId = data.getString("chatId");
                    String userId = data.getString("userId");
                    String userName = data.getString("userName");
                    String currentUserId = sessionManager.getCurrentUserId();
                    
                    if (!userId.equals(currentUserId)) {
                        runOnUiThread(() -> {
                            chatAdapter.updateTypingStatus(chatId, true, userName);
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing typing-start event", e);
                }
            }
        });

        socket.on("typing-stop", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String chatId = data.getString("chatId");
                    String userId = data.getString("userId");
                    String currentUserId = sessionManager.getCurrentUserId();
                    
                    if (!userId.equals(currentUserId)) {
                        runOnUiThread(() -> {
                            chatAdapter.updateTypingStatus(chatId, false, null);
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing typing-stop event", e);
                }
            }
        });

        // Interface implementation for chat list updates
    }
    
    // Add call listener implementation
    private void setupCallListeners() {
        SocketManager.setupCallListeners(new SocketManager.CallListener() {
            @Override
            public void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp) {
                runOnUiThread(() -> {
                    Log.d(TAG, "📲 Incoming call from: " + callerName);
                    
                    // Start IncomingCallActivity
                    Intent incomingCallIntent = new Intent(ChatListActivity.this, IncomingCallActivity.class);
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
                // Handle call answered (if needed in chat list)
            }

            @Override
            public void onCallDeclined(String callId, long timestamp) {
                // Handle call declined (if needed in chat list)
            }

            @Override
            public void onCallEnded(String callId, long timestamp) {
                // Handle call ended (if needed in chat list)
            }

            @Override
            public void onCallFailed(String callId, String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatListActivity.this, "Call failed: " + reason, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCallMuteStatus(String callId, String userId, boolean isMuted) {
                // Handle mute status (if needed in chat list)
            }

            @Override
            public void onCallVideoStatus(String callId, String userId, boolean isVideoOn) {
                // Handle video status (if needed in chat list)
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

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chưa đọc"));
        tabLayout.addTab(tabLayout.newTab().setText("Nhóm"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // TODO: Filter chat list based on tab
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchChatsFromApi() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Call<List<ChatResponse>> call = apiService.getChats(userId);
        call.enqueue(new Callback<List<ChatResponse>>() {
            @Override
            public void onResponse(Call<List<ChatResponse>> call, Response<List<ChatResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chatList.clear();
                    participantChatMap.clear();
                    for (ChatResponse chat : response.body()) {
                        String messageText = "";
                        if (chat.getLastMessage() != null) {
                            MessageResponse lastMsg = chat.getLastMessage();
                            String senderName = lastMsg.getSenderID() != null
                                    ? lastMsg.getSenderID().getFullname()
                                    : "Unknown";
                            messageText = senderName + ": " + lastMsg.getText();
                        }

                        String chatName;
                        String participantId = null;
                        if (chat.getGroupName() != null) {
                            chatName = chat.getGroupName();
                        } else {
                            chatName = getOtherUserName(chat, userId);
                            participantId = getOtherUserId(chat, userId);
                        }

                        ChatItem chatItem = new ChatItem(
                                chat.getId(),
                                chatName,
                                messageText,
                                chat.getUpdateAtTime(),
                                chat.getGroupAvatar(),
                                false, // Default isOnline to false, updated by fetchUserStatus
                                chat.getGroupName() != null
                        );
                        
                        // Set participant ID for non-group chats
                        if (participantId != null) {
                            chatItem.setParticipantId(participantId);
                        }
                        
                        chatList.add(chatItem);
                        if (participantId != null) {
                            participantChatMap.put(participantId, chat.getId());
                            fetchUserStatus(participantId, chatItem);
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                } else {
                    String errorMessage = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMessage = e.getMessage();
                    }
                    Log.e(TAG, "Failed to load chats: " + errorMessage);
                    Toast.makeText(ChatListActivity.this, "Failed to load chats: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChatResponse>> call, Throwable t) {
                String errorMessage = t.getMessage() != null ? t.getMessage() : "Unknown error";
                Log.e(TAG, "Error fetching chat data", t);
                Toast.makeText(ChatListActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchUserStatus(String userId, ChatItem chatItem) {
        apiService.getUserStatus(userId).enqueue(new Callback<UserStatus>() {
            @Override
            public void onResponse(Call<UserStatus> call, Response<UserStatus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched status for user " + userId + ": isOnline=" + response.body().isOnline + ", lastOnline=" + response.body().lastOnline);
                    chatItem.setOnline(response.body().isOnline);
                    chatItem.setLastOnline(response.body().lastOnline);
                    runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
                }
            }

            @Override
            public void onFailure(Call<UserStatus> call, Throwable t) {
                Log.e(TAG, "Error fetching status for user " + userId + ": " + t.getMessage());
            }
        });
    }

    private void updateUserStatus(String userId, boolean isOnline, String lastOnline) {
        String chatId = participantChatMap.get(userId);
        if (chatId != null) {
            for (ChatItem chatItem : chatList) {
                if (chatItem.getId().equals(chatId)) {
                    Log.d(TAG, "Updating status for user " + userId + ": isOnline=" + isOnline + ", lastOnline=" + lastOnline);
                    chatItem.setOnline(isOnline);
                    chatItem.setLastOnline(lastOnline);
                    runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
                    break;
                }
            }
        }
    }

    private String getOtherUserName(ChatResponse chat, String currentUserId) {
        if (chat.getParticipants() != null) {
            for (fpt.edu.vn.fchat_mobile.models.Participant participant : chat.getParticipants()) {
                if (!currentUserId.equals(participant.getId())) {
                    return participant.getFullname() != null ? participant.getFullname() : participant.getUsername();
                }
            }
        }
        return "Unknown User";
    }

    private String getOtherUserId(ChatResponse chat, String currentUserId) {
        if (chat.getParticipants() != null) {
            for (fpt.edu.vn.fchat_mobile.models.Participant participant : chat.getParticipants()) {
                if (!currentUserId.equals(participant.getId())) {
                    return participant.getId();
                }
            }
        }
        return null;
    }

    // Interface implementation for ChatListListener
    @Override
    public void onChatListMessageUpdate(String chatId, String lastMessage, String senderName, String timestamp) {
        runOnUiThread(() -> {
            Log.d(TAG, "📋 Updating chat list for chat: " + chatId);
            updateChatListItem(chatId, lastMessage, senderName, timestamp);
        });
    }

    private void updateChatListItem(String chatId, String lastMessage, String senderName, String timestamp) {
        // Find the chat item in the list and update it
        for (int i = 0; i < chatList.size(); i++) {
            ChatItem chatItem = chatList.get(i);
            if (chatItem.getId().equals(chatId)) {
                // Format the message with sender name
                String formattedMessage = senderName + ": " + lastMessage;

                // Update the chat item
                chatItem.setMessage(formattedMessage);
                chatItem.setTime(formatTimestamp(timestamp));

                // Move chat to top of list for better UX
                chatList.remove(i);
                chatList.add(0, chatItem);

                // Notify adapter of changes
                chatAdapter.notifyDataSetChanged();

                Log.d(TAG, "✅ Updated chat list item: " + chatItem.getName());
                break;
            }
        }
    }

    private String formatTimestamp(String timestamp) {
        try {
            // If timestamp is in ISO format, parse and format it
            if (timestamp != null && !timestamp.isEmpty()) {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                java.util.Date date = isoFormat.parse(timestamp);
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                return timeFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp: " + timestamp, e);
        }

        // Return current time as fallback
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
        return timeFormat.format(new java.util.Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Do not disconnect socket to maintain connection
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        String currentUserId = sessionManager.getCurrentUserId();
        
        // Enhanced auto-login handling - ensure user is properly registered
        if (currentUserId != null) {
            Log.d(TAG, "📱 AUTO-LOGIN: ChatList resumed with user: " + currentUserId);
            
            // Check if user is registered, if not, force registration
            if (!SocketManager.isRegistrationVerified()) {
                Log.w(TAG, "⚠️ AUTO-LOGIN: User not registered on resume, forcing registration");
                SocketManager.registerUserForAutoLogin(currentUserId);
            } else {
                Log.d(TAG, "✅ AUTO-LOGIN: User already registered, performing sync");
                SocketManager.forceSyncOnResume(currentUserId);
            }
        }
        
        // Refresh chat list when returning to activity
        Log.d(TAG, "📱 ChatList resumed - refreshing data");
        fetchChatsFromApi();
        
        // Re-setup socket listeners in case they were lost
        if (socket != null && socket.connected()) {
            SocketManager.setupChatListListeners(this);
        }
    }
}