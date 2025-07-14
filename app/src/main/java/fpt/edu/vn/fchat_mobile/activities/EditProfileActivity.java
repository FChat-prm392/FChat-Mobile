package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.File;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.models.User;
import fpt.edu.vn.fchat_mobile.repositories.UserRepository;
import fpt.edu.vn.fchat_mobile.responses.UpdateUserResponse;
import fpt.edu.vn.fchat_mobile.utils.FileUtils;
import fpt.edu.vn.fchat_mobile.utils.SessionManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView avatarView;
    private EditText editFullname, editUsername, editPhone, editEmail;
    private AutoCompleteTextView editGender;
    private Button btnSave, btnChangeAvatar;

    private Uri selectedImageUri;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    avatarView.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        initViews();
        setupGenderDropdown();
        loadUserData();

        btnChangeAvatar.setOnClickListener(v -> showImagePickerDialog());
        btnSave.setOnClickListener(v -> updateProfile());
    }

    private void initViews() {
        avatarView = findViewById(R.id.avatar);
        editFullname = findViewById(R.id.edit_fullname);
        editUsername = findViewById(R.id.edit_username);
        editGender = findViewById(R.id.edit_gender);
        editPhone = findViewById(R.id.edit_phone);
        editEmail = findViewById(R.id.edit_email);
        btnSave = findViewById(R.id.btn_save_profile);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        progressDialog = new ProgressDialog(this);
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                new String[]{"Male", "Female", "Other"});
        editGender.setAdapter(adapter);
        editGender.setOnClickListener(v -> editGender.showDropDown());
    }

    private void loadUserData() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        editFullname.setText(user.getFullname());
        editUsername.setText(user.getUsername());
        editGender.setText(user.getGender());
        editPhone.setText(user.getPhoneNumber());
        editEmail.setText(user.getEmail());

        if (user.getImageURL() != null && !user.getImageURL().equals("N/A")) {
            Glide.with(this).load(user.getImageURL())
                    .into(avatarView);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Chọn từ thư viện"};
        new AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickFromGallery();
                }).show();
    }

    private void pickFromGallery() {
        if (!hasGalleryPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1234);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
            }
        } else {
            launchGalleryIntent();
        }
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1234 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchGalleryIntent();
        } else {
            Toast.makeText(this, "Permission denied to access images", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasGalleryPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                : ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateProfile() {
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.show();

        String userId = sessionManager.getCurrentUserId();

        RequestBody fullname = RequestBody.create(MediaType.parse("text/plain"), editFullname.getText().toString());
        RequestBody username = RequestBody.create(MediaType.parse("text/plain"), editUsername.getText().toString());
        RequestBody gender   = RequestBody.create(MediaType.parse("text/plain"), editGender.getText().toString());
        RequestBody phone    = RequestBody.create(MediaType.parse("text/plain"), editPhone.getText().toString());
        RequestBody email    = RequestBody.create(MediaType.parse("text/plain"), editEmail.getText().toString());

        MultipartBody.Part imagePart = null;
        if (selectedImageUri != null) {
            File file = new File(FileUtils.getPath(this, selectedImageUri));
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            imagePart = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
        }

        UserRepository.getInstance().updateUserWithImage(
                userId, imagePart, fullname, username, gender, phone, email,
                new Callback<UpdateUserResponse>() {
                    @Override
                    public void onResponse(Call<UpdateUserResponse> call, Response<UpdateUserResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            sessionManager.saveUserSession(response.body().getUser());
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateUserResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
