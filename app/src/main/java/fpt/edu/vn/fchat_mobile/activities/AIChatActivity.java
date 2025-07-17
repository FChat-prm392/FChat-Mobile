package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpt.edu.vn.fchat_mobile.R;
import fpt.edu.vn.fchat_mobile.adapters.MessageAdapter;
import fpt.edu.vn.fchat_mobile.items.MessageItem;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.requests.MessageRequest;
import fpt.edu.vn.fchat_mobile.responses.GeminiResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatActivity extends AppCompatActivity {
    private EditText editMessage;
    private ImageView btnSend;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<MessageItem> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        recyclerView = findViewById(R.id.message_list);

        messageAdapter = new MessageAdapter(messageList);
        messageAdapter.setCurrentUserId("you");
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                addUserMessage(msg);
                sendToGemini(msg);
                editMessage.setText("");
            }
        });

        editMessage.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }

    private void addUserMessage(String text) {
        messageList.add(new MessageItem(text, true, null, getTimeNow()));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void addAIMessage(String text) {
        messageList.add(new MessageItem(text, false, null, getTimeNow()));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendToGemini(String text) {
        ApiService api = ApiClient.getService();
        Call<GeminiResponse> call = api.askGemini(new MessageRequest(text));
        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addAIMessage(response.body().getText());
                } else {
                    Toast.makeText(AIChatActivity.this, "Error: Invalid AI response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Toast.makeText(AIChatActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String getTimeNow() {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
    }
}
