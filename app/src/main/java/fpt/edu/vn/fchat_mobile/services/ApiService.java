package fpt.edu.vn.fchat_mobile.services;

import fpt.edu.vn.fchat_mobile.models.LoginResponse;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.Call;

public interface ApiService {
    @FormUrlEncoded
    @POST("api/login")
    Call<LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );
}
