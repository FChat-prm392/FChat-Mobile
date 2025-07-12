package fpt.edu.vn.fchat_mobile.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.models.MessageItem;

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
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        SentViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.sent_text);
            image = itemView.findViewById(R.id.message_image);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.received_text);
            image = itemView.findViewById(R.id.message_image);
        }
    }
}
