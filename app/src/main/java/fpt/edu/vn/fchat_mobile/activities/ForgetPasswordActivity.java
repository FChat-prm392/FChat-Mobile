package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
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

            if (TextUtils.isEmpty(email) || !email.contains("@")) {
                emailLayout.setError("Enter a valid email");
            } else {
                emailLayout.setError(null);

                // TODO: Verify email with backend
                Toast.makeText(this, "Verification link sent!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, ForgetPasswordActivity2.class);
                intent.putExtra("email", email); // optional
                startActivity(intent);
            }
        });
    }
}