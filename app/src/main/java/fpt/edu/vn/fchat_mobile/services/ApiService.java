package fpt.edu.vn.fchat_mobile.services;

import java.util.List;

import fpt.edu.vn.fchat_mobile.items.ChatItem;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.models.UserStatus;
import fpt.edu.vn.fchat_mobile.requests.GoogleLoginRequest;
import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.requests.RegisterRequest;
import fpt.edu.vn.fchat_mobile.requests.SendMessageRequest;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.responses.RegisterResponse;
import fpt.edu.vn.fchat_mobile.responses.ChatResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.responses.SendMessageResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.Call;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/accounts/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @Multipart
    @POST("api/accounts")
    Call<RegisterResponse> registerWithImage(
            @Part MultipartBody.Part image,
            @Part("fullname") RequestBody fullname,
            @Part("username") RequestBody username,
            @Part("email") RequestBody email,
            @Part("password") RequestBody password,
            @Part("gender") RequestBody gender,
            @Part("phoneNumber") RequestBody phoneNumber,
            @Part("currentStatus") RequestBody currentStatus,
            @Part("fcmToken") RequestBody fcmToken
    );

    @FormUrlEncoded
    @POST("api/forgot-password")
    Call<LoginResponse> resetPassword(@Field("email") String email);

    @POST("api/auth/google-login")
    Call<LoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @GET("api/messages/{chatId}")
    Call<List<MessageResponse>> getMessagesByChatId(@Path("chatId") String chatId, @Query("limit") int limit);

    @GET("api/accounts/status/{userId}")
    Call<UserStatus> getUserStatus(@Path("userId") String userId);

    @GET("api/chats/user/{userId}")
    Call<List<ChatResponse>> getChats(@Path("userId") String userId);

    @POST("api/messages")
    Call<SendMessageResponse> sendMessage(@Body SendMessageRequest request);

    // ✅ THÊM API LẤY DANH SÁCH BẠN BÈ
    @GET("api/friends/{userId}")
    Call<List<Friend>> getFriends(@Path("userId") String userId);

    @POST("api/friend-requests")
    Call<Void> sendFriendRequest(
            @Query("requesterId") String requesterId,
            @Query("recipientId") String recipientId
    );

}
