package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fpt.edu.vn.fchat_mobile.R;

public class ForgetPasswordActivity2 extends AppCompatActivity {

    TextInputLayout newPasswordLayout, confirmPasswordLayout;
    TextInputEditText newPasswordInput, confirmPasswordInput;
    MaterialButton confirmResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password2);

        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        confirmResetButton = findViewById(R.id.confirmResetButton);

        confirmResetButton.setOnClickListener(v -> {
            String password = newPasswordInput.getText().toString().trim();
            String confirm = confirmPasswordInput.getText().toString().trim();

            boolean isValid = true;

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                newPasswordLayout.setError("Min 6 characters");
                isValid = false;
            } else newPasswordLayout.setError(null);

            if (!password.equals(confirm)) {
                confirmPasswordLayout.setError("Passwords do not match");
                isValid = false;
            } else confirmPasswordLayout.setError(null);

            if (isValid) {
                // TODO: Send password update to backend
                Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show();
                finish(); // or navigate to login
            }
        });
    }
}
