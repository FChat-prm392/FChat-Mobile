package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.EnhancedFriendListAdapter;
import fpt.edu.vn.fchat_mobile.adapters.FriendRequestAdapter;
import fpt.edu.vn.fchat_mobile.adapters.FriendsPagerAdapter;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;

public class FriendListActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FriendsPagerAdapter pagerAdapter;
    private SessionManager sessionManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list_enhanced);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getCurrentUserId();
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        setupViewPager();
    }

    private void setupViews() {
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bạn bè");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViewPager() {
        pagerAdapter = new FriendsPagerAdapter(this, currentUserId);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Bạn bè");
                    break;
                case 1:
                    tab.setText("Lời mời");
                    break;
            }
        }).attach();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
