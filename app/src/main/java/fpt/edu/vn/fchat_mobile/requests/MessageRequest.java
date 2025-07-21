package fpt.edu.vn.fchat_mobile.requests;
import com.google.gson.annotations.SerializedName;

public class MessageRequest {
    @SerializedName("message")
    private String message;

    @SerializedName("context")
    private String context;

    public MessageRequest(String message, String context) {
        this.message = message;
        this.context = context;
    }

    public MessageRequest(String message) {
        this.message = message;
        this.context = "";
    }

    public String getMessage() {
        return message;
    }

    public String getContext() {
        return context;
    }
}