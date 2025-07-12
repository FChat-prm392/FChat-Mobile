package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("groupAvatar")
    private String groupAvatar;

    @SerializedName("createAt")
    private String createAt;

    @SerializedName("updateAt")
    private String updateAt;

    @SerializedName("lastMessageID")
    private MessageResponse lastMessage;

    public String getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public String getUpdateAtTime() {
        return updateAt;
    }

    public MessageResponse getLastMessage() {
        return lastMessage;
    }
}
