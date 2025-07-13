package fpt.edu.vn.fchat_mobile.requests;

import com.google.gson.annotations.SerializedName;

public class GoogleLoginRequest {
    @SerializedName("idToken")
    private String idToken;
    
    @SerializedName("fcmToken")
    private String fcmToken;

    public GoogleLoginRequest(String idToken, String fcmToken) {
        this.idToken = idToken;
        this.fcmToken = fcmToken;
    }

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
        this.fcmToken = null;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @Override
    public String toString() {
        return "GoogleLoginRequest{" +
                "idToken='" + idToken + '\'' +
                ", fcmToken='" + fcmToken + '\'' +
                '}';
    }
}
