package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import fpt.edu.vn.fchat_mobile.R;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout fullNameLayout, usernameLayout, emailLayout, phoneLayout, genderLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText fullNameInput, usernameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private AutoCompleteTextView genderInput;
    private MaterialButton registerButton, chooseImageButton, takePhotoButton;
    private ImageView profileImageView;

    private Uri selectedImageUri;
    private Bitmap selectedCameraBitmap;

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    selectedCameraBitmap = null;
                    previewImageFromUri(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedCameraBitmap = (Bitmap) result.getData().getExtras().get("data");
                    selectedImageUri = null;
                    profileImageView.setImageBitmap(selectedCameraBitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupGenderDropdown();

        chooseImageButton.setOnClickListener(v -> requestGalleryPermission());
        takePhotoButton.setOnClickListener(v -> requestCameraPermission());

        registerButton.setOnClickListener(v -> validateAndRegister());
    }

    private void initViews() {
        fullNameLayout = findViewById(R.id.fullNameLayout);
        usernameLayout = findViewById(R.id.usernameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        phoneLayout = findViewById(R.id.phoneLayout);
        genderLayout = findViewById(R.id.genderLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        fullNameInput = findViewById(R.id.fullNameInput);
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        genderInput = findViewById(R.id.genderInput);

        profileImageView = findViewById(R.id.profileImage);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        registerButton = findViewById(R.id.registerButton);
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Male", "Female", "Other"}
        );
        genderInput.setAdapter(genderAdapter);
        genderInput.setOnClickListener(v -> genderInput.showDropDown());
    }

    private void previewImageFromUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            profileImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    private void requestGalleryPermission() {
        if (!hasGalleryPermission()) {
            String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[]{Manifest.permission.READ_MEDIA_IMAGES}
                    : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_REQUEST);
        } else {
            openGallery();
        }
    }

    private boolean hasGalleryPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                : ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void openCamera() {
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void openGallery() {
        galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void validateAndRegister() {
        String fullName = fullNameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String gender = genderInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(fullName)) {
            fullNameLayout.setError("Full Name is required");
            isValid = false;
        } else fullNameLayout.setError(null);

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            isValid = false;
        } else usernameLayout.setError(null);

        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            emailLayout.setError("Valid email required");
            isValid = false;
        } else emailLayout.setError(null);

        if (TextUtils.isEmpty(phone) || phone.length() < 8) {
            phoneLayout.setError("Invalid phone number");
            isValid = false;
        } else phoneLayout.setError(null);

        if (TextUtils.isEmpty(gender)) {
            genderLayout.setError("Please select gender");
            isValid = false;
        } else genderLayout.setError(null);

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Minimum 6 characters");
            isValid = false;
        } else passwordLayout.setError(null);

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else confirmPasswordLayout.setError(null);

        if (selectedImageUri == null && selectedCameraBitmap == null) {
            Toast.makeText(this, "Please add a profile picture", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show();
            // TODO: Send form + image to backend
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == STORAGE_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}