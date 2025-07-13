package fpt.edu.vn.fchat_mobile.items;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fpt.edu.vn.fchat_mobile.models.MessageReaction;
import fpt.edu.vn.fchat_mobile.models.ReactionSummary;

public class MessageItem {
    private String content;
    private boolean isSentByUser;
    private String messageId;
    private String status; // "sent", "delivered", "read"
    private String timestamp;

    private Bitmap image;
    private boolean isImage;
    
    // Reaction support
    private List<MessageReaction> reactions = new ArrayList<>();
    private Map<String, ReactionSummary> reactionSummaries = new HashMap<>();

    // ✅ Text message constructor
    public MessageItem(String content, boolean isSentByUser) {
        this.content = content;
        this.isSentByUser = isSentByUser;
        this.isImage = false;
        this.status = "sent";
        this.timestamp = getCurrentTime();
    }

    // ✅ Text message constructor with ID
    public MessageItem(String content, boolean isSentByUser, String messageId) {
        this.content = content;
        this.isSentByUser = isSentByUser;
        this.isImage = false;
        this.messageId = messageId;
        this.status = "sent";
        this.timestamp = getCurrentTime();
    }

    // ✅ Text message constructor with ID and timestamp
    public MessageItem(String content, boolean isSentByUser, String messageId, String timestamp) {
        this.content = content;
        this.isSentByUser = isSentByUser;
        this.isImage = false;
        this.messageId = messageId;
        this.status = "sent";
        this.timestamp = timestamp;
    }

    // ✅ Image message constructor
    public MessageItem(Bitmap image) {
        this.image = image;
        this.isImage = true;
        this.status = "sent";
        this.timestamp = getCurrentTime();
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // Getters and setters
    public boolean isImage() {
        return isImage;
    }

    public String getContent() {
        return content;
    }

    public boolean isSentByUser() {
        return isSentByUser;
    }

    public Bitmap getImage() {
        return image;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    // Reaction methods
    public List<MessageReaction> getReactions() {
        return reactions;
    }
    
    public void setReactions(List<MessageReaction> reactions) {
        this.reactions = reactions;
        updateReactionSummaries(null);
    }
    
    public void setReactions(List<MessageReaction> reactions, String currentUserId) {
        this.reactions = reactions;
        updateReactionSummaries(currentUserId);
    }
    
    public void addReaction(MessageReaction reaction) {
        addReaction(reaction, null);
    }
    
    public void addReaction(MessageReaction reaction, String currentUserId) {
        if (reaction == null) {
            return;
        }
        if (reactions == null) {
            reactions = new ArrayList<>();
        }
        try {
            reactions.add(reaction);
            updateReactionSummaries(currentUserId);
        } catch (Exception e) {
            // Ignore error
        }
    }
    
    public void removeReaction(String userId, String emoji) {
        removeReaction(userId, emoji, null);
    }
    
    public void removeReaction(String userId, String emoji, String currentUserId) {
        if (reactions != null && userId != null && emoji != null) {
            try {
                reactions.removeIf(reaction -> {
                    if (reaction == null) return false;
                    String reactionUserId = reaction.getUserId();
                    String reactionEmoji = reaction.getEmoji();
                    return reactionUserId != null && reactionEmoji != null &&
                           reactionUserId.equals(userId) && reactionEmoji.equals(emoji);
                });
                updateReactionSummaries(currentUserId);
            } catch (Exception e) {
                // Ignore error
            }
        }
    }
    
    public Map<String, ReactionSummary> getReactionSummaries() {
        return reactionSummaries;
    }
    
    public boolean hasReactions() {
        return reactions != null && !reactions.isEmpty();
    }
    
    public boolean hasUserReacted(String userId, String emoji) {
        if (reactions == null || userId == null || emoji == null) return false;
        try {
            return reactions.stream().anyMatch(reaction -> {
                if (reaction == null) return false;
                String reactionUserId = reaction.getUserId();
                String reactionEmoji = reaction.getEmoji();
                return reactionUserId != null && reactionEmoji != null &&
                       reactionUserId.equals(userId) && reactionEmoji.equals(emoji);
            });
        } catch (Exception e) {
            return false;
        }
    }
    
    private void updateReactionSummaries() {
        updateReactionSummaries(null);
    }
    
    private void updateReactionSummaries(String currentUserId) {
        try {
            reactionSummaries.clear();
            if (reactions == null) return;
            
            Map<String, List<MessageReaction>> groupedReactions = new HashMap<>();
            for (MessageReaction reaction : reactions) {
                if (reaction != null && reaction.getEmoji() != null) {
                    groupedReactions.computeIfAbsent(reaction.getEmoji(), k -> new ArrayList<>()).add(reaction);
                }
            }
            
            for (Map.Entry<String, List<MessageReaction>> entry : groupedReactions.entrySet()) {
                String emoji = entry.getKey();
                List<MessageReaction> emojiReactions = entry.getValue();
                
                if (emoji == null || emojiReactions == null) continue;
                
                List<String> userNames = new ArrayList<>();
                boolean currentUserReacted = false;
                
                for (MessageReaction reaction : emojiReactions) {
                    if (reaction != null) {
                        String userName = reaction.getUserName();
                        if (userName != null) {
                            userNames.add(userName);
                        }
                        if (currentUserId != null && reaction.getUserId() != null && 
                            reaction.getUserId().equals(currentUserId)) {
                            currentUserReacted = true;
                        }
                    }
                }
                
                ReactionSummary summary = new ReactionSummary(emoji, emojiReactions.size(), userNames, currentUserReacted);
                reactionSummaries.put(emoji, summary);
            }
        } catch (Exception e) {
            // Ignore error
        }
    }
}
