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
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
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
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Get current user's full name
     */
    public String getCurrentUserFullname() {
        return sharedPreferences.getString(KEY_FULLNAME, null);
    }

    /**
     * Get current user's username
     */
    public String getCurrentUserUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    /**
     * Get current user's email
     */
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get complete user data
     */
    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        // Create a User object with stored data
        // Note: You might need to add setters to your User class or create a constructor
        // For now, I'll create a simple method that returns the basic info
        return null; // You can enhance this based on your User class structure
    }

    /**
     * Clear user session (logout)
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /**
     * Check if user session exists and is valid
     */
    public boolean hasValidSession() {
        return isLoggedIn() && getCurrentUserId() != null;
    }
}
