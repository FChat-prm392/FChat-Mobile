package fpt.edu.vn.fchat_mobile.requests;

import com.google.gson.annotations.SerializedName;

public class SendFriendRequestRequest {
    @SerializedName("requester")
    private String requester;

    @SerializedName("recipient")
    private String recipient;

    @SerializedName("requestStatus")
    private String requestStatus;

    public SendFriendRequestRequest() {}

    public SendFriendRequestRequest(String requester, String recipient, String requestStatus) {
        this.requester = requester;
        this.recipient = recipient;
        this.requestStatus = requestStatus;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}