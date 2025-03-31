package com.codingworld.service1.controller;

public class LoginResponse {
    private String userName;
    private String email;
    private String profilePic;
    private String status;
    private String userFlag;
    private Boolean isUserActive;

    public LoginResponse(String userName, String email, String profilePic, String status, String userFlag, Boolean isUserActive) {
        this.userName = userName;
        this.email = email;
        this.profilePic = profilePic;
        this.status = status;
        this.userFlag = userFlag;
        this.isUserActive = isUserActive;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserFlag() {
        return userFlag;
    }

    public void setUserFlag(String userFlag) {
        this.userFlag = userFlag;
    }

    public Boolean getUserActive() {
        return isUserActive;
    }

    public void setUserActive(Boolean userActive) {
        isUserActive = userActive;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", status='" + status + '\'' +
                ", userFlag='" + userFlag + '\'' +
                ", isUserActive=" + isUserActive +
                '}';
    }
}
