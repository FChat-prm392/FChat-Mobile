package fpt.edu.vn.fchat_mobile.repositories;

import java.util.List;

import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FriendRepository {
    private final ApiService apiService;

    public FriendRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://yourapi.com/api/") // <-- đổi đúng baseURL thực tế
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public void getFriends(String userId, FriendCallback callback) {
        apiService.getFriends(userId).enqueue(new Callback<List<Friend>>() {
            @Override
            public void onResponse(Call<List<Friend>> call, Response<List<Friend>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("API Error"));
                }
            }

            @Override
            public void onFailure(Call<List<Friend>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void sendFriendRequest(String requesterId, String recipientId, Callback<Void> callback) {
        apiService.sendFriendRequest(requesterId, recipientId).enqueue(callback);
    }


    public interface FriendCallback {
        void onSuccess(List<Friend> friends);
        void onError(Throwable t);
    }
}
