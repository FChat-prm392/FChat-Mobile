package fpt.edu.vn.fchat_mobile.models;

public class User {
    private String id;
    private String fullname;
    private String username;
    private String email;
    private String gender;
    private String phoneNumber;
    private String imageURL;
    private boolean status;
    private String createdAt;
    private String updatedAt;

    public String getId() { return id; }
    public String getFullname() { return fullname; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getimageURL() { return imageURL; }
    public boolean isStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
