package fpt.edu.vn.fchat_mobile.repositories;

import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.responses.UpdateUserResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class UserRepository {
    private static UserRepository instance;
    private final ApiService apiService;

    private UserRepository() {
        apiService = ApiClient.getService();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void updateUserWithImage(String userId,
                                    MultipartBody.Part image,
                                    RequestBody fullname,
                                    RequestBody username,
                                    RequestBody gender,
                                    RequestBody phone,
                                    RequestBody email,
                                    Callback<UpdateUserResponse> callback) {
        Call<UpdateUserResponse> call = apiService.updateUser(userId, image, fullname, username, gender, phone, email);
        call.enqueue(callback);
    }

}
