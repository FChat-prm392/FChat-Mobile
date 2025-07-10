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

public class LoginActivity extends AppCompatActivity {

    TextInputLayout emailLayout, passwordLayout;
    TextInputEditText emailInput, passwordInput;
    MaterialButton loginButton;
    TextView forgetPasswordText, registerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgetPasswordText = findViewById(R.id.forgetPasswordText);
        registerText = findViewById(R.id.registerText);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

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
                // TODO: Replace with API login call
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
            }
        });

        forgetPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
            startActivity(intent);
        });

        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
