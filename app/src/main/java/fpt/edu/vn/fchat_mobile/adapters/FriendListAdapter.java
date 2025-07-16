package fpt.edu.vn.fchat_mobile.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.activities.ProfileActivity;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {

    private static final String TAG = "FriendListAdapter";
    private final List<Friend> friendList;
    private final Context context;
    private final FriendRepository friendRepository;
    private final String currentUserId;

    public FriendListAdapter(Context context, List<Friend> friendList, String currentUserId) {
        this.context = context;
        this.friendList = friendList;
        this.currentUserId = currentUserId;
        this.friendRepository = new FriendRepository();
        Log.d(TAG, "Initialized with currentUserId: " + (currentUserId != null ? currentUserId : "null"));
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_list, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.fullname.setText(friend.getFullname());
        holder.username.setText("@" + friend.getUsername());
        Glide.with(context).load(friend.getImageURL()).placeholder(R.drawable.ic_avatar).into(holder.avatar);

        holder.btnAdd.setVisibility(View.VISIBLE);
        holder.btnAdd.setEnabled(true);
        holder.btnAdd.setText("Kết bạn");

        // Set item click to open profile
        holder.itemView.setOnClickListener(v -> openProfile(friend.getId(), position));

        holder.btnAdd.setOnClickListener(v -> {
            holder.btnAdd.setEnabled(false);
            friendRepository.sendFriendRequest(currentUserId, friend.getId(), new FriendRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        holder.btnAdd.setText("Đã gửi");
                        holder.btnAdd.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.darker_gray));
                        Toast.makeText(context, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Throwable t) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        holder.btnAdd.setEnabled(true);
                        String errorMessage = t != null ? t.getMessage() : "Lỗi không xác định";
                        Toast.makeText(context, "Không gửi được kết bạn: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    private void openProfile(String userId, int position) {
        Log.d(TAG, "Opening profile for userId: " + (userId != null ? userId : "null") + " at position " + position + ", currentUserId: " + (currentUserId != null ? currentUserId : "null"));
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("currentUserId", currentUserId);
        if (userId != null && currentUserId != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView fullname, username;
        ImageView avatar;
        Button btnAdd;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            fullname = itemView.findViewById(R.id.fullname);
            username = itemView.findViewById(R.id.username);
            avatar = itemView.findViewById(R.id.avatar);
            btnAdd = itemView.findViewById(R.id.btn_add);
        }
    }
}