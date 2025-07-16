package fpt.edu.vn.fchat_mobile.responses;

public class UserResponse {
    private String id;
    private String username;
    private String imageURL;
    private String fullname;
    private String email;
    private String gender;
    private String phoneNumber;
    private String currentStatus;
    private boolean status;
    private String createdAt;
    private String updatedAt;
    private String friendshipStatus;
    private String lastOnline;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return imageURL; }
    public void setAvatarUrl(String imageURL) { this.imageURL = imageURL; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getLastOnline() { return lastOnline; }
    public void setLastOnline(String lastOnline) { this.lastOnline = lastOnline; }
}