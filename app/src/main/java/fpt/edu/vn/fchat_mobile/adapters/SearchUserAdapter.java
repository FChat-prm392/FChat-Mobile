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
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {
    private Context context;
    private List<FriendRepository.AccountWithStatus> usersWithStatus;
    private String currentUserId;
    private FriendRepository friendRepository;

    public SearchUserAdapter(Context context, List<FriendRepository.AccountWithStatus> usersWithStatus, String currentUserId) {
        this.context = context;
        this.usersWithStatus = usersWithStatus;
        this.currentUserId = currentUserId;
        this.friendRepository = new FriendRepository();
    }

    public void updateUsers(List<FriendRepository.AccountWithStatus> newUsersWithStatus) {
        this.usersWithStatus = newUsersWithStatus;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            FriendRepository.AccountWithStatus userWithStatus = usersWithStatus.get(position);
            
            if (userWithStatus == null || userWithStatus.getAccount() == null) {
                return;
            }

            Account user = userWithStatus.getAccount();
            FriendRepository.FriendshipStatus status = userWithStatus.getStatus();

            // Set name with null check
            String fullName = user.getFullname();
            holder.nameText.setText(fullName != null ? fullName : "Unknown User");
            
            // Set username with null check
            String username = user.getUsername();
            holder.usernameText.setText(username != null ? "@" + username : "@unknown");
            
            // Set status with null check
            String userStatus = user.getCurrentStatus();
            holder.statusText.setText(userStatus != null && !userStatus.isEmpty() ? userStatus : "Available");

            // Load avatar with null checks
            String imageUrl = user.getImageURL();
            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("N/A")) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .circleCrop()
                        .into(holder.avatarImage);
            } else {
                holder.avatarImage.setImageResource(R.drawable.ic_avatar);
            }

            // Configure button based on friendship status
            String userId = user.get_id();
            switch (status) {
                case SELF:
                    holder.addButton.setVisibility(View.GONE);
                    holder.statusText.setText("Đây là bạn");
                    break;
                    
                case FRIENDS:
                    holder.addButton.setVisibility(View.VISIBLE);
                    holder.addButton.setText("Bạn bè");
                    holder.addButton.setEnabled(false);
                    holder.addButton.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                    break;
                    
                case PENDING:
                    holder.addButton.setVisibility(View.VISIBLE);
                    holder.addButton.setText("Đã gửi");
                    holder.addButton.setEnabled(false);
                    holder.addButton.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                    break;
                    
                case NOT_FRIENDS:
                    holder.addButton.setVisibility(View.VISIBLE);
                    holder.addButton.setText("Thêm bạn");
                    holder.addButton.setEnabled(true);
                    holder.addButton.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                      // Handle add friend button click
                    holder.addButton.setOnClickListener(v -> {
                        if (userId == null || currentUserId == null) {
                            Toast.makeText(context, "Lỗi: Không thể gửi lời mời", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        holder.addButton.setEnabled(false);
                        holder.addButton.setText("Đang gửi...");
                        
                        friendRepository.sendFriendRequest(currentUserId, userId, new FriendRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (context != null) {
                                    Toast.makeText(context, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                                    holder.addButton.setText("Đã gửi");
                                    holder.addButton.setEnabled(false);
                                    holder.addButton.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (context != null) {
                                    String errorMsg = t != null ? t.getMessage() : "Lỗi không xác định";
                                    Toast.makeText(context, "Lỗi khi gửi lời mời: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    holder.addButton.setText("Thêm bạn");
                                    holder.addButton.setEnabled(true);
                                    holder.addButton.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                                }
                            }
                        });
                    });
                    break;
            }
        } catch (Exception e) {
            if (context != null) {
                Toast.makeText(context, "Lỗi hiển thị người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public int getItemCount() {
        return usersWithStatus != null ? usersWithStatus.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView nameText, usernameText, statusText;
        Button addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            nameText = itemView.findViewById(R.id.name_text);
            usernameText = itemView.findViewById(R.id.username_text);
            statusText = itemView.findViewById(R.id.status_text);
            addButton = itemView.findViewById(R.id.add_button);
        }
    }
}
