package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

public class SendMessageResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("senderID")
    private String senderID; // This is a string when sending

    @SerializedName("chatID")
    private String chatID;

    @SerializedName("text")
    private String text;

    @SerializedName("messageStatus")
    private String messageStatus;

    @SerializedName("createAt")
    private String createAt;

    public String getId() {
        return id;
    }

    public String getSenderID() {
        return senderID;
    }

    public String getChatID() {
        return chatID;
    }

    public String getText() {
        return text;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public String getCreateAt() {
        return createAt;
    }
}
