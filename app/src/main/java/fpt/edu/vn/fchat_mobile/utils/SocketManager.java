package fpt.edu.vn.fchat_mobile.utils;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import fpt.edu.vn.fchat_mobile.BuildConfig;

public class SocketManager {
    private static Socket socket;
    private static final String SERVER_URL = BuildConfig.BASE_URL;
    private static final String TAG = "SocketManager";

    public static void initializeSocket() {
        if (socket == null) {
            try {
                IO.Options opts = new IO.Options();
                opts.reconnection = true;
                opts.reconnectionAttempts = Integer.MAX_VALUE;
                opts.reconnectionDelay = 1000;
                socket = IO.socket(SERVER_URL, opts);
                socket.connect();
                Log.d(TAG, "Socket initialized and connected");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing socket: " + e.getMessage());
            }
        }
    }

    public static Socket getSocket() {
        if (socket == null) {
            initializeSocket();
        }
        return socket;
    }

    public static void disconnectSocket() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            socket = null;
            Log.d(TAG, "Socket disconnected");
        }
    }
    public static void emitUserLogout(String userId) {
        if (socket != null && socket.connected()) {
            socket.emit("user-logout", userId);
            Log.d(TAG, "Emitted user-logout for user: " + userId);
        }
    }
    
    public static void joinRoom(String chatId) {
        if (socket != null && socket.connected()) {
            socket.emit("join-room", chatId);
            Log.d(TAG, "Joined room: " + chatId);
        }
    }
    
    public static void registerUser(String userId) {
        if (socket != null && socket.connected()) {
            socket.emit("register-user", userId);
            Log.d(TAG, "Registered user: " + userId);
            
            // Request status sync for offline period
            requestStatusSync(userId);
        }
    }
    
    // Request status sync for messages sent while offline
    public static void requestStatusSync(String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("sync-message-status", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error requesting status sync", e);
            }
        }
    }

    public static boolean isConnected() {
        return socket != null && socket.connected();
    }



    // Message Status Methods
    public static void emitMessageSent(String messageId, String chatId, String senderId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("senderId", senderId);
                socket.emit("message-sent", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-sent", e);
            }
        }
    }

    public static void emitMessageDelivered(String messageId, String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("message-delivered", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-delivered", e);
            }
        }
    }

    public static void emitMessageRead(String messageId, String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("message-read", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-read", e);
            }
        }
    }

    // Typing Indicator Methods
    public static void emitTypingStart(String chatId, String userId, String userName) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatId", chatId);
                data.put("userId", userId);
                data.put("userName", userName);
                socket.emit("typing-start", data);
                Log.d(TAG, "Emitted typing-start for chat: " + chatId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting typing-start", e);
            }
        }
    }

    public static void emitTypingStop(String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("typing-stop", data);
                Log.d(TAG, "Emitted typing-stop for chat: " + chatId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting typing-stop", e);
            }
        }
    }

    // Chat Presence Methods
    public static void emitUserEnteredChat(String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("user-entered-chat", data);
                Log.d(TAG, "Emitted user-entered-chat: " + chatId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting user-entered-chat", e);
            }
        }
    }

    public static void emitUserLeftChat(String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("user-left-chat", data);
                Log.d(TAG, "Emitted user-left-chat: " + chatId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting user-left-chat", e);
            }
        }
    }

    // Socket Event Listeners Setup
    public static void setupMessageStatusListeners(MessageStatusListener listener) {
        if (socket != null && listener != null) {
            socket.on("message-status-update", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String messageId = data.getString("messageId");
                    String status = data.getString("status");
                    listener.onMessageStatusChanged(messageId, status);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing message-status-update", e);
                }
            });
            
            // Listen for bulk status sync
            socket.on("status-sync-complete", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    int syncCount = data.optInt("count", 0);
                    // Refresh UI if needed
                    if (syncCount > 0) {
                        // You might want to refresh the current chat view
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing status-sync-complete", e);
                }
            });

            socket.on("user-typing", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    boolean isTyping = data.getBoolean("isTyping");
                    String userName = data.optString("userName", "");
                    listener.onUserTyping(userId, userName, isTyping);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing user-typing", e);
                }
            });

            socket.on("user-chat-presence", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    boolean isInChat = data.getBoolean("isInChat");
                    listener.onUserPresenceChanged(userId, isInChat);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing user-chat-presence", e);
                }
            });
        }
    }

    // Interface for handling socket events
    public interface MessageStatusListener {
        void onMessageStatusChanged(String messageId, String status);
        void onUserTyping(String userId, String userName, boolean isTyping);
        void onUserPresenceChanged(String userId, boolean isInChat);
    }
}