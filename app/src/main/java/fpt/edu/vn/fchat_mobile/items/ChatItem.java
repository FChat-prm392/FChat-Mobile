package fpt.edu.vn.fchat_mobile.items;

public class ChatItem {
    public String id;
    public String name;
    public String message;
    public String time;
    public String avatarUrl;
    public boolean isOnline;

    public ChatItem(String id, String name, String message, String time, String avatarUrl, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isOnline = isOnline;
    }
}

