package fpt.edu.vn.fchat_mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.MessageAdapter;
import fpt.edu.vn.fchat_mobile.models.MessageItem;

public class ChatDetailActivity extends AppCompatActivity {

    private ImageView avatarView;
    private TextView nameText, statusText;
    private EditText editMessage;
    private ImageView btnSend, btnMic;

    private List<MessageItem> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // Nhận dữ liệu từ ChatListActivity
        avatarView = findViewById(R.id.avatar);
        nameText = findViewById(R.id.name);
        statusText = findViewById(R.id.status);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String status = intent.getStringExtra("status");
        int avatarResId = intent.getIntExtra("avatarResId", R.drawable.ic_avatar);

        nameText.setText(name);
        statusText.setText(status);
        avatarView.setImageResource(avatarResId);

        // Khởi tạo RecyclerView và Adapter
        RecyclerView recyclerView = findViewById(R.id.message_list);
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gửi tin nhắn
        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                messageList.add(new MessageItem(content, true));
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
                editMessage.setText("");
            }
        });

        // Tự động chuyển icon mic ↔ send
        editMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = !s.toString().trim().isEmpty();
                btnSend.setVisibility(hasText ? ImageView.VISIBLE : ImageView.GONE);
                btnMic.setVisibility(hasText ? ImageView.GONE : ImageView.VISIBLE);
            }
        });
    }
}
