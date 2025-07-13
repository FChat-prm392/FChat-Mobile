package fpt.edu.vn.fchat_mobile.items;

import android.graphics.Bitmap;

public class MessageItem {
    private String content;
    private boolean isSentByUser;
    private String messageId;
    private String status; // "sent", "delivered", "read"
    private String timestamp;

    private Bitmap image;
    private boolean isImage;

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
}
