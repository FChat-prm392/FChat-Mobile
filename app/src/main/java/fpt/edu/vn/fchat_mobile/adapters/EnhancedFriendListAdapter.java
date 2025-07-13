package fpt.edu.vn.fchat_mobile.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.activities.ChatDetailActivity;
import fpt.edu.vn.fchat_mobile.models.Account;

public class EnhancedFriendListAdapter extends RecyclerView.Adapter<EnhancedFriendListAdapter.ViewHolder> {
    private Context context;
    private List<Account> friends;
    private String currentUserId;

    public EnhancedFriendListAdapter(Context context, List<Account> friends, String currentUserId) {
        this.context = context;
        this.friends = friends;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account friend = friends.get(position);

        holder.nameText.setText(friend.getFullname());
        holder.usernameText.setText("@" + friend.getUsername());
        
        // Show online status
        if (friend.isOnline()) {
            holder.statusText.setText("Đang hoạt động");
            holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.statusText.setText("Không hoạt động");
            holder.statusText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        // Load avatar
        if (friend.getImageURL() != null && !friend.getImageURL().equals("N/A")) {
            Glide.with(context)
                    .load(friend.getImageURL())
                    .placeholder(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(holder.avatarImage);
        } else {
            holder.avatarImage.setImageResource(R.drawable.ic_avatar);
        }

        // Handle click to start chat
        holder.itemView.setOnClickListener(v -> {
            // Navigate to chat with this friend
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("participantId", friend.get_id());
            intent.putExtra("participantName", friend.getFullname());
            intent.putExtra("participantAvatar", friend.getImageURL());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage, onlineIndicator;
        TextView nameText, usernameText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            onlineIndicator = itemView.findViewById(R.id.online_indicator);
            nameText = itemView.findViewById(R.id.name_text);
            usernameText = itemView.findViewById(R.id.username_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
    }
}
