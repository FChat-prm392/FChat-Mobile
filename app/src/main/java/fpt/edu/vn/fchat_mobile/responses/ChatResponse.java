package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import fpt.edu.vn.fchat_mobile.models.Participant;

public class ChatResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("groupAvatar")
    private String groupAvatar;

    @SerializedName("createAt")
    private String createAt;

    @SerializedName("participants")
    private List<Participant> participants;

    @SerializedName("isGroup")
    private boolean isGroup;
    @SerializedName("updateAt")
    private String updateAt;

    @SerializedName("lastMessageID")
    private MessageResponse lastMessage;

    public String getId() {
        return id;
    }

    public boolean isGroup() { return isGroup; }
    public List<Participant> getParticipants() { return participants; }

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
