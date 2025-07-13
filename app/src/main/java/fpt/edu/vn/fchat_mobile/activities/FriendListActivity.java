package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.FriendListAdapter;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;

public class FriendListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FriendListAdapter adapter;
    private List<Friend> friendList = new ArrayList<>();
    private FriendRepository friendRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        sessionManager = new SessionManager(this);
        String currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new FriendListAdapter(this, friendList, currentUserId); // ✅ truyền requesterId
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        friendRepository = new FriendRepository();
        fetchFriends(currentUserId);
    }

    private void fetchFriends(String userId) {
        friendRepository.getFriends(userId, new FriendRepository.FriendCallback() {
            @Override
            public void onSuccess(List<Friend> friends) {
                friendList.clear();
                friendList.addAll(friends);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable t) {
                Toast.makeText(FriendListActivity.this, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
