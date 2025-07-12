package fpt.edu.vn.fchat_mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;

import fpt.edu.vn.fchat_mobile.R;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView avatarView;
    private EditText editName;
    private Button btnSave;

    private SharedPreferences prefs;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        prefs.edit().putString("avatar_uri", imageUri.toString()).apply();
                        avatarView.setImageURI(imageUri);
                    }
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    prefs.edit().putString("avatar_uri", cameraImageUri.toString()).apply();
                    avatarView.setImageURI(cameraImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        avatarView = findViewById(R.id.avatar);
        editName = findViewById(R.id.edit_username);
        btnSave = findViewById(R.id.btn_save_profile);

        prefs = getSharedPreferences("user", MODE_PRIVATE);
        editName.setText(prefs.getString("username", "admin"));

        String avatarUri = prefs.getString("avatar_uri", null);
        if (avatarUri != null) {
            avatarView.setImageURI(Uri.parse(avatarUri));
        } else {
            avatarView.setImageResource(R.drawable.ic_avatar);
        }

        avatarView.setOnClickListener(v -> showImagePickOptions());

        btnSave.setOnClickListener(v -> {
            prefs.edit().putString("username", editName.getText().toString().trim()).apply();
            finish();
        });
    }

    private void showImagePickOptions() {
        String[] options = {"Chọn từ thư viện", "Chụp ảnh mới"};
        new AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else {
                        checkCameraPermissionAndLaunch();
                    }
                }).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        } else {
            captureImageFromCamera();
        }
    }

    private void captureImageFromCamera() {
        File file = new File(getExternalCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        cameraLauncher.launch(cameraImageUri);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImageFromCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền CAMERA để sử dụng chức năng này", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
