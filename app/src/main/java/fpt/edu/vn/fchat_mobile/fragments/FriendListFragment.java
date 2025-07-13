package fpt.edu.vn.fchat_mobile.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.EnhancedFriendListAdapter;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class FriendListFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private EnhancedFriendListAdapter adapter;
    private List<Account> friendList = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;

    public static FriendListFragment newInstance(String userId) {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getString(ARG_USER_ID);
        }
        friendRepository = new FriendRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_list, container, false);
        
        setupViews(view);
        setupRecyclerView();
        loadFriends();
        
        return view;
    }

    private void setupViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);
        
        swipeRefreshLayout.setOnRefreshListener(() -> loadFriends());
    }

    private void setupRecyclerView() {
        adapter = new EnhancedFriendListAdapter(getContext(), friendList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadFriends() {
        swipeRefreshLayout.setRefreshing(true);
        friendRepository.getFriendList(currentUserId, new FriendRepository.FriendListCallback() {
            @Override
            public void onSuccess(List<Account> friends) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        friendList.clear();
                        friendList.addAll(friends);
                        adapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Lỗi tải danh sách bạn bè: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
