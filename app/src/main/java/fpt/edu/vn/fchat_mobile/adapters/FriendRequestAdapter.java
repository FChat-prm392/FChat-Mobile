package fpt.edu.vn.fchat_mobile.adapters;

import android.content.Context;
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
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private Context context;
    private List<Friendship> friendRequests;
    private String currentUserId;
    private FriendRepository friendRepository;

    public FriendRequestAdapter(Context context, List<Friendship> friendRequests, String currentUserId) {
        this.context = context;
        this.friendRequests = friendRequests;
        this.currentUserId = currentUserId;
        this.friendRepository = new FriendRepository();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friendship friendRequest = friendRequests.get(position);
        
        // Get the other user (requester if current user is recipient)
        String otherUserName = friendRequest.getRequester().getFullname();
        String otherUserAvatar = friendRequest.getRequester().getImageURL();
        String otherUserStatus = friendRequest.getRequester().getCurrentStatus();

        holder.nameText.setText(otherUserName);
        holder.statusText.setText(otherUserStatus != null ? otherUserStatus : "Available");

        // Load avatar
        if (otherUserAvatar != null && !otherUserAvatar.equals("N/A")) {
            Glide.with(context)
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(holder.avatarImage);
        } else {
            holder.avatarImage.setImageResource(R.drawable.ic_avatar);
        }

        // Handle accept button
        holder.acceptButton.setOnClickListener(v -> {
            friendRepository.acceptFriendRequest(friendRequest.getId(), new FriendRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(context, "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    friendRequests.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, friendRequests.size());
                }

                @Override
                public void onError(Throwable t) {
                    Toast.makeText(context, "Lỗi khi chấp nhận: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Handle decline button
        holder.declineButton.setOnClickListener(v -> {
            friendRepository.declineFriendRequest(friendRequest.getId(), new FriendRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(context, "Đã từ chối lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    friendRequests.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, friendRequests.size());
                }

                @Override
                public void onError(Throwable t) {
                    Toast.makeText(context, "Lỗi khi từ chối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView nameText, statusText;
        Button acceptButton, declineButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            nameText = itemView.findViewById(R.id.name_text);
            statusText = itemView.findViewById(R.id.status_text);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}
