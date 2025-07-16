package fpt.edu.vn.fchat_mobile.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository.SimpleCallback;
import fpt.edu.vn.fchat_mobile.requests.UpdateFriendRequestRequest;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private final Context context;
    private List<Friendship> friendRequests;
    private final String currentUserId;
    private final FriendRepository friendRepository;

    public FriendRequestAdapter(Context context, List<Friendship> friendRequests, String currentUserId, FriendRepository friendRepository) {
        this.context = context;
        this.friendRequests = friendRequests != null ? friendRequests : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.friendRepository = friendRepository;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        Friendship request = friendRequests.get(position);
        Object requester = request.getRequester();
        String requesterName = null;
        if (requester instanceof String) {
            requesterName = (String) requester; // Use the String as the name if provided
        } else if (requester instanceof Account) {
            Account account = (Account) requester;
            requesterName = account.getFullname(); // Use fullname from Account
        }
        holder.requesterName.setText("From: " + (requesterName != null ? requesterName : "Unknown Requester"));

        holder.btnAccept.setOnClickListener(v -> {
            updateFriendRequestStatus(request.getId(), "accepted", holder);
            holder.btnAccept.setEnabled(false);
            holder.btnRefuse.setEnabled(false);
        });

        holder.btnRefuse.setOnClickListener(v -> {
            updateFriendRequestStatus(request.getId(), "declined", holder);
            holder.btnAccept.setEnabled(false);
            holder.btnRefuse.setEnabled(false);
        });
    }

    private void updateFriendRequestStatus(String friendshipId, String status, FriendRequestViewHolder holder) {
        friendRepository.updateFriendRequest(friendshipId, new UpdateFriendRequestRequest(status), new SimpleCallback() {
            @Override
            public void onSuccess() {
                for (int i = friendRequests.size() - 1; i >= 0; i--) {
                    if (friendRequests.get(i).getId().equals(friendshipId)) {
                        friendRequests.remove(i);
                        notifyDataSetChanged();
                        break;
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                    if (holder != null) {
                        holder.btnAccept.setEnabled(true);
                        holder.btnRefuse.setEnabled(true);
                    }
                });
                Log.e("FriendRequestAdapter", "Update failed", t);
            }
        });
    }

    // Placeholder method to get username (replace with actual implementation)
    private String getUserName(String userId) {
        // Fetch username from API or local data
        return "User_" + (userId != null ? userId.substring(0, Math.min(6, userId.length())) : "Unknown"); // Dummy implementation
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView requesterName;
        Button btnAccept, btnRefuse;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            requesterName = itemView.findViewById(R.id.name_text);
            btnAccept = itemView.findViewById(R.id.accept_button);
            btnRefuse = itemView.findViewById(R.id.decline_button);
        }
    }
}