package fpt.edu.vn.fchat_mobile.requests;

public class RegisterRequest {
    private String fullname;
    private String username;
    private String email;
    private String password;
    private String fcmToken;
    private String gender;
    private String phoneNumber;
    private String imageURL = "N/A"; // default
    private String currentStatus = "active"; // default

    public RegisterRequest(String fullname, String username, String email, String password,
                           String gender, String phoneNumber, String fcmToken, String imageURL, String currentStatus) {
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.fcmToken = fcmToken;
        this.imageURL = imageURL;
        this.currentStatus = currentStatus;
    }
}
