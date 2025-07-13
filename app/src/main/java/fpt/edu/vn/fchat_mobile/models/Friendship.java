package fpt.edu.vn.fchat_mobile.models;

import java.util.Date;

public class Friendship {
    private String id;  // Note: your API uses "id" not "_id"
    private Account requester;
    private Account recipient;
    private String requestStatus; // Your API uses "requestStatus" not "status"
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Friendship() {}

    public Friendship(String id, Account requester, Account recipient, String requestStatus, Date createdAt, Date updatedAt) {
        this.id = id;
        this.requester = requester;
        this.recipient = recipient;
        this.requestStatus = requestStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account getRequester() {
        return requester;
    }

    public void setRequester(Account requester) {
        this.requester = requester;
    }

    public Account getRecipient() {
        return recipient;
    }

    public void setRecipient(Account recipient) {
        this.recipient = recipient;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isPending() {
        return "pending".equals(requestStatus);
    }

    public boolean isAccepted() {
        return "accepted".equals(requestStatus);
    }

    public boolean isDeclined() {
        return "declined".equals(requestStatus);
    }

    /**
     * Get the other user in this friendship (not the current user)
     */
    public Account getOtherUser(String currentUserId) {
        if (requester != null && currentUserId.equals(requester.get_id())) {
            return recipient;
        } else {
            return requester;
        }
    }

    /**
     * Check if current user is the requester
     */
    public boolean isRequester(String currentUserId) {
        return requester != null && currentUserId.equals(requester.get_id());
    }

    /**
     * Check if current user is the recipient
     */
    public boolean isRecipient(String currentUserId) {
        return recipient != null && currentUserId.equals(recipient.get_id());
    }
}
