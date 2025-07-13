package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

public class MessageResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("senderID")
    private SenderResponse senderID;

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

    public SenderResponse  getSenderID() {
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

    public static class Sender {
        @SerializedName("id")
        private String id;

        @SerializedName("fullname")
        private String fullname;

        @SerializedName("username")
        private String username;

        public String getId() {
            return id;
        }

        public String getFullname() {
            return fullname;
        }

        public String getUsername() {
            return username;
        }
    }
}
