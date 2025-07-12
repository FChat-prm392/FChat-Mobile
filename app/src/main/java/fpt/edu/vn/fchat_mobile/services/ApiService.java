package fpt.edu.vn.fchat_mobile.services;

import fpt.edu.vn.fchat_mobile.models.LoginRequest;
import fpt.edu.vn.fchat_mobile.models.LoginResponse;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.Call;

public interface ApiService {
    @POST("api/accounts/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @FormUrlEncoded
    @POST("api/register")
    Call<LoginResponse> register(
            @Field("username") String username,
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("api/forgot-password")
    Call<LoginResponse> resetPassword(
            @Field("email") String email
    );
}
