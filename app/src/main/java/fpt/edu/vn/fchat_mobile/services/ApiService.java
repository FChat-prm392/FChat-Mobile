package fpt.edu.vn.fchat_mobile.services;

import java.util.List;

import fpt.edu.vn.fchat_mobile.items.ChatItem;
import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.models.MessageReaction;
import fpt.edu.vn.fchat_mobile.models.UserStatus;
import fpt.edu.vn.fchat_mobile.requests.GoogleLoginRequest;
import fpt.edu.vn.fchat_mobile.requests.LoginRequest;
import fpt.edu.vn.fchat_mobile.requests.RegisterRequest;
import fpt.edu.vn.fchat_mobile.requests.SendFriendRequestRequest;
import fpt.edu.vn.fchat_mobile.requests.SendMessageRequest;
import fpt.edu.vn.fchat_mobile.requests.UpdateFriendRequestRequest;
import fpt.edu.vn.fchat_mobile.responses.AccountListResponse;
import fpt.edu.vn.fchat_mobile.responses.FriendshipResponse;
import fpt.edu.vn.fchat_mobile.responses.LoginResponse;
import fpt.edu.vn.fchat_mobile.responses.RegisterResponse;
import fpt.edu.vn.fchat_mobile.responses.ChatResponse;
import fpt.edu.vn.fchat_mobile.responses.MessageResponse;
import fpt.edu.vn.fchat_mobile.responses.SendMessageResponse;

import fpt.edu.vn.fchat_mobile.responses.UpdateUserResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.Call;
import retrofit2.http.PUT;
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

    @Multipart
    @PUT("api/accounts/{id}")
    Call<UpdateUserResponse> updateUser(
            @Path("id") String id,
            @Part MultipartBody.Part file,
            @Part("fullname") RequestBody fullname,
            @Part("username") RequestBody username,
            @Part("gender") RequestBody gender,
            @Part("phoneNumber") RequestBody phone,
            @Part("email") RequestBody email
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

    // ✅ FRIENDSHIP ENDPOINTS (Updated to match your existing server API)
    
    // Send friend request
    @POST("api/friendships")
    Call<Void> sendFriendRequest(@Body SendFriendRequestRequest request);

    // Update friend request status (accept/decline)
    @PUT("api/friendships/{id}")
    Call<Void> updateFriendRequest(@Path("id") String friendshipId, @Body UpdateFriendRequestRequest request);

    // Get pending friend requests for a user
    @GET("api/friendships/requests/{userId}")
    Call<FriendshipResponse> getFriendRequests(@Path("userId") String userId);

    // Get all friendships for a user  
    @GET("api/friendships/friends/{userId}")
    Call<FriendshipResponse> getAllFriendships(@Path("userId") String userId);

    // Get friend list (just the friend users) for a user
    @GET("api/friendships/list/{userId}")
    Call<AccountListResponse> getFriendList(@Path("userId") String userId);

    // Delete friendship
    @DELETE("api/friendships/{id}")
    Call<Void> deleteFriendship(@Path("id") String friendshipId);

    // Search users (for adding friends) - using dedicated search endpoint
    @GET("api/accounts/search")
    Call<AccountListResponse> searchUsers(@Query("q") String query);

    // ✅ EXISTING ENDPOINTS (keeping for backward compatibility)
    @GET("api/friends/{userId}")
    Call<List<Friend>> getFriends(@Path("userId") String userId);

    @POST("api/friend-requests")
    Call<Void> sendFriendRequest(
            @Query("requesterId") String requesterId,
            @Query("recipientId") String recipientId
    );

    // 🎭 MESSAGE REACTIONS ENDPOINTS
    @POST("api/messages/{messageId}/reactions")
    Call<MessageReaction> addReaction(
            @Path("messageId") String messageId,
            @Query("userId") String userId,
            @Query("emoji") String emoji
    );

    @DELETE("api/messages/{messageId}/reactions")
    Call<Void> removeReaction(
            @Path("messageId") String messageId,
            @Query("userId") String userId,
            @Query("emoji") String emoji
    );

    @GET("api/messages/{messageId}/reactions")
    Call<List<MessageReaction>> getMessageReactions(@Path("messageId") String messageId);


}
