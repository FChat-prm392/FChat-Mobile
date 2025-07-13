package fpt.edu.vn.fchat_mobile.requests;

public class UpdateFriendRequestRequest {
    private String status; // "accepted" or "declined"

    public UpdateFriendRequestRequest() {}

    public UpdateFriendRequestRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
