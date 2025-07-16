package fpt.edu.vn.fchat_mobile.responses;

import java.util.List;

import fpt.edu.vn.fchat_mobile.responses.UserResponse;

public class NonFriendsResponse {
    private List<UserResponse> data;

    public List<UserResponse> getData() {
        return data;
    }

    public void setData(List<UserResponse> data) {
        this.data = data;
    }
}