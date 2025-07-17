package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.BlockListAdapter;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository.BlockedUsersCallback;

public class BlockListActivity extends AppCompatActivity {

    private RecyclerView blockListRecycler;
    private BlockListAdapter blockListAdapter;
    private FriendRepository friendRepository;
    private String currentUserId = "68727d12f34219a6bffbf504"; // Replace with actual user ID logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_block_list);

        // Initialize repository and adapter
        friendRepository = new FriendRepository();
        blockListRecycler = findViewById(R.id.block_list_recycler);
        if (blockListRecycler != null) {
            blockListRecycler.setLayoutManager(new LinearLayoutManager(this));
            blockListAdapter = new BlockListAdapter(this, new ArrayList<>(), currentUserId, friendRepository);
            blockListRecycler.setAdapter(blockListAdapter);
        } else {
            Log.e("BlockListActivity", "RecyclerView not found");
        }

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Fetch blocked users
        fetchBlockedUsers();
    }

    private void fetchBlockedUsers() {
        friendRepository.getBlockedUsers(currentUserId, new BlockedUsersCallback() {
            @Override
            public void onSuccess(List<Account> blockedUsers) {
                runOnUiThread(() -> {
                    if (blockListAdapter != null) {
                        blockListAdapter = new BlockListAdapter(BlockListActivity.this, blockedUsers, currentUserId, friendRepository);
                        blockListRecycler.setAdapter(blockListAdapter);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    Log.e("BlockListActivity", "Error fetching blocked users: " + t.getMessage(), t);
                    Toast.makeText(BlockListActivity.this, "Failed to load block list", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}