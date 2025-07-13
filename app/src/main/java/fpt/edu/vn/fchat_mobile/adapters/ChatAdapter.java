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
import fpt.edu.vn.fchat_mobile.views.TypingIndicatorView;

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
        
        if (chat.isTyping()) {
            holder.typingIndicator.setVisibility(View.VISIBLE);
            holder.lastMessage.setVisibility(View.GONE);
            holder.typingIndicator.showTyping(chat.getTypingUser());
        } else {
            holder.typingIndicator.setVisibility(View.GONE);
            holder.lastMessage.setVisibility(View.VISIBLE);
            holder.lastMessage.setText(chat.getMessage());
            holder.typingIndicator.hideTyping();
        }
        
        holder.time.setText(chat.getTime());

        // Load avatar from URL using Glide
        Glide.with(context)
                .load(chat.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar)
                .into(holder.avatar);

        if (!chat.isGroup()) {
            holder.onlineDot.setVisibility(View.VISIBLE);
            holder.onlineDot.setBackgroundResource(
                    chat.isOnline() ? R.drawable.online_dot : R.drawable.offline_dot
            );
            
            if (!chat.isTyping()) {
                holder.lastMessage.setText(chat.getMessage());
            }
            
            if (chat.isOnline()) {
                holder.time.setText("Online");
            } else if (chat.getLastOnline() != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date lastOnlineDate = sdf.parse(chat.getLastOnline());

                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                    calendar.setTime(lastOnlineDate);
                    long gmtPlus7Time = calendar.getTimeInMillis();

                    String relativeTime = DateUtils.getRelativeTimeSpanString(
                            gmtPlus7Time,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString();

                    holder.time.setText("Last seen " + relativeTime);
                } catch (ParseException e) {
                    holder.time.setText("Last seen " + chat.getLastOnline());
                } catch (Exception e) {
                    holder.time.setText("Offline");
                }
            } else {
                holder.time.setText("Offline");
            }
        } else {
            holder.onlineDot.setVisibility(View.GONE);
            if (!chat.isTyping()) {
                holder.lastMessage.setText(chat.getMessage());
            }
            holder.time.setText(chat.getTime());
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
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date lastOnlineDate = sdf.parse(chat.getLastOnline());

                        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        calendar.setTime(lastOnlineDate);
                        long gmtPlus7Time = calendar.getTimeInMillis();

                        status = "Last seen " + DateUtils.getRelativeTimeSpanString(
                                gmtPlus7Time,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString();
                    } catch (ParseException e) {
                        status = "Last seen " + chat.getLastOnline();
                    } catch (Exception e) {
                        status = "Offline";
                    }
                } else {
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

    public void updateTypingStatus(String chatId, boolean isTyping, String typingUser) {
        for (int i = 0; i < chatList.size(); i++) {
            ChatItem chat = chatList.get(i);
            if (chat.getId().equals(chatId)) {
                chat.setTyping(isTyping);
                chat.setTypingUser(typingUser);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        View onlineDot;
        TextView name, lastMessage, time;
        TypingIndicatorView typingIndicator;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            onlineDot = itemView.findViewById(R.id.online_dot);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.last_message);
            time = itemView.findViewById(R.id.time);
            typingIndicator = itemView.findViewById(R.id.typing_indicator);
        }
    }
}