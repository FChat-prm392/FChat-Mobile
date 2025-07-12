package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fpt.edu.vn.fchat_mobile.R;

public class ForgetPasswordActivity extends AppCompatActivity {

    TextInputLayout emailLayout;
    TextInputEditText emailInput;
    MaterialButton resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        emailLayout = findViewById(R.id.emailLayout);
        emailInput = findViewById(R.id.emailInput);
        resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("Email is required");
            } else {
                emailLayout.setError(null);
                Toast.makeText(this, "Password reset request sent", Toast.LENGTH_SHORT).show();
                // TODO: Call forgot password API
            }
        });
    }
}
