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
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository.SimpleCallback;

public class BlockListAdapter extends RecyclerView.Adapter<BlockListAdapter.BlockListViewHolder> {

    private final Context context;
    private List<Account> blockedUsers;
    private final String currentUserId;
    private final FriendRepository friendRepository;

    public BlockListAdapter(Context context, List<Account> blockedUsers, String currentUserId, FriendRepository friendRepository) {
        this.context = context;
        this.blockedUsers = blockedUsers != null ? blockedUsers : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.friendRepository = friendRepository;
    }

    @NonNull
    @Override
    public BlockListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new BlockListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockListViewHolder holder, int position) {
        Account blockedUser = blockedUsers.get(position);
        holder.blockedUserName.setText("Blocked: " + (blockedUser.getFullname() != null ? blockedUser.getFullname() : "Unknown User"));

        holder.btnUnblock.setOnClickListener(v -> {
            unblockUser(blockedUser.get_id(), holder);
            holder.btnUnblock.setEnabled(false);
        });
    }

    private void unblockUser(String targetId, BlockListViewHolder holder) {
        friendRepository.unblockUser(currentUserId, targetId, new SimpleCallback() {
            @Override
            public void onSuccess() {
                for (int i = blockedUsers.size() - 1; i >= 0; i--) {
                    if (blockedUsers.get(i).get_id().equals(targetId)) {
                        blockedUsers.remove(i);
                        notifyDataSetChanged();
                        break;
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Failed to unblock user", Toast.LENGTH_SHORT).show();
                    if (holder != null) {
                        holder.btnUnblock.setEnabled(true);
                    }
                });
                Log.e("BlockListAdapter", "Unblock failed", t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }

    public static class BlockListViewHolder extends RecyclerView.ViewHolder {
        TextView blockedUserName;
        Button btnUnblock;

        public BlockListViewHolder(@NonNull View itemView) {
            super(itemView);
            blockedUserName = itemView.findViewById(R.id.blocked_name_text);
            btnUnblock = itemView.findViewById(R.id.unblock_button);
        }
    }
}