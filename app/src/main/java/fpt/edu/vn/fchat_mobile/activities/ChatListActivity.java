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

public class ChatListActivity extends AppCompatActivity {

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

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    String userId = sessionManager.getCurrentUserId();
                    socket.emit("register-user", userId);
                    Log.d(TAG, "Socket connected, user registered: " + userId);
                    Toast.makeText(ChatListActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Do not disconnect socket to maintain connection
    }
}