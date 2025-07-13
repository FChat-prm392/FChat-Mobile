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
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.repositories.FriendRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {

    private final List<Friend> friendList;
    private final Context context;
    private final FriendRepository friendRepository;
    private final String requesterId;

    public FriendListAdapter(Context context, List<Friend> friendList, String requesterId) {
        this.context = context;
        this.friendList = friendList;
        this.requesterId = requesterId;
        this.friendRepository = new FriendRepository();
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

        holder.btnAdd.setOnClickListener(v -> {
            holder.btnAdd.setEnabled(false);
            friendRepository.sendFriendRequest(requesterId, friend.getId(), new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        holder.btnAdd.setText("Đã gửi");
                        Toast.makeText(context, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    } else {
                        holder.btnAdd.setEnabled(true);
                        Toast.makeText(context, "Không gửi được kết bạn", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    holder.btnAdd.setEnabled(true);
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
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
