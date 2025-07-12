package fpt.edu.vn.fchat_mobile.services;

import java.util.List;

import fpt.edu.vn.fchat_mobile.items.ChatItem;
import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.requests.RegisterRequest;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.responses.RegisterResponse;
import fpt.edu.vn.fchat_mobile.responses.ChatResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;

import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.Call;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/accounts/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/accounts")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @FormUrlEncoded
    @POST("api/forgot-password")
    Call<LoginResponse> resetPassword(
            @Field("email") String email
    );

    @GET("api/messages/{messageId}")
    Call<MessageResponse> getMessageById(@Path("messageId") String messageId);


    @GET("api/chats/user/{userId}")
    Call<List<ChatResponse>> getChats(@Path("userId") String userId);
}
