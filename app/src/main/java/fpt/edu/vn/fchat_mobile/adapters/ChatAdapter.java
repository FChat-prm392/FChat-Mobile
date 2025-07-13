package fpt.edu.vn.fchat_mobile.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.activities.ChatDetailActivity;
import fpt.edu.vn.fchat_mobile.items.ChatItem;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final String TAG = "ChatAdapter";
    private final Context context;
    private final List<ChatItem> chatList;

    public ChatAdapter(Context context, List<ChatItem> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chat = chatList.get(position);
        holder.name.setText(chat.getName());
        holder.lastMessage.setText(chat.getMessage());
        holder.time.setText(chat.getTime());

        // Load avatar from URL using Glide
        Glide.with(context)
                .load(chat.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar)
                .into(holder.avatar);

        // Show online dot and status for one-on-one chats only
        if (!chat.isGroup()) {
            holder.onlineDot.setVisibility(View.VISIBLE);
            holder.onlineDot.setBackgroundResource(
                    chat.isOnline() ? R.drawable.online_dot : R.drawable.offline_dot
            );
            
            // Display message content only
            holder.lastMessage.setText(chat.getMessage());
            
            // Handle status display in the time field
            if (chat.isOnline()) {
                holder.time.setText("Online");
            } else if (chat.getLastOnline() != null) {
                try {
                    Log.d(TAG, "Parsing lastOnline: " + chat.getLastOnline());
                    // Parse ISO 8601 format (e.g., "2025-07-13T07:06:55.814Z")
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Input is UTC
                    Date lastOnlineDate = sdf.parse(chat.getLastOnline());
                    Log.d(TAG, "Parsed UTC date: " + lastOnlineDate);

                    // Convert to GMT+7
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                    calendar.setTime(lastOnlineDate);
                    long gmtPlus7Time = calendar.getTimeInMillis();
                    Log.d(TAG, "GMT+7 time (ms): " + gmtPlus7Time + ", Current time (ms): " + System.currentTimeMillis());

                    String relativeTime = DateUtils.getRelativeTimeSpanString(
                            gmtPlus7Time,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString();
                    Log.d(TAG, "Relative time: " + relativeTime);

                    // Set Vietnamese locale for translation
                    String translatedTime = DateUtils.getRelativeTimeSpanString(
                            gmtPlus7Time,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString();
                    Log.d(TAG, "Translated relative time (vi): " + translatedTime);

                    holder.time.setText("Last seen " + translatedTime);
                } catch (ParseException e) {
                    Log.e(TAG, "ParseException for lastOnline: " + chat.getLastOnline(), e);
                    holder.time.setText("Last seen " + chat.getLastOnline()); // Debug
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error for lastOnline: " + chat.getLastOnline(), e);
                    holder.time.setText("Offline");
                }
            } else {
                Log.d(TAG, "lastOnline is null for chat: " + chat.getName());
                holder.time.setText("Offline");
            }
        } else {
            holder.onlineDot.setVisibility(View.GONE);
            holder.lastMessage.setText(chat.getMessage());
            holder.time.setText(chat.getTime()); // Show original time for group chats
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("chatId", chat.getId());
            intent.putExtra("name", chat.getName());
            String status;
            if (!chat.isGroup()) {
                if (chat.isOnline()) {
                    status = "Online";
                } else if (chat.getLastOnline() != null) {
                    try {
                        Log.d(TAG, "Parsing lastOnline for status: " + chat.getLastOnline());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date lastOnlineDate = sdf.parse(chat.getLastOnline());
                        Log.d(TAG, "Parsed UTC date for status: " + lastOnlineDate);

                        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        calendar.setTime(lastOnlineDate);
                        long gmtPlus7Time = calendar.getTimeInMillis();
                        Log.d(TAG, "GMT+7 time for status (ms): " + gmtPlus7Time);

                        Locale vietnameseLocale = new Locale("vi", "VN");
                        status = "Last seen " + DateUtils.getRelativeTimeSpanString(
                                gmtPlus7Time,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString();
                        Log.d(TAG, "Status relative time: " + status);
                    } catch (ParseException e) {
                        Log.e(TAG, "ParseException for status: " + chat.getLastOnline(), e);
                        status = "Last seen " + chat.getLastOnline(); // Debug
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error for status: " + chat.getLastOnline(), e);
                        status = "Offline";
                    }
                } else {
                    Log.d(TAG, "lastOnline is null for status: " + chat.getName());
                    status = "Offline";
                }
            } else {
                status = "Group Chat";
            }
            intent.putExtra("status", status);
            intent.putExtra("avatarUrl", chat.getAvatarUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        View onlineDot;
        TextView name, lastMessage, time;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            onlineDot = itemView.findViewById(R.id.online_dot);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.last_message);
            time = itemView.findViewById(R.id.time);
        }
    }
}