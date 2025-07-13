package fpt.edu.vn.fchat_mobile.repositories;

import android.util.Log;

import java.util.List;

import fpt.edu.vn.fchat_mobile.models.MessageReaction;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReactionRepository {
    private static final String TAG = "ReactionRepository";
    private final ApiService apiService;

    public interface ReactionCallback {
        void onSuccess(MessageReaction reaction);
        void onError(Throwable error);
    }

    public interface ReactionsListCallback {
        void onSuccess(List<MessageReaction> reactions);
        void onError(Throwable error);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(Throwable error);
    }

    public ReactionRepository() {
        this.apiService = ApiClient.getService();
    }

    public void addReaction(String messageId, String userId, String emoji, ReactionCallback callback) {
        try {
            if (messageId == null || userId == null || emoji == null || callback == null) {
                if (callback != null) {
                    callback.onError(new Exception("Invalid parameters"));
                }
                return;
            }
            
            Call<MessageReaction> call = apiService.addReaction(messageId, userId, emoji);
            call.enqueue(new Callback<MessageReaction>() {
                @Override
                public void onResponse(Call<MessageReaction> call, Response<MessageReaction> response) {
                    try {
                        if (response.isSuccessful()) {
                            MessageReaction serverReaction = response.body();
                            if (serverReaction != null && serverReaction.getEmoji() != null) {
                                callback.onSuccess(serverReaction);
                            } else {
                                MessageReaction manualReaction = new MessageReaction();
                                manualReaction.setMessageId(messageId);
                                manualReaction.setUserId(userId);
                                manualReaction.setEmoji(emoji);
                                callback.onSuccess(manualReaction);
                            }
                        } else {
                            String errorMessage = "Failed to add reaction: " + response.message();
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    if (errorBody.contains("already exists")) {
                                        errorMessage = "REACTION_EXISTS";
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore error reading error body
                            }
                            callback.onError(new Exception(errorMessage));
                        }
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }

                @Override
                public void onFailure(Call<MessageReaction> call, Throwable t) {
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public void removeReaction(String messageId, String userId, String emoji, VoidCallback callback) {
        try {
            Call<Void> call = apiService.removeReaction(messageId, userId, emoji);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(new Exception("Failed to remove reaction: " + response.message()));
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void getMessageReactions(String messageId, ReactionsListCallback callback) {
        try {
            Call<List<MessageReaction>> call = apiService.getMessageReactions(messageId);
            call.enqueue(new Callback<List<MessageReaction>>() {
                @Override
                public void onResponse(Call<List<MessageReaction>> call, Response<List<MessageReaction>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError(new Exception("Failed to get reactions: " + response.message()));
                    }
                }

                @Override
                public void onFailure(Call<List<MessageReaction>> call, Throwable t) {
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
