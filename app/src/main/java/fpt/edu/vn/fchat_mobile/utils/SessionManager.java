package fpt.edu.vn.fchat_mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

import fpt.edu.vn.fchat_mobile.models.User;

public class SessionManager {
    private static final String PREF_NAME = "FChat_Session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Save user session data
     */
    public void saveUserSession(User user) {
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_FULLNAME, user.getFullname());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_GENDER, user.getGender());
        editor.putString(KEY_PHONE_NUMBER, user.getPhoneNumber());
        editor.putString(KEY_AVATAR_URL, user.getImageURL());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getCurrentUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getCurrentUserFullname() {
        return sharedPreferences.getString(KEY_FULLNAME, null);
    }

    public String getCurrentUserUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public String getCurrentUserGender() {
        return sharedPreferences.getString(KEY_GENDER, null);
    }

    public String getCurrentUserPhoneNumber() {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null);
    }

    public String getCurrentUserAvatarUrl() {
        return sharedPreferences.getString(KEY_AVATAR_URL, "N/A");
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean hasValidSession() {
        return isLoggedIn() && getCurrentUserId() != null;
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;

        User user = new User();
        user.setId(getCurrentUserId());
        user.setFullname(getCurrentUserFullname());
        user.setUsername(getCurrentUserUsername());
        user.setEmail(getCurrentUserEmail());
        user.setGender(getCurrentUserGender());
        user.setPhoneNumber(getCurrentUserPhoneNumber());
        user.setImageURL(getCurrentUserAvatarUrl());
        return user;
    }

    public void logout() {
        String userId = getCurrentUserId();
        if (userId != null) {
            SocketManager.initializeSocket();
            SocketManager.emitUserLogout(userId);
        }
        editor.clear();
        editor.apply();
    }
}
