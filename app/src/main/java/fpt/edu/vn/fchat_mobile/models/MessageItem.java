package fpt.edu.vn.fchat_mobile.models;

public class MessageItem {
    public String content;
    public boolean isSentByUser;

    public MessageItem(String content, boolean isSentByUser) {
        this.content = content;
        this.isSentByUser = isSentByUser;
    }
}
