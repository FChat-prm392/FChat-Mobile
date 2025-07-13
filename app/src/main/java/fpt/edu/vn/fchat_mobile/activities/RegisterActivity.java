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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.repositories.AuthRepository;
import fpt.edu.vn.fchat_mobile.responses.RegisterResponse;
import fpt.edu.vn.fchat_mobile.utils.FileUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    private final AuthRepository authRepository = new AuthRepository();

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
                this, android.R.layout.simple_dropdown_item_1line,
                new String[]{"Male", "Female", "Other"});
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
        String fullname = fullNameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String gender = genderInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(fullname)) {
            fullNameLayout.setError("Required"); isValid = false;
        } else fullNameLayout.setError(null);

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Required"); isValid = false;
        } else usernameLayout.setError(null);

        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            emailLayout.setError("Invalid email"); isValid = false;
        } else emailLayout.setError(null);

        if (TextUtils.isEmpty(phone) || phone.length() < 8) {
            phoneLayout.setError("Invalid phone"); isValid = false;
        } else phoneLayout.setError(null);

        if (TextUtils.isEmpty(gender)) {
            genderLayout.setError("Required"); isValid = false;
        } else genderLayout.setError(null);

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Min 6 chars"); isValid = false;
        } else passwordLayout.setError(null);

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords mismatch"); isValid = false;
        } else confirmPasswordLayout.setError(null);

        if (!isValid) return;

        // Prepare file
        File imageFile = null;
        try {
            if (selectedImageUri != null) {
                imageFile = new File(FileUtils.getPath(this, selectedImageUri));
            } else if (selectedCameraBitmap != null) {
                imageFile = FileUtils.saveBitmapToFile(this, selectedCameraBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // RequestBody for form fields
        RequestBody rbFullname = RequestBody.create(MediaType.parse("text/plain"), fullname);
        RequestBody rbUsername = RequestBody.create(MediaType.parse("text/plain"), username);
        RequestBody rbEmail = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody rbPassword = RequestBody.create(MediaType.parse("text/plain"), password);
        RequestBody rbGender = RequestBody.create(MediaType.parse("text/plain"), gender);
        RequestBody rbPhone = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody rbStatus = RequestBody.create(MediaType.parse("text/plain"), "Online.");
        RequestBody rbFcm = RequestBody.create(MediaType.parse("text/plain"), "dummyfcmtoken");

        MultipartBody.Part imagePart = null;
        if (imageFile != null) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
            imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), fileBody);
        }

        authRepository.registerWithImage(
                imagePart, rbFullname, rbUsername, rbEmail, rbPassword,
                rbGender, rbPhone, rbStatus, rbFcm,
                new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == STORAGE_PERMISSION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
