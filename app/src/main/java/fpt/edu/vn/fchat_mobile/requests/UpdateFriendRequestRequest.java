package fpt.edu.vn.fchat_mobile.requests;

public class UpdateFriendRequestRequest {
    private String requestStatus;

    public UpdateFriendRequestRequest(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getRequestStatus() {
        return requestStatus;
    }
}