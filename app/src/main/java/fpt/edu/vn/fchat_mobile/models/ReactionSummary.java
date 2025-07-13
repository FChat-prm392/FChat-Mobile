package fpt.edu.vn.fchat_mobile.models;

import java.util.List;

public class ReactionSummary {
    private String emoji;
    private int count;
    private List<String> userNames;
    private boolean currentUserReacted;

    public ReactionSummary() {}

    public ReactionSummary(String emoji, int count, List<String> userNames, boolean currentUserReacted) {
        this.emoji = emoji;
        this.count = count;
        this.userNames = userNames;
        this.currentUserReacted = currentUserReacted;
    }

    // Getters and setters
    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(List<String> userNames) {
        this.userNames = userNames;
    }

    public boolean isCurrentUserReacted() {
        return currentUserReacted;
    }

    public void setCurrentUserReacted(boolean currentUserReacted) {
        this.currentUserReacted = currentUserReacted;
    }
}
