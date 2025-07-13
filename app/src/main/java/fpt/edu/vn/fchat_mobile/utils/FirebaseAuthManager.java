package fpt.edu.vn.fchat_mobile.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import fpt.edu.vn.fchat_mobile.R;

public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static final int RC_SIGN_IN = 9001;
    
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Activity activity;
    private AuthListener listener;

    public interface AuthListener {
        void onAuthSuccess(String idToken, FirebaseUser user);
        void onAuthFailure(String error);
    }

    public FirebaseAuthManager(Activity activity, AuthListener listener) {
        this.activity = activity;
        this.listener = listener;
        initializeFirebaseAuth();
    }

    private void initializeFirebaseAuth() {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(int requestCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign In successful, authenticating with Firebase");
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign In failed: " + e.getStatusCode(), e);
                listener.onAuthFailure("Google Sign In failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Get fresh ID token for backend
                            user.getIdToken(true)
                                    .addOnCompleteListener(tokenTask -> {
                                        if (tokenTask.isSuccessful()) {
                                            String token = tokenTask.getResult().getToken();
                                            listener.onAuthSuccess(token, user);
                                        } else {
                                            listener.onAuthFailure("Failed to get ID token");
                                        }
                                    });
                        }
                    } else {
                        Log.e(TAG, "Firebase Authentication failed", task.getException());
                        listener.onAuthFailure("Authentication failed: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    public void signOut() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Sign out from Google
        mGoogleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            Log.d(TAG, "Sign out completed");
        });
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public static int getSignInRequestCode() {
        return RC_SIGN_IN;
    }
}
