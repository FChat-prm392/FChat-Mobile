package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.SearchUserAdapter;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;

public class AddFriendActivity extends AppCompatActivity {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;

    private SearchUserAdapter adapter;
    private List<FriendRepository.AccountWithStatus> userListWithStatus = new ArrayList<>();
    private FriendRepository friendRepository;
    private SessionManager sessionManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getCurrentUserId();

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        friendRepository = new FriendRepository();

        setupViews();
        setupRecyclerView();
        setupSearch();
        fetchNonFriends();

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thêm bạn bè");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyText = findViewById(R.id.empty_text);
    }

    private void setupRecyclerView() {
        adapter = new SearchUserAdapter(this, userListWithStatus, currentUserId, this::onProfileClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                android.util.Log.d("AddFriend", "Search query: " + query + ", length: " + query.length());

                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    fetchNonFriends();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchNonFriends() {
        android.util.Log.d("AddFriend", "Fetching non-friends for user: " + currentUserId);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        friendRepository.getNonFriends(currentUserId, new FriendRepository.UsersWithStatusCallback() {
            @Override
            public void onSuccess(List<FriendRepository.AccountWithStatus> usersWithStatus) {
                android.util.Log.d("AddFriend", "Fetched non-friends: " + (usersWithStatus != null ? usersWithStatus.size() : 0));
                runOnUiThread(() -> {
                    try {
                        progressBar.setVisibility(View.GONE);
                        userListWithStatus.clear();

                        if (usersWithStatus != null) {
                            android.util.Log.d("AddFriend", "Adding " + usersWithStatus.size() + " non-friends to list");
                            userListWithStatus.addAll(usersWithStatus);
                        }

                        if (adapter != null) {
                            adapter.updateUsers(userListWithStatus);
                            android.util.Log.d("AddFriend", "Adapter notified");
                        }

                        if (usersWithStatus == null || usersWithStatus.isEmpty()) {
                            showEmptyState(true, "Không có người dùng nào để thêm bạn");
                        } else {
                            showEmptyState(false, "");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AddFriend", "Error in onSuccess: " + e.getMessage(), e);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddFriendActivity.this, "Lỗi xử lý danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true, "Lỗi hiển thị danh sách");
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                android.util.Log.e("AddFriend", "Error fetching non-friends: " + (t != null ? t.getMessage() : "null"), t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    String errorMessage = t != null ? t.getMessage() : "Lỗi không xác định";
                    Toast.makeText(AddFriendActivity.this, "Lỗi tải danh sách: " + errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState(true, "Lỗi khi tải danh sách người dùng");
                });
            }
        });
    }

    private void searchUsers(String query) {
        android.util.Log.d("AddFriend", "Searching users with query: " + query);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        friendRepository.searchUsers(query, currentUserId, new FriendRepository.UsersWithStatusCallback() {
            @Override
            public void onSuccess(List<FriendRepository.AccountWithStatus> usersWithStatus) {
                android.util.Log.d("AddFriend", "Search success, users found: " + (usersWithStatus != null ? usersWithStatus.size() : 0));
                runOnUiThread(() -> {
                    try {
                        progressBar.setVisibility(View.GONE);
                        userListWithStatus.clear();

                        if (usersWithStatus != null) {
                            android.util.Log.d("AddFriend", "Adding " + usersWithStatus.size() + " users to list");
                            userListWithStatus.addAll(usersWithStatus);
                        }

                        if (adapter != null) {
                            adapter.updateUsers(userListWithStatus);
                            android.util.Log.d("AddFriend", "Adapter notified");
                        }

                        if (usersWithStatus == null || usersWithStatus.isEmpty()) {
                            showEmptyState(true, "Không tìm thấy người dùng nào");
                        } else {
                            showEmptyState(false, "");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AddFriend", "Error in onSuccess: " + e.getMessage(), e);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddFriendActivity.this, "Lỗi xử lý kết quả tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true, "Lỗi hiển thị kết quả");
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                android.util.Log.e("AddFriend", "Search error: " + (t != null ? t.getMessage() : "null"), t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    String errorMessage = t != null ? t.getMessage() : "Lỗi không xác định";
                    Toast.makeText(AddFriendActivity.this, "Lỗi tìm kiếm: " + errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState(true, "Lỗi khi tìm kiếm người dùng");
                });
            }
        });
    }

    private void onProfileClick(String userId) {
        Intent intent = new Intent(AddFriendActivity.this, ProfileActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("currentUserId", currentUserId);
        startActivity(intent);
    }

    private void showEmptyState(boolean show, String message) {
        if (show) {
            emptyText.setText(message);
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}