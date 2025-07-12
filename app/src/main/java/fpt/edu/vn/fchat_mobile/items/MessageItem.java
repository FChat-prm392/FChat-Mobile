package fpt.edu.vn.fchat_mobile.items;

import android.graphics.Bitmap;

public class MessageItem {
    private String content;
    private boolean isSentByUser;

    private Bitmap image;
    private boolean isImage;

    // ✅ Text message constructor
    public MessageItem(String content, boolean isSentByUser) {
        this.content = content;
        this.isSentByUser = isSentByUser;
        this.isImage = false;
    }

    // ✅ Image message constructor
    public MessageItem(Bitmap image) {
        this.image = image;
        this.isImage = true;
    }

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
}
