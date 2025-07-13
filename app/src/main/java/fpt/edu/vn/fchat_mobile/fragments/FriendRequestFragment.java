package fpt.edu.vn.fchat_mobile.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import fpt.edu.vn.fchat_mobile.adapters.FriendRequestAdapter;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class FriendRequestFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private FriendRequestAdapter adapter;
    private List<Friendship> friendRequestList = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;

    public static FriendRequestFragment newInstance(String userId) {
        FriendRequestFragment fragment = new FriendRequestFragment();
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
        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);
        
        setupViews(view);
        setupRecyclerView();
        loadFriendRequests();
        
        return view;
    }

    private void setupViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyText = view.findViewById(R.id.empty_text);
        
        swipeRefreshLayout.setOnRefreshListener(() -> loadFriendRequests());
    }

    private void setupRecyclerView() {
        adapter = new FriendRequestAdapter(getContext(), friendRequestList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadFriendRequests() {
        swipeRefreshLayout.setRefreshing(true);
        friendRepository.getFriendRequests(currentUserId, new FriendRepository.FriendRequestsCallback() {
            @Override
            public void onSuccess(List<Friendship> friendRequests) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        friendRequestList.clear();
                        
                        // Filter to only show pending requests where current user is recipient
                        for (Friendship request : friendRequests) {
                            if (request.isPending() && request.isRecipient(currentUserId)) {
                                friendRequestList.add(request);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        // Show/hide empty state
                        if (friendRequestList.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyText.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Lỗi tải lời mời kết bạn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
