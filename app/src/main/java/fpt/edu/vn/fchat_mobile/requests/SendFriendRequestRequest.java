package fpt.edu.vn.fchat_mobile.requests;

public class SendFriendRequestRequest {
    private String requesterId;
    private String recipientId;

    public SendFriendRequestRequest() {}

    public SendFriendRequestRequest(String requesterId, String recipientId) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
}
