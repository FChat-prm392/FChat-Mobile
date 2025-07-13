package fpt.edu.vn.fchat_mobile.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.items.MessageItem;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<MessageItem> messages;

    public MessageAdapter(List<MessageItem> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByUser() ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageItem message = messages.get(position);

        if (holder instanceof SentViewHolder) {
            SentViewHolder viewHolder = (SentViewHolder) holder;
            if (message.isImage()) {
                viewHolder.text.setVisibility(View.GONE);
                viewHolder.image.setVisibility(View.VISIBLE);
                viewHolder.image.setImageBitmap(message.getImage());
            } else {
                viewHolder.image.setVisibility(View.GONE);
                viewHolder.text.setVisibility(View.VISIBLE);
                viewHolder.text.setText(message.getContent());
            }
            
            // Set timestamp
            viewHolder.timeText.setText(message.getTimestamp());
            
            // Set status icon based on message status
            updateStatusIcon(viewHolder.statusIcon, message.getStatus());
            
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder viewHolder = (ReceivedViewHolder) holder;
            if (message.isImage()) {
                viewHolder.text.setVisibility(View.GONE);
                viewHolder.image.setVisibility(View.VISIBLE);
                viewHolder.image.setImageBitmap(message.getImage());
            } else {
                viewHolder.image.setVisibility(View.GONE);
                viewHolder.text.setVisibility(View.VISIBLE);
                viewHolder.text.setText(message.getContent());
            }
            
            // Set timestamp for received messages
            viewHolder.timeText.setText(message.getTimestamp());
        }
    }
    
    private void updateStatusIcon(ImageView statusIcon, String status) {
        switch (status) {
            case "sent":
                statusIcon.setImageResource(R.drawable.ic_check);
                statusIcon.setColorFilter(android.graphics.Color.parseColor("#999999"));
                break;
            case "delivered":
                statusIcon.setImageResource(R.drawable.ic_double_check);
                statusIcon.setColorFilter(android.graphics.Color.parseColor("#999999"));
                break;
            case "read":
                statusIcon.setImageResource(R.drawable.ic_double_check_read);
                statusIcon.setColorFilter(null); // Remove color filter to show blue/green
                break;
            default:
                statusIcon.setImageResource(R.drawable.ic_check);
                statusIcon.setColorFilter(android.graphics.Color.parseColor("#999999"));
                break;
        }
        statusIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public void updateMessageStatus(String messageId, String status) {
        android.util.Log.d("MessageAdapter", "🔍 SEARCHING FOR MESSAGE - ID: " + messageId + ", Status: " + status);
        
        for (int i = 0; i < messages.size(); i++) {
            MessageItem message = messages.get(i);
            android.util.Log.d("MessageAdapter", "🔎 Checking message " + i + " - ID: " + message.getMessageId() + ", Content: '" + message.getContent() + "'");
            
            if (messageId.equals(message.getMessageId())) {
                String oldStatus = message.getStatus();
                message.setStatus(status);
                android.util.Log.d("MessageAdapter", "✅ FOUND & UPDATED - Position: " + i + ", Old: " + oldStatus + " → New: " + status);
                notifyItemChanged(i);
                return;
            }
        }
        
        android.util.Log.w("MessageAdapter", "❌ MESSAGE NOT FOUND - ID: " + messageId + " (Total messages: " + messages.size() + ")");
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;
        TextView timeText;
        ImageView statusIcon;

        SentViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.sent_text);
            image = itemView.findViewById(R.id.message_image);
            timeText = itemView.findViewById(R.id.message_time);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;
        TextView timeText;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.received_text);
            image = itemView.findViewById(R.id.message_image);
            timeText = itemView.findViewById(R.id.message_time);
        }
    }
}
