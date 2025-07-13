package fpt.edu.vn.fchat_mobile.repositories;

import java.util.ArrayList;
import java.util.List;

import fpt.edu.vn.fchat_mobile.models.Account;
import fpt.edu.vn.fchat_mobile.models.Friend;
import fpt.edu.vn.fchat_mobile.models.Friendship;
import fpt.edu.vn.fchat_mobile.network.ApiClient;
import fpt.edu.vn.fchat_mobile.requests.SendFriendRequestRequest;
import fpt.edu.vn.fchat_mobile.requests.UpdateFriendRequestRequest;
import fpt.edu.vn.fchat_mobile.responses.AccountListResponse;
import fpt.edu.vn.fchat_mobile.responses.FriendshipResponse;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {
    private final ApiService apiService;

    public FriendRepository() {
        apiService = ApiClient.getService();
    }

    // Get friend list (Account objects)
    public void getFriendList(String userId, FriendListCallback callback) {
        apiService.getFriendList(userId).enqueue(new Callback<AccountListResponse>() {
            @Override
            public void onResponse(Call<AccountListResponse> call, Response<AccountListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Extract friends from accepted friendships
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

    // Get pending friend requests
    public void getFriendRequests(String userId, FriendRequestsCallback callback) {
        apiService.getFriendRequests(userId).enqueue(new Callback<FriendshipResponse>() {
            @Override
            public void onResponse(Call<FriendshipResponse> call, Response<FriendshipResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Friendship> friendRequests = response.body().getData();
                    if (friendRequests != null) {
                        callback.onSuccess(friendRequests);
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<FriendshipResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Send friend request
    public void sendFriendRequest(String requesterId, String recipientId, SimpleCallback callback) {
        SendFriendRequestRequest request = new SendFriendRequestRequest(requesterId, recipientId);
        apiService.sendFriendRequest(request).enqueue(new Callback<Void>() {
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

    // Accept friend request
    public void acceptFriendRequest(String friendshipId, SimpleCallback callback) {
        UpdateFriendRequestRequest request = new UpdateFriendRequestRequest("accepted");
        apiService.updateFriendRequest(friendshipId, request).enqueue(new Callback<Void>() {
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

    // Decline friend request
    public void declineFriendRequest(String friendshipId, SimpleCallback callback) {
        UpdateFriendRequestRequest request = new UpdateFriendRequestRequest("declined");
        apiService.updateFriendRequest(friendshipId, request).enqueue(new Callback<Void>() {
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

    // Delete friendship
    public void deleteFriendship(String friendshipId, SimpleCallback callback) {
        apiService.deleteFriendship(friendshipId).enqueue(new Callback<Void>() {
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

    // Search users
    public void searchUsers(String query, UsersCallback callback) {
        try {
            if (query == null || query.trim().isEmpty()) {
                callback.onError(new Exception("Search query cannot be empty"));
                return;
            }

            apiService.searchUsers(query.trim()).enqueue(new Callback<AccountListResponse>() {
                @Override
                public void onResponse(Call<AccountListResponse> call, Response<AccountListResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            AccountListResponse responseBody = response.body();
                            if (responseBody.isSuccess()) {
                                List<Account> users = responseBody.getData();
                                callback.onSuccess(users != null ? users : new ArrayList<>());
                            } else {
                                String message = responseBody.getMessage() != null ? responseBody.getMessage() : "Search failed";
                                callback.onError(new Exception("Server error: " + message));
                            }
                        } else {
                            String errorMsg = "Search failed with code: " + response.code();
                            if (response.errorBody() != null) {
                                try {
                                    errorMsg += " - " + response.errorBody().string();
                                } catch (Exception e) {
                                    // Ignore error body parsing errors
                                }
                            }
                            callback.onError(new Exception(errorMsg));
                        }
                    } catch (Exception e) {
                        callback.onError(new Exception("Error processing search response: " + e.getMessage()));
                    }
                }

                @Override
                public void onFailure(Call<AccountListResponse> call, Throwable t) {
                    String errorMessage = t != null ? t.getMessage() : "Unknown network error";
                    callback.onError(new Exception("Network error: " + errorMessage));
                }
            });
        } catch (Exception e) {
            callback.onError(new Exception("Error initiating search: " + e.getMessage()));
        }
    }

    // Legacy method for backward compatibility
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

    // Check friendship status between two users
    public void checkFriendshipStatus(String userId1, String userId2, FriendshipStatusCallback callback) {
        try {
            if (userId1 == null || userId2 == null) {
                callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
                return;
            }
            
            getAllFriendships(userId1, new FriendRequestsCallback() {
                @Override
                public void onSuccess(List<Friendship> friendships) {
                    try {
                        if (friendships == null) {
                            callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
                            return;
                        }
                        
                        for (Friendship friendship : friendships) {
                            if (friendship == null) continue;
                            
                            boolean isRelatedUser = false;
                            
                            // Check requester
                            if (friendship.getRequester() != null && 
                                friendship.getRequester().get_id() != null && 
                                friendship.getRequester().get_id().equals(userId2)) {
                                isRelatedUser = true;
                            }
                            
                            // Check recipient
                            if (friendship.getRecipient() != null && 
                                friendship.getRecipient().get_id() != null && 
                                friendship.getRecipient().get_id().equals(userId2)) {
                                isRelatedUser = true;
                            }
                            
                            if (isRelatedUser) {
                                if (friendship.isAccepted()) {
                                    callback.onStatusChecked(FriendshipStatus.FRIENDS);
                                    return;
                                } else if (friendship.isPending()) {
                                    callback.onStatusChecked(FriendshipStatus.PENDING);
                                    return;
                                }
                            }
                        }
                        callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
                    } catch (Exception e) {
                        android.util.Log.e("FriendRepository", "Error checking friendship status: " + e.getMessage(), e);
                        callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    android.util.Log.e("FriendRepository", "Error getting friendships: " + (t != null ? t.getMessage() : "null"), t);
                    callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("FriendRepository", "Error in checkFriendshipStatus: " + e.getMessage(), e);
            callback.onStatusChecked(FriendshipStatus.NOT_FRIENDS);
        }
    }

    // Enhanced search that includes friendship status
    public void searchUsersWithFriendshipStatus(String query, String currentUserId, UsersWithStatusCallback callback) {
        try {
            if (currentUserId == null) {
                callback.onError(new Exception("Current user ID is null"));
                return;
            }
            
            searchUsers(query, new UsersCallback() {
                @Override
                public void onSuccess(List<Account> users) {
                    try {
                        if (users == null || users.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }

                        List<AccountWithStatus> usersWithStatus = new ArrayList<>();
                        int[] processedCount = {0}; // Use array to make it effectively final
                        
                        for (Account user : users) {
                            if (user == null || user.get_id() == null) {
                                processedCount[0]++;
                                if (processedCount[0] == users.size()) {
                                    callback.onSuccess(usersWithStatus);
                                }
                                continue;
                            }
                            
                            if (user.get_id().equals(currentUserId)) {
                                // Current user - mark as self
                                usersWithStatus.add(new AccountWithStatus(user, FriendshipStatus.SELF));
                                processedCount[0]++;
                                
                                if (processedCount[0] == users.size()) {
                                    callback.onSuccess(usersWithStatus);
                                }
                            } else {
                                checkFriendshipStatus(currentUserId, user.get_id(), status -> {
                                    try {
                                        usersWithStatus.add(new AccountWithStatus(user, status));
                                        processedCount[0]++;
                                        
                                        if (processedCount[0] == users.size()) {
                                            callback.onSuccess(usersWithStatus);
                                        }
                                    } catch (Exception e) {
                                        android.util.Log.e("FriendRepository", "Error processing user status: " + e.getMessage(), e);
                                        processedCount[0]++;
                                        if (processedCount[0] == users.size()) {
                                            callback.onSuccess(usersWithStatus);
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FriendRepository", "Error in searchUsersWithFriendshipStatus onSuccess: " + e.getMessage(), e);
                        callback.onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    android.util.Log.e("FriendRepository", "Error in search: " + (t != null ? t.getMessage() : "null"), t);
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("FriendRepository", "Error in searchUsersWithFriendshipStatus: " + e.getMessage(), e);
            callback.onError(e);
        }
    }

    // Get all friendships for a user (including accepted and pending)
    public void getAllFriendships(String userId, FriendRequestsCallback callback) {
        apiService.getAllFriendships(userId).enqueue(new Callback<FriendshipResponse>() {
            @Override
            public void onResponse(Call<FriendshipResponse> call, Response<FriendshipResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Friendship> friendships = response.body().getData();
                    if (friendships != null) {
                        callback.onSuccess(friendships);
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                } else {
                    callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<FriendshipResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Callback interfaces
    public interface FriendListCallback {
        void onSuccess(List<Account> friends);
        void onError(Throwable t);
    }

    public interface FriendRequestsCallback {
        void onSuccess(List<Friendship> friendRequests);
        void onError(Throwable t);
    }

    public interface UsersCallback {
        void onSuccess(List<Account> users);
        void onError(Throwable t);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public interface FriendCallback {
        void onSuccess(List<Friend> friends);
        void onError(Throwable t);
    }

    public interface FriendshipStatusCallback {
        void onStatusChecked(FriendshipStatus status);
    }

    public interface UsersWithStatusCallback {
        void onSuccess(List<AccountWithStatus> users);
        void onError(Throwable t);
    }

    public enum FriendshipStatus {
        FRIENDS,
        PENDING,
        NOT_FRIENDS,
        SELF
    }

    public class AccountWithStatus {
        private Account account;
        private FriendshipStatus status;

        public AccountWithStatus(Account account, FriendshipStatus status) {
            this.account = account;
            this.status = status;
        }

        public Account getAccount() {
            return account;
        }

        public FriendshipStatus getStatus() {
            return status;
        }
    }
}
