package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

public class SenderResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("fullname")
    private String fullname;

    @SerializedName("username")
    private String username;

    @SerializedName("imageURL")
    private String imageURL;

    public String getId() {
        return id;
    }

    public String getFullname() {
        return fullname;
    }

    public String getUsername() {
        return username;
    }

    public String getImageURL() {
        return imageURL;
    }
}
