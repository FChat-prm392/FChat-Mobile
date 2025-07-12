package fpt.edu.vn.fchat_mobile.items;

public class ChatItem {
    public String name, message, time;
    public int avatarResId;
    public boolean isOnline;

    public ChatItem(String name, String message, String time, int avatarResId, boolean isOnline) {
        this.name = name;
        this.message = message;
        this.time = time;
        this.avatarResId = avatarResId;
        this.isOnline = isOnline;
    }
}
