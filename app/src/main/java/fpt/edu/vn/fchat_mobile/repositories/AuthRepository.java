package fpt.edu.vn.fchat_mobile.repositories;

import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.requests.RegisterRequest;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.responses.RegisterResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;

public class AuthRepository {
    private final ApiService apiService = ApiClient.getService();

    public void login(LoginRequest request, Callback<LoginResponse> callback) {
        Call<LoginResponse> call = apiService.login(request);
        call.enqueue(callback);
    }

    public void register(RegisterRequest request, Callback<RegisterResponse> callback) {
        Call<RegisterResponse> call = apiService.register(request);
        call.enqueue(callback);
    }


    public void resetPassword(String email, Callback<LoginResponse> callback) {
        Call<LoginResponse> call = apiService.resetPassword(email);
        call.enqueue(callback);
    }
}
