package fpt.edu.vn.fchat_mobile.models;

public class LoginResponse {
    private boolean success;
    private String token;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
