package fpt.edu.vn.fchat_mobile.repositories;

import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.models.LoginResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;

public class AuthRepository {
    private final ApiService apiService = ApiClient.getService();

    public void login(String email, String password, Callback<LoginResponse> callback) {
        Call<LoginResponse> call = apiService.login(email, password);
        call.enqueue(callback);
    }
}

