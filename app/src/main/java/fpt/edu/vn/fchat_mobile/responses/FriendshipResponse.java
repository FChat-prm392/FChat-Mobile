package fpt.edu.vn.fchat_mobile.responses;

import java.util.List;

public class FriendshipResponse {
    private boolean success;
    private String message;
    private List<fpt.edu.vn.fchat_mobile.models.Friendship> data;

    public FriendshipResponse() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<fpt.edu.vn.fchat_mobile.models.Friendship> getData() {
        return data;
    }

    public void setData(List<fpt.edu.vn.fchat_mobile.models.Friendship> data) {
        this.data = data;
    }
}
