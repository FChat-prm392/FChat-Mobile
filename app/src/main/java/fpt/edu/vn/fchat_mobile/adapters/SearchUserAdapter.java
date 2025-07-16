package fpt.edu.vn.fchat_mobile.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.UserViewHolder> {
    private final Context context;
    private List<FriendRepository.AccountWithStatus> users;
    private final String currentUserId;
    private final OnProfileClickListener profileClickListener;
    private final FriendRepository friendRepository;

    public SearchUserAdapter(Context context, List<FriendRepository.AccountWithStatus> users, String currentUserId,
                             OnProfileClickListener listener, FriendRepository friendRepository) {
        this.context = context;
        this.users = users != null ? users : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.profileClickListener = listener;
        this.friendRepository = friendRepository;
    }

    public void updateUsers(List<FriendRepository.AccountWithStatus> newUsers) {
        this.users = newUsers != null ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        FriendRepository.AccountWithStatus userWithStatus = users.get(position);
        Account user = userWithStatus.getAccount();

        holder.fullNameTextView.setText(user.getFullname() != null ? user.getFullname() : "Unknown");
        holder.usernameTextView.setText(user.getUsername() != null ? user.getUsername() : "");

        if (user.getImageURL() != null && !user.getImageURL().isEmpty()) {
            Glide.with(context)
                    .load(user.getImageURL())
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.ic_avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            if (profileClickListener != null && !user.get_id().equals(currentUserId)) {
                profileClickListener.onProfileClick(user.get_id());
            }
        });

        // Handle "Thêm bạn" button
        holder.addFriendButton.setEnabled(true);
        holder.addFriendButton.setText("Thêm bạn");
        if (!user.get_id().equals(currentUserId)) {
            holder.addFriendButton.setOnClickListener(v -> {
                holder.addFriendButton.setEnabled(false);
                friendRepository.sendFriendRequest(currentUserId, user.get_id(), new FriendRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        holder.addFriendButton.post(() -> {
                            holder.addFriendButton.setText("Pending");
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        holder.addFriendButton.post(() -> {
                            String errorMsg = t.getMessage();
                            if (errorMsg != null && (errorMsg.contains("You already sent a request to this account") ||
                                    errorMsg.contains("Friend request already exists"))) {
                                holder.addFriendButton.setText("Pending");
                            } else {
                                holder.addFriendButton.setText("Thêm bạn");
                                holder.addFriendButton.setEnabled(true);
                            }
                        });
                        Log.e("SearchUserAdapter", "Send friend request failed", t);
                    }
                });
            });
        } else {
            holder.addFriendButton.setVisibility(View.GONE); // Hide button for self
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView fullNameTextView;
        TextView usernameTextView;
        Button addFriendButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatar_image);
            fullNameTextView = itemView.findViewById(R.id.name_text);
            usernameTextView = itemView.findViewById(R.id.username_text);
            addFriendButton = itemView.findViewById(R.id.add_button);
        }
    }

    public interface OnProfileClickListener {
        void onProfileClick(String userId);
    }
}