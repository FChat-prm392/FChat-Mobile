package fpt.edu.vn.fchat_mobile.responses;

import com.google.gson.annotations.SerializedName;

public class GeminiResponse {
    @SerializedName("reply")
    private String text;

    public String getText() {
        return text;
    }
}