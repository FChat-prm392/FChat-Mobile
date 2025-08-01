package fpt.edu.vn.fchat_mobile.repositories;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.requests.SendFriendRequestRequest;
import fpt.edu.vn.fchat_mobile.requests.UpdateFriendRequestRequest; // Import from requests package
import fpt.edu.vn.fchat_mobile.responses.AccountListResponse;
import fpt.edu.vn.fchat_mobile.responses.FriendshipResponse;
import fpt.edu.vn.fchat_mobile.responses.NonFriendsResponse;
import fpt.edu.vn.fchat_mobile.responses.UserResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {
    private static final String TAG = "FriendRepository";
    private final ApiService apiService;

    public FriendRepository() {
        apiService = ApiClient.getService();
    }

    public void getNonFriends(String userId, UsersWithStatusCallback callback) {
        Log.d(TAG, "Fetching non-friends for user: " + (userId != null ? userId : "null"));
        if (userId == null) {
            callback.onError(new Exception("UserId is null"));
            return;
        }
        apiService.getNonFriends(userId).enqueue(new Callback<NonFriendsResponse>() {
            @Override
            public void onResponse(Call<NonFriendsResponse> call, Response<NonFriendsResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<UserResponse> userResponses = response.body().getData();
                        if (userResponses != null) {
                            List<AccountWithStatus> usersWithStatus = new ArrayList<>();
                            for (UserResponse userResponse : userResponses) {
                                Account account = new Account(
                                        userResponse.getId(),
                                        userResponse.getFullname(),
                                        userResponse.getUsername(),
                                        userResponse.getEmail(),
                                        userResponse.getAvatarUrl()
                                );
                                account.setGender(userResponse.getGender());
                                account.setPhoneNumber(userResponse.getPhoneNumber());
                                account.setCurrentStatus(userResponse.getCurrentStatus());
                                account.setOnline(userResponse.isStatus());
                                account.setLastOnline(userResponse.getLastOnline());
                                usersWithStatus.add(new AccountWithStatus(account, userResponse.getFriendshipStatus()));
                            }
                            Log.d(TAG, "Fetched " + usersWithStatus.size() + " non-friends");
                            callback.onSuccess(usersWithStatus);
                        } else {
                            Log.w(TAG, "No data field in response");
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        String errorMsg = "Failed to fetch non-friends: " + response.code();
                        Log.e(TAG, errorMsg);
                        callback.onError(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing non-friends response: " + e.getMessage(), e);
                    callback.onError(e);
                }
            }

            @Override
            public void onFailure(Call<NonFriendsResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching non-friends: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }

    public void searchUsers(String query, String userId, UsersWithStatusCallback callback) {
        Log.d(TAG, "Searching users with query: " + (query != null ? query : "null") + ", userId: " + (userId != null ? userId : "null"));
        try {
            if (query == null || query.trim().isEmpty()) {
                callback.onError(new Exception("Search query cannot be empty"));
                return;
            }
            if (userId == null) {
                callback.onError(new Exception("UserId is null for search"));
                return;
            }

            apiService.searchUsers(query.trim(), userId).enqueue(new Callback<List<UserResponse>>() {
                @Override
                public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                    try {
                        Log.d(TAG, "Search response code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            List<UserResponse> userResponses = response.body();
                            List<AccountWithStatus> usersWithStatus = new ArrayList<>();
                            for (UserResponse userResponse : userResponses) {
                                Account account = new Account(
                                        userResponse.getId(),
                                        userResponse.getFullname(),
                                        userResponse.getUsername(),
                                        userResponse.getEmail(),
                                        userResponse.getAvatarUrl()
                                );
                                account.setGender(userResponse.getGender());
                                account.setPhoneNumber(userResponse.getPhoneNumber());
                                account.setCurrentStatus(userResponse.getCurrentStatus());
                                account.setOnline(userResponse.isStatus());
                                account.setLastOnline(userResponse.getLastOnline());
                                usersWithStatus.add(new AccountWithStatus(account, userResponse.getFriendshipStatus()));
                            }
                            Log.d(TAG, "Fetched " + usersWithStatus.size() + " search results");
                            callback.onSuccess(usersWithStatus);
                        } else {
                            String errorMsg = "Search failed with code: " + response.code();
                            Log.e(TAG, errorMsg + (response.errorBody() != null ? " - " + response.errorBody().toString() : ""));
                            callback.onError(new Exception(errorMsg));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing search response: " + e.getMessage(), e);
                        callback.onError(e);
                    }
                }

                @Override
                public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                    Log.e(TAG, "Network error searching users: " + (t != null ? t.getMessage() : "null"), t);
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating search: " + e.getMessage(), e);
            callback.onError(e);
        }
    }

    public void sendFriendRequest(String requesterId, String recipientId, SimpleCallback callback) {
        Log.d(TAG, "Sending friend request from requesterId: " + (requesterId != null ? requesterId : "null") +
                " to recipientId: " + (recipientId != null ? recipientId : "null"));
        if (requesterId == null || recipientId == null) {
            callback.onError(new Exception("RequesterId or recipientId is null"));
            return;
        }
        if (requesterId.equals(recipientId)) {
            callback.onError(new Exception("Cannot send friend request to yourself"));
            return;
        }
        SendFriendRequestRequest request = new SendFriendRequestRequest(requesterId, recipientId, "pending");
        Log.d(TAG, "Request object: requester=" + request.getRequester() + ", recipient=" + request.getRecipient() + ", requestStatus=" + request.getRequestStatus());
        apiService.sendFriendRequest(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Friend request sent successfully");
                    callback.onSuccess();
                } else {
                    String errorMsg = "API Error: " + response.code() +
                            (response.errorBody() != null ? " - " + parseErrorBody(response.errorBody()) : "");
                    Log.e(TAG, errorMsg);
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error sending friend request: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }

    private String parseErrorBody(ResponseBody errorBody) {
        if (errorBody == null) return "No error details available";

        try {
            // Assuming the server returns a JSON object with a "message" field
            String errorBodyString = errorBody.string();
            JSONObject jsonObject = new JSONObject(errorBodyString);
            return jsonObject.optString("message", "Unknown error");
        } catch (IOException e) {
            Log.e(TAG, "Error reading error body: " + e.getMessage(), e);
            return "Error reading response";
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing error body JSON: " + e.getMessage(), e);
            return "Invalid error format";
        }
    }

    public void getFriendList(String userId, FriendListCallback callback) {
        Log.d(TAG, "Fetching friend list for user: " + (userId != null ? userId : "null"));
        if (userId == null) {
            callback.onError(new Exception("UserId is null"));
            return;
        }
        apiService.getFriendList(userId).enqueue(new Callback<AccountListResponse>() {
            @Override
            public void onResponse(Call<AccountListResponse> call, Response<AccountListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Account> friends = new ArrayList<>();
                    if (response.body().getData() != null) {
                        friends.addAll(response.body().getData());
                    }
                    callback.onSuccess(friends);
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AccountListResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void getFriendRequests(String userId, FriendRequestsCallback callback) {
        Log.d(TAG, "Fetching friend requests for user: " + (userId != null ? userId : "null"));
        if (userId == null) {
            callback.onError(new Exception("UserId is null"));
            return;
        }
        apiService.getFriendRequests(userId).enqueue(new Callback<FriendshipResponse>() {
            @Override
            public void onResponse(Call<FriendshipResponse> call, Response<FriendshipResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Friendship> friendRequests = response.body().getData();
                        Log.d(TAG, "Fetched " + (friendRequests != null ? friendRequests.size() : 0) + " friend requests");
                        callback.onSuccess(friendRequests != null ? friendRequests : new ArrayList<>());
                    } else {
                        String errorMsg = "Failed to fetch friend requests: " + response.code();
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Raw error body: " + response.errorBody().string());
                        }
                        Log.e(TAG, errorMsg);
                        callback.onError(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing friend requests response: " + e.getMessage(), e);
                    if (e instanceof com.google.gson.JsonSyntaxException && response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Raw response body: " + response.errorBody().string());
                        } catch (IOException ioe) {
                            Log.e(TAG, "Failed to read raw response: " + ioe.getMessage(), ioe);
                        }
                    }
                    callback.onError(e);
                }
            }

            @Override
            public void onFailure(Call<FriendshipResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching friend requests: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }

    public void updateFriendRequest(String friendshipId, UpdateFriendRequestRequest request, SimpleCallback callback) {
        Log.d(TAG, "Updating friend request: " + (friendshipId != null ? friendshipId : "null") + " with status: " + request.getRequestStatus());
        if (friendshipId == null) {
            callback.onError(new Exception("FriendshipId is null"));
            return;
        }
        apiService.updateFriendRequest(friendshipId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Friend request updated successfully");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to update friend request: " + response.code() +
                            (response.errorBody() != null ? " - " + parseErrorBody(response.errorBody()) : "");
                    Log.e(TAG, errorMsg);
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error updating friend request: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }

    public void acceptFriendRequest(String requestId, SimpleCallback callback) {
        Log.d(TAG, "Accepting friend request: " + (requestId != null ? requestId : "null"));
        if (requestId == null) {
            callback.onError(new Exception("RequestId is null"));
            return;
        }
        updateFriendRequest(requestId, new UpdateFriendRequestRequest("accepted"), callback);
    }

    public void declineFriendRequest(String requestId, SimpleCallback callback) {
        Log.d(TAG, "Declining friend request: " + (requestId != null ? requestId : "null"));
        if (requestId == null) {
            callback.onError(new Exception("RequestId is null"));
            return;
        }
        updateFriendRequest(requestId, new UpdateFriendRequestRequest("declined"), callback);
    }

    public void getUserById(String userId, UserCallback callback) {
        Log.d(TAG, "Fetching user by ID: " + (userId != null ? userId : "null"));
        if (userId == null) {
            callback.onError(new Exception("UserId is null"));
            return;
        }
        apiService.getUserById(userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        UserResponse userResponse = response.body();
                        Account account = new Account(
                                userResponse.getId(),
                                userResponse.getFullname(),
                                userResponse.getUsername(),
                                userResponse.getEmail(),
                                userResponse.getAvatarUrl()
                        );
                        account.setGender(userResponse.getGender());
                        account.setPhoneNumber(userResponse.getPhoneNumber());
                        account.setCurrentStatus(userResponse.getCurrentStatus());
                        account.setOnline(userResponse.isStatus());
                        account.setLastOnline(userResponse.getLastOnline());
                        callback.onSuccess(account);
                    } else {
                        String errorMsg = "Failed to fetch user: " + response.code();
                        Log.e(TAG, errorMsg);
                        callback.onError(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing user response: " + e.getMessage(), e);
                    callback.onError(e);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching user: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }




    public interface UsersWithStatusCallback {
        void onSuccess(List<AccountWithStatus> users);
        void onError(Throwable t);
    }

    public interface FriendListCallback {
        void onSuccess(List<Account> friends);
        void onError(Throwable t);
    }

    public interface FriendRequestsCallback {
        void onSuccess(List<Friendship> friendRequests);
        void onError(Throwable t);
    }

    public interface FriendRequestCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public interface UserCallback {
        void onSuccess(Account account);
        void onError(Throwable t);
    }

    public class AccountWithStatus {
        private final Account account;
        private String status;

        public AccountWithStatus(Account account, String status) {
            this.account = account;
            this.status = status;
        }

        public Account getAccount() {
            return account;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }


    // Block user
    public void blockUser(String blockerId, String blockedId, SimpleCallback callback) {
        Log.d(TAG, "Blocking user from " + (blockerId != null ? blockerId : "null") + " to " + (blockedId != null ? blockedId : "null"));
        if (blockerId == null || blockedId == null) {
            callback.onError(new Exception("BlockerId or blockedId is null"));
            return;
        }
        apiService.blockUser(blockerId, blockedId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Unblock user
    public void unblockUser(String blockerId, String blockedId, SimpleCallback callback) {
        Log.d(TAG, "Unblocking user from " + (blockerId != null ? blockerId : "null") + " to " + (blockedId != null ? blockedId : "null"));
        if (blockerId == null || blockedId == null) {
            callback.onError(new Exception("BlockerId or blockedId is null"));
            return;
        }
        apiService.unblockUser(blockerId, blockedId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void getBlockedUsers(String userId, BlockedUsersCallback callback) {
        Log.d(TAG, "Fetching blocked users for user: " + (userId != null ? userId : "null"));
        if (userId == null) {
            callback.onError(new Exception("UserId is null"));
            return;
        }
        apiService.getBlockedUsers(userId).enqueue(new Callback<AccountListResponse>() {
            @Override
            public void onResponse(Call<AccountListResponse> call, Response<AccountListResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Account> blockedUsers = response.body().getData();
                        Log.d(TAG, "Fetched " + (blockedUsers != null ? blockedUsers.size() : 0) + " blocked users");
                        callback.onSuccess(blockedUsers != null ? blockedUsers : new ArrayList<>());
                    } else {
                        String errorMsg = "Failed to fetch blocked users: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                Log.e(TAG, "Raw error body: " + response.errorBody().string());
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to read error body: " + e.getMessage(), e);
                            }
                        }
                        Log.e(TAG, errorMsg);
                        callback.onError(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing blocked users response: " + e.getMessage(), e);
                    if (e instanceof com.google.gson.JsonSyntaxException && response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Raw response body: " + response.errorBody().string());
                        } catch (IOException ioe) {
                            Log.e(TAG, "Failed to read raw response: " + ioe.getMessage(), ioe);
                        }
                    }
                    callback.onError(e);
                }
            }

            @Override
            public void onFailure(Call<AccountListResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching blocked users: " + (t != null ? t.getMessage() : "null"), t);
                callback.onError(t);
            }
        });
    }

    // [Other interfaces and methods omitted for brevity]

    public interface BlockedUsersCallback {
        void onSuccess(List<Account> blockedUsers);
        void onError(Throwable t);
    }
}