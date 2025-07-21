package fpt.edu.vn.fchat_mobile.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatActivity extends AppCompatActivity {
    private static final String TAG = "AIChatActivity";
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

        // Send welcome message when activity is created
        sendWelcomeMessage();

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

    private void sendWelcomeMessage() {
        String welcomePrompt = "Greet the user warmly and introduce yourself as Gemini, an AI assistant ready to help with any questions or conversation.";
        sendToGemini(welcomePrompt);
    }

    private void addUserMessage(String text) {
        messageList.add(new MessageItem(text, true, null, getTimeNow()));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void addAIMessage(String text) {
        if (text != null && !text.isEmpty()) {
            messageList.add(new MessageItem(text, false, null, getTimeNow()));
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        } else {
            Log.w(TAG, "Attempted to add empty or null AI message");
        }
    }

    private String getConversationHistory() {
        StringBuilder history = new StringBuilder();
        for (MessageItem item : messageList) {
            String role = item.isSentByUser() ? "User" : "Gemini";
            history.append(role).append(": ").append(item.getContent()).append("\n");
        }
        return history.toString();
    }

    private void sendToGemini(String text) {
        ApiService api = ApiClient.getService();
        String context = getConversationHistory();
        Log.d(TAG, "Sending to Gemini - Message: " + text + ", Context: " + context);
        Call<GeminiResponse> call = api.askGemini(new MessageRequest(text, context));
        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().getText();
                    Log.d(TAG, "Gemini Response: " + reply);
                    addAIMessage(reply);
                } else {
                    String errorMsg = "Error: Invalid AI response - Code: " + response.code() + ", Message: " + response.message();
                    Log.e(TAG, errorMsg);
                    Toast.makeText(AIChatActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                String errorMsg = "Failed: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                Toast.makeText(AIChatActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getTimeNow() {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
    }
}
