package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.requests.GoogleLoginRequest;
import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import fpt.edu.vn.fchat_mobile.utils.FirebaseAuthManager;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import fpt.edu.vn.fchat_mobile.utils.SocketManager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements FirebaseAuthManager.AuthListener {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton, googleSignInButton;
    private TextView forgetPasswordText, registerText;

    private ApiService apiService;
    private SessionManager sessionManager;
    private Socket socket;
    private FirebaseAuthManager firebaseAuthManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupSocket();
        setupListeners();
        setupFirebaseAuth();
    }

    private void initViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        forgetPasswordText = findViewById(R.id.forgetPasswordText);
        registerText = findViewById(R.id.registerText);
        apiService = ApiClient.getService();
        sessionManager = new SessionManager(this);
    }

    private void setupSocket() {
        SocketManager.initializeSocket();
        socket = SocketManager.getSocket();

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                });
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Disconnected from server", Toast.LENGTH_SHORT).show();
                });
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

            boolean isValid = true;

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("Email is required");
                isValid = false;
            } else {
                emailLayout.setError(null);
            }

            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError("Password is required");
                isValid = false;
            } else {
                passwordLayout.setError(null);
            }

            if (isValid) {
                login(email, password);
            }
        });

        forgetPasswordText.setOnClickListener(v ->
                startActivity(new Intent(this, ForgetPasswordActivity.class)));

        registerText.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void setupFirebaseAuth() {
        firebaseAuthManager = new FirebaseAuthManager(this, this);
        
        googleSignInButton.setOnClickListener(v -> {
            firebaseAuthManager.signInWithGoogle();
        });
    }

    private void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    // Save user session
                    sessionManager.saveUserSession(response.body().getUser());

                    String userId = response.body().getUser().getId();
                    socket.emit("register-user", userId);

                    String name = response.body().getUser().getFullname();
                    Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void googleLogin(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);
        apiService.googleLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    // Save user session
                    sessionManager.saveUserSession(response.body().getUser());

                    String userId = response.body().getUser().getId();
                    socket.emit("register-user", userId);

                    String name = response.body().getUser().getFullname();
                    Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Google login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Firebase Auth Callbacks
    @Override
    public void onAuthSuccess(String idToken, FirebaseUser user) {
        Toast.makeText(this, "Firebase Authentication successful", Toast.LENGTH_SHORT).show();
        Log.d("LoginActivity", "Firebase user: " + user.getEmail());
        googleLogin(idToken);
    }

    @Override
    public void onAuthFailure(String error) {
        Toast.makeText(this, "Authentication failed: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FirebaseAuthManager.getSignInRequestCode()) {
            firebaseAuthManager.handleSignInResult(requestCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.disconnectSocket();
    }
}