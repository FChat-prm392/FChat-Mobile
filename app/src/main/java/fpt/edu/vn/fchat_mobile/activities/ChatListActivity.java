package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.ChatAdapter;
import fpt.edu.vn.fchat_mobile.items.ChatItem;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.responses.ChatResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatItem> chatList = new ArrayList<>();
    private SessionManager sessionManager;
    private static final String TAG = "ChatListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.hasValidSession()) {
            // Redirect to login if no valid session
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
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
        // You can add TextWatcher here if needed
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
        
        ApiService apiService = ApiClient.getService();
        Call<List<ChatResponse>> call = apiService.getChats(userId);

        call.enqueue(new Callback<List<ChatResponse>>() {
            @Override
            public void onResponse(Call<List<ChatResponse>> call, Response<List<ChatResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chatList.clear();
                    for (ChatResponse chat : response.body()) {
                        String messageText = "";

                        if (chat.getLastMessage() != null) {
                            MessageResponse lastMsg = chat.getLastMessage();
                            String senderName = lastMsg.getSenderID() != null
                                    ? lastMsg.getSenderID().getFullname()
                                    : "Unknown";

                            messageText = senderName + ": " + lastMsg.getText();
                        }

                        // Determine chat name
                        String chatName;
                        if (chat.getGroupName() != null) {
                            chatName = chat.getGroupName();
                        } else {
                            chatName = getOtherUserName(chat, userId);
                        }

                        chatList.add(new ChatItem(
                                chat.getId(),
                                chatName,
                                messageText,
                                chat.getUpdateAtTime(),
                                chat.getGroupAvatar(),
                                true
                        ));
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

                    Toast.makeText(ChatListActivity.this, "Failed to load chats: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChatResponse>> call, Throwable t) {
                String errorMessage = t.getMessage() != null ? t.getMessage() : "Unknown error";
                Log.e("ChatList", "Error fetching chat data", t); // Full stack trace in Logcat
                Toast.makeText(ChatListActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }

        });
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

}
