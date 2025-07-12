package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.repositories.AuthRepository;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView forgetPasswordText, registerText;

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgetPasswordText = findViewById(R.id.forgetPasswordText);
        registerText = findViewById(R.id.registerText);
        authRepository = new AuthRepository();
        sessionManager = new SessionManager(this);
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

    private void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        authRepository.login(request, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    // Save user session
                    sessionManager.saveUserSession(response.body().getUser());
                    
                    String name = response.body().getUser().getFullname();
                    Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                    finish();
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
}
