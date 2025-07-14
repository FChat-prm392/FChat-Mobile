package fpt.edu.vn.fchat_mobile.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.items.MessageItem;
import fpt.edu.vn.fchat_mobile.models.ReactionSummary;

public class ReactionManager {
    
    public interface ReactionCallback {
        void onReactionSelected(String messageId, String emoji, boolean isAdding);
        void onReactionClicked(String messageId, String emoji, List<String> userNames);
    }
    
    private static final List<String> EMOJI_LIST = Arrays.asList("👍", "❤️", "😂", "😮", "😢", "😡");
    
    public static void showReactionPicker(Context context, View anchorView, String messageId, ReactionCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View popupView = inflater.inflate(R.layout.reaction_picker, null);
        
        PopupWindow popupWindow = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );
        
        setupEmojiClickListener(popupView, R.id.emoji_like, "👍", messageId, callback, popupWindow);
        setupEmojiClickListener(popupView, R.id.emoji_love, "❤️", messageId, callback, popupWindow);
        setupEmojiClickListener(popupView, R.id.emoji_laugh, "😂", messageId, callback, popupWindow);
        setupEmojiClickListener(popupView, R.id.emoji_wow, "😮", messageId, callback, popupWindow);
        setupEmojiClickListener(popupView, R.id.emoji_sad, "😢", messageId, callback, popupWindow);
        setupEmojiClickListener(popupView, R.id.emoji_angry, "😡", messageId, callback, popupWindow);
        
        popupWindow.setElevation(8);
        popupWindow.showAsDropDown(anchorView, 0, -anchorView.getHeight() - 200, Gravity.START);
    }
    
    private static void setupEmojiClickListener(View popupView, int viewId, String emoji, 
                                              String messageId, ReactionCallback callback, 
                                              PopupWindow popupWindow) {
        TextView emojiView = popupView.findViewById(viewId);
        emojiView.setOnClickListener(v -> {
            callback.onReactionSelected(messageId, emoji, false);
            popupWindow.dismiss();
        });
    }
    
    public static void displayReactions(FlexboxLayout container, MessageItem message, 
                                      String currentUserId, ReactionCallback callback) {
        container.removeAllViews();
        
        if (!message.hasReactions()) {
            container.setVisibility(View.GONE);
            return;
        }
        
        container.setVisibility(View.VISIBLE);
        Map<String, ReactionSummary> summaries = message.getReactionSummaries();
        
        for (ReactionSummary summary : summaries.values()) {
            if (summary.getCount() > 0) {
                View reactionView = createReactionView(container.getContext(), summary, 
                                                     currentUserId, message.getMessageId(), callback);
                container.addView(reactionView);
            }
        }
    }
    
    private static View createReactionView(Context context, ReactionSummary summary, 
                                         String currentUserId, String messageId, 
                                         ReactionCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View reactionView = inflater.inflate(R.layout.item_reaction, null);
        
        TextView emojiView = reactionView.findViewById(R.id.reaction_emoji);
        TextView countView = reactionView.findViewById(R.id.reaction_count);
        
        emojiView.setText(summary.getEmoji());
        countView.setText(String.valueOf(summary.getCount()));
        
        final boolean currentUserReacted = (currentUserId != null) ? summary.isCurrentUserReacted() : false;
        
        if (currentUserReacted) {
            reactionView.setBackgroundResource(R.drawable.bg_reaction_item_selected);
        } else {
            reactionView.setBackgroundResource(R.drawable.bg_reaction_item);
        }
        
        reactionView.setOnClickListener(v -> {
            if (currentUserReacted) {
                callback.onReactionSelected(messageId, summary.getEmoji(), false);
            } else {
                callback.onReactionSelected(messageId, summary.getEmoji(), true);
            }
        });
        
        reactionView.setOnLongClickListener(v -> {
            callback.onReactionClicked(messageId, summary.getEmoji(), summary.getUserNames());
            return true;
        });
        
        return reactionView;
    }
    
    public static void showReactionUsers(Context context, String emoji, List<String> userNames) {
        StringBuilder message = new StringBuilder();
        message.append(emoji).append(" ");
        
        if (userNames.size() == 1) {
            message.append(userNames.get(0));
        } else if (userNames.size() == 2) {
            message.append(userNames.get(0)).append(" and ").append(userNames.get(1));
        } else if (userNames.size() > 2) {
            message.append(userNames.get(0))
                   .append(", ")
                   .append(userNames.get(1))
                   .append(" and ")
                   .append(userNames.size() - 2)
                   .append(" others");
        }
        
        Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
    }
}
