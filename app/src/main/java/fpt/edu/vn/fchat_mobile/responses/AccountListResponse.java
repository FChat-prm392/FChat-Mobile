package fpt.edu.vn.fchat_mobile.responses;

import java.util.List;

public class AccountListResponse {
    private boolean success;
    private String message;
    private List<fpt.edu.vn.fchat_mobile.models.Account> data;

    public AccountListResponse() {}

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

    public List<fpt.edu.vn.fchat_mobile.models.Account> getData() {
        return data;
    }

    public void setData(List<fpt.edu.vn.fchat_mobile.models.Account> data) {
        this.data = data;
    }
}
