package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.FriendRequestAdapter;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;

public class AddFriendRequestActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FriendRequestAdapter adapter;
    private FriendRepository friendRepository;
    private SessionManager sessionManager;

    private static final String TAG = "AddFriendRequestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_friend_request);

        recyclerView = findViewById(R.id.friend_requests_recycler);
        progressBar = findViewById(R.id.friend_requests_progress);

        sessionManager = new SessionManager(this);
        friendRepository = new FriendRepository();

        String currentUserId = sessionManager.getCurrentUserId();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendRequestAdapter(this, null, currentUserId, friendRepository);
        recyclerView.setAdapter(adapter);

        loadFriendRequests(currentUserId);
    }

    private void loadFriendRequests(String userId) {
        progressBar.setVisibility(View.VISIBLE);

        friendRepository.getFriendRequests(userId, new FriendRepository.FriendRequestsCallback() {
            @Override
            public void onSuccess(List<Friendship> requests) {
                runOnUiThread(() -> {
                    adapter = new FriendRequestAdapter(AddFriendRequestActivity.this, requests, userId, friendRepository);
                    recyclerView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddFriendRequestActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Error loading friend requests", t);
            }
        });
    }
}
