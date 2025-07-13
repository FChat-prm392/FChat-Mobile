package fpt.edu.vn.fchat_mobile.items;

public class ChatItem {
    public String id;
    public String name;
    public String message;
    public String time;
    public String avatarUrl;
    public boolean isGroup;
    public boolean isOnline;
    public String lastOnline;

    public ChatItem(String id, String name, String message, String time, String avatarUrl, boolean isOnline, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isOnline = isOnline;
        this.isGroup = isGroup;
        this.lastOnline = null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isOnline() { return isOnline; }
    public boolean isGroup() { return isGroup; }
    public String getLastOnline() { return lastOnline; }
    public void setOnline(boolean isOnline) { this.isOnline = isOnline; }
    public void setLastOnline(String lastOnline) { this.lastOnline = lastOnline; }
    public void setMessage(String message) { this.message = message; }
    public void setTime(String time) { this.time = time; }
}