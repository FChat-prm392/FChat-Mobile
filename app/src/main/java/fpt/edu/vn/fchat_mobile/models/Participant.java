package fpt.edu.vn.fchat_mobile.models;

import com.google.gson.annotations.SerializedName;

public class Participant {
    @SerializedName("_id")
    private String id;

    @SerializedName("fullname")
    private String fullname;

    @SerializedName("username")
    private String username;

    public String getId() { return id; }
    public String getFullname() { return fullname; }
    public String getUsername() { return username; }
}

