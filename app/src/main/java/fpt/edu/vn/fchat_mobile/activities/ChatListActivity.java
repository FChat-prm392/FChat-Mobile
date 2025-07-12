package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

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

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatItem> chatList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        initViews();
        setupTabs();
        loadChats();
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
            }  else if (id == R.id.nav_menu) {
                startActivity(new Intent(this, MenuActivity.class));
                return true;
            }

            return false;
        });


        EditText searchInput = findViewById(R.id.search_input);
        // Bạn có thể thêm TextWatcher để lọc danh sách tại đây
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chưa đọc"));
        tabLayout.addTab(tabLayout.newTab().setText("Nhóm"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // TODO: Lọc danh sách dựa theo tab
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadChats() {
        chatList.clear();
        chatList.add(new ChatItem("Minh Hoàng", "API_BASE_URL=http://10.0.2.2:50...", "14:26", R.drawable.ic_avatar, true));
        chatList.add(new ChatItem("Huy Nguyen", "Đã bày tỏ cảm xúc 😆 về tin nhắn...", "11:54", R.drawable.ic_avatar, true));
        chatList.add(new ChatItem("Nhan Pham", "Đã bày tỏ cảm xúc 😆 về tin nhắn...", "11:26", R.drawable.ic_avatar, false));
        chatList.add(new ChatItem("Trương An Đào", "Check discord", "11:24", R.drawable.ic_avatar, true));
        chatList.add(new ChatItem("Minh Vu", "Ok", "11:23", R.drawable.ic_avatar, true));
        chatList.add(new ChatItem("Mèi", "Thế là chạy ké sang bên", "11:14", R.drawable.ic_avatar, false));
        chatAdapter.notifyDataSetChanged();
    }


}
