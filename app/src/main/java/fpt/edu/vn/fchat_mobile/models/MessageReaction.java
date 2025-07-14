package fpt.edu.vn.fchat_mobile.models;

public class MessageReaction {
    private String id;
    private String messageId;
    private String userId;
    private String userName;
    private String emoji;
    private String createdAt;

    public MessageReaction() {}

    public MessageReaction(String messageId, String userId, String userName, String emoji) {
        this.messageId = messageId;
        this.userId = userId;
        this.userName = userName;
        this.emoji = emoji;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
