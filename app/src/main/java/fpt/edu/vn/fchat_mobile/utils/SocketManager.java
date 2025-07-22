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
    private static android.os.Handler heartbeatHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static Runnable heartbeatRunnable;
    private static String currentUserId;
    private static boolean registrationVerified = false;
    private static RegistrationCallback registrationCallback;
    
    // Interface for registration verification callback
    public interface RegistrationCallback {
        void onRegistrationVerified(String userId, boolean isVerified);
    }
    
    public static void setRegistrationCallback(RegistrationCallback callback) {
        registrationCallback = callback;
    }

    public static void initializeSocket() {
        Log.e(TAG, "🔧 FORCE LOG: initializeSocket() called"); // Using Log.e to ensure it appears
        if (socket == null) {
            try {
                Log.d(TAG, "🔄 Initializing socket connection to: " + SERVER_URL);
                Log.e(TAG, "🔧 FORCE LOG: Starting socket initialization to: " + SERVER_URL);
                
                IO.Options opts = new IO.Options();
                opts.reconnection = true;
                opts.reconnectionAttempts = Integer.MAX_VALUE;
                opts.reconnectionDelay = 1000;
                opts.reconnectionDelayMax = 5000;
                opts.timeout = 10000;
                opts.forceNew = false;
                opts.upgrade = true;
                opts.rememberUpgrade = true;
                socket = IO.socket(SERVER_URL, opts);
                
                Log.e(TAG, "🔧 FORCE LOG: Socket object created, connecting..."); // Using Log.e to ensure it appears
                
                // Add connection event listeners for debugging
                socket.on(Socket.EVENT_CONNECT, args -> {
                    Log.d(TAG, "🔌 SOCKET CONNECTED to " + SERVER_URL);
                    Log.d(TAG, "✅ Connection established successfully");
                });
                
                socket.on(Socket.EVENT_DISCONNECT, args -> {
                    String reason = args.length > 0 ? args[0].toString() : "Unknown";
                    Log.d(TAG, "🔌 SOCKET DISCONNECTED - Reason: " + reason);
                    registrationVerified = false; // Reset verification on disconnect
                });
                
                socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                    String error = args.length > 0 ? args[0].toString() : "Unknown";
                    Log.e(TAG, "🔌 SOCKET CONNECTION ERROR: " + error);
                    Log.e(TAG, "❌ Failed to connect to server: " + SERVER_URL);
                    Log.e(TAG, "💡 Please check:");
                    Log.e(TAG, "   • Server is running");
                    Log.e(TAG, "   • Network connectivity"); 
                    Log.e(TAG, "   • Firewall settings");
                    Log.e(TAG, "   • Server URL is correct: " + SERVER_URL);
                });
                
                // Add reconnection event listeners (using string constants instead of non-existent constants)
                socket.on("reconnect", args -> {
                    Log.d(TAG, "🔄 SOCKET RECONNECTED after " + (args.length > 0 ? args[0].toString() : "unknown") + " attempts");
                });
                
                socket.on("reconnect_error", args -> {
                    Log.e(TAG, "🔄 SOCKET RECONNECTION ERROR: " + (args.length > 0 ? args[0].toString() : "Unknown"));
                });
                
                socket.on("reconnect_failed", args -> {
                    Log.e(TAG, "🔄 SOCKET RECONNECTION FAILED - Max attempts reached");
                });
                
                // Add registration verification listener
                socket.on("registration-verified", args -> {
                    Log.d(TAG, "🔔 Registration verification event received with " + args.length + " arguments");
                    if (args.length > 0) {
                        try {
                            org.json.JSONObject data = (org.json.JSONObject) args[0];
                            String userId = data.optString("userId", "");
                            String socketId = data.optString("socketId", "");
                            boolean isOnline = data.optBoolean("isOnline", false);
                            
                            Log.d(TAG, "✅ DETAILED Registration verified for user: " + userId + 
                                      " with socket: " + socketId + 
                                      " isOnline: " + isOnline);
                            Log.d(TAG, "📊 Current user check - Expected: " + currentUserId + ", Received: " + userId + ", Match: " + userId.equals(currentUserId));
                            
                            if (isOnline && userId.equals(currentUserId)) {
                                registrationVerified = true;
                                Log.d(TAG, "✅ CONFIRMED Current user registration verified successfully");
                                
                                // Notify callback if set
                                if (registrationCallback != null) {
                                    Log.d(TAG, "📞 Triggering registration callback for user: " + userId);
                                    registrationCallback.onRegistrationVerified(userId, true);
                                } else {
                                    Log.w(TAG, "⚠️ Registration callback is null - cannot notify");
                                }
                            } else {
                                Log.w(TAG, "⚠️ Registration verification failed - isOnline: " + isOnline + ", userMatch: " + userId.equals(currentUserId));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error processing registration verification", e);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Registration verification event received with no data");
                    }
                });
                
                // Add heartbeat acknowledgment listener
                socket.on("heartbeat-ack", args -> {
                    if (args.length > 0) {
                        try {
                            org.json.JSONObject data = (org.json.JSONObject) args[0];
                            String userId = data.optString("userId", "");
                            boolean isRegistered = data.optBoolean("isRegistered", false);
                            
                            Log.d(TAG, "💓 Heartbeat ACK received for user: " + userId + 
                                      " isRegistered: " + isRegistered);
                            
                            if (!isRegistered && userId.equals(currentUserId)) {
                                Log.w(TAG, "⚠️ Heartbeat indicates user not registered, re-registering...");
                                registerUser(currentUserId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing heartbeat acknowledgment", e);
                        }
                    }
                });
                
                socket.connect();
                Log.d(TAG, "Socket initialized and connecting to " + SERVER_URL);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing socket: " + e.getMessage());
            }
        }
    }

    public static Socket getSocket() {
        Log.d(TAG, "🔧 getSocket() called - Socket exists: " + (socket != null));
        if (socket == null) {
            Log.d(TAG, "🔧 Socket is null, initializing...");
            initializeSocket();
        }
        Log.d(TAG, "🔧 getSocket() returning - Socket: " + (socket != null ? "exists" : "still null"));
        return socket;
    }

    public static void disconnectSocket() {
        stopHeartbeat();
        if (socket != null) {
            if (currentUserId != null) {
                emitUserLogout(currentUserId);
            }
            socket.disconnect();
            socket = null;
            currentUserId = null;
            Log.d(TAG, "Socket disconnected and cleaned up");
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
            currentUserId = userId;
            registrationVerified = false; // Reset verification status
            
            Log.d(TAG, "🔄 Starting registration process for user: " + userId);
            Log.d(TAG, "📊 Socket state - Connected: " + socket.connected() + ", ID: " + socket.id());
            
            // Send registration request with additional data
            try {
                org.json.JSONObject registrationData = new org.json.JSONObject();
                registrationData.put("userId", userId);
                registrationData.put("socketId", socket.id());
                registrationData.put("platform", "android");
                registrationData.put("timestamp", System.currentTimeMillis());
                
                socket.emit("register-user", registrationData);
                Log.d(TAG, "📤 DETAILED Registration request sent for user: " + userId + " with socket: " + socket.id());
                Log.d(TAG, "📋 Registration data: " + registrationData.toString());
            } catch (org.json.JSONException e) {
                // Fallback to simple registration
                socket.emit("register-user", userId);
                Log.d(TAG, "📤 FALLBACK Simple registration sent for user: " + userId);
            }

            // Start heartbeat to maintain connection
            startHeartbeat(userId);
            
            // Request status sync for offline period
            requestStatusSync(userId);
        } else {
            Log.w(TAG, "⚠️ Cannot register user - Socket not available or not connected");
            Log.w(TAG, "📊 Socket state - Null: " + (socket == null) + ", Connected: " + (socket != null ? socket.connected() : "N/A"));
        }
    }
    
    private static void startHeartbeat(String userId) {
        // Stop any existing heartbeat
        stopHeartbeat();
        
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (socket != null && socket.connected() && userId != null) {
                    try {
                        org.json.JSONObject heartbeatData = new org.json.JSONObject();
                        heartbeatData.put("userId", userId);
                        heartbeatData.put("timestamp", System.currentTimeMillis());
                        socket.emit("heartbeat", heartbeatData);
                        Log.d(TAG, "💓 Heartbeat sent for user: " + userId);
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Error sending heartbeat", e);
                    }
                }
                
                // Schedule next heartbeat
                heartbeatHandler.postDelayed(this, 30000); // Every 30 seconds
            }
        };
        
        // Start heartbeat after 30 seconds
        heartbeatHandler.postDelayed(heartbeatRunnable, 30000);
        Log.d(TAG, "💓 Heartbeat started for user: " + userId);
    }
    
    private static void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
            Log.d(TAG, "💓 Heartbeat stopped");
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
                Log.d(TAG, "📥 REQUESTED GLOBAL STATUS SYNC for user: " + userId);
            } catch (JSONException e) {
                Log.e(TAG, "Error requesting status sync", e);
            }
        }
    }

    // Request status sync for a specific chat
    public static void requestChatStatusSync(String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("chatId", chatId); // Server now uses this to filter by specific chat
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("sync-message-status", data);
                Log.d(TAG, "📥 REQUESTED CHAT-SPECIFIC STATUS SYNC for user: " + userId + " in chat: " + chatId);
            } catch (JSONException e) {
                Log.e(TAG, "Error requesting chat status sync", e);
            }
        }
    }

    public static boolean isConnected() {
        boolean connected = socket != null && socket.connected();
        Log.d(TAG, "🔌 isConnected() called - Result: " + connected + 
                  " (Socket: " + (socket != null ? "exists" : "null") + 
                  ", Connected: " + (socket != null ? socket.connected() : "N/A") + ")");
        return connected;
    }
    
    public static boolean isRegistrationVerified() {
        Log.d(TAG, "✅ isRegistrationVerified() called - Result: " + registrationVerified + 
                  " (User: " + currentUserId + ")");
        return registrationVerified;
    }
    
    public static String getCurrentUserId() {
        return currentUserId;
    }
    
    public static void forceRegistrationCheck() {
        if (currentUserId != null) {
            Log.w(TAG, "🔍 Force registration check for user: " + currentUserId);
            registrationVerified = false;
            ensureConnection();
        }
    }
    
    public static void verifyRegistration() {
        if (socket != null && socket.connected() && currentUserId != null) {
            try {
                org.json.JSONObject verificationData = new org.json.JSONObject();
                verificationData.put("userId", currentUserId);
                verificationData.put("timestamp", System.currentTimeMillis());
                
                socket.emit("verify-registration", verificationData);
                Log.d(TAG, "🔍 Registration verification requested for user: " + currentUserId);
            } catch (org.json.JSONException e) {
                // Fallback to simple verification
                socket.emit("verify-registration", currentUserId);
                Log.d(TAG, "🔍 Simple registration verification requested for user: " + currentUserId);
            }
        }
    }

    public static void ensureConnection() {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "🔄 Socket not connected, attempting to reconnect...");
            initializeSocket();
            
            // Re-register user if we have the current user ID
            if (currentUserId != null) {
                Log.d(TAG, "🔄 Re-registering user after reconnection: " + currentUserId);
                // Wait a bit for the connection to establish
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (socket != null && socket.connected()) {
                        registerUser(currentUserId);
                    }
                }, 1000);
            }
        } else if (currentUserId != null && !registrationVerified) {
            // Connected but registration not verified, check registration status
            Log.w(TAG, "🔍 Connected but registration not verified, checking status...");
            verifyRegistration();
            
            // If still not verified after a short delay, re-register
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!registrationVerified && currentUserId != null) {
                    Log.w(TAG, "⚠️ Registration still not verified, re-registering user: " + currentUserId);
                    registerUser(currentUserId);
                }
            }, 2000);
        }
    }
    
    private static void ensureUserRegistered() {
        if (currentUserId != null && !registrationVerified) {
            Log.w(TAG, "⚠️ Operation attempted but user not properly registered, ensuring registration...");
            ensureConnection();
        }
    }
    
    // Force sync method for app resume or when coming back from auto-login
    public static void forceSyncOnResume(String userId) {
        Log.d(TAG, "🔄 Force sync on resume for user: " + userId);
        
        // Force registration check
        forceRegistrationCheck();
        
        // Wait a moment then verify registration and sync
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (currentUserId != null && currentUserId.equals(userId)) {
                verifyRegistration();
                requestStatusSync(userId);
                Log.d(TAG, "✅ Force sync completed for user: " + userId);
            }
        }, 1000);
    }
    
    // Enhanced method specifically for auto-login scenarios
    public static void registerUserForAutoLogin(String userId) {
        Log.d(TAG, "🔑 AUTO-LOGIN: Starting registration for auto-login user: " + userId);
        
        // Set current user immediately to prevent race conditions
        currentUserId = userId;
        registrationVerified = false; // Reset verification status
        
        // Initialize socket if needed
        if (socket == null) {
            Log.d(TAG, "🔄 AUTO-LOGIN: Socket null, initializing...");
            initializeSocket();
        }
        
        // Wait for socket connection if needed
        if (socket != null && !socket.connected()) {
            Log.d(TAG, "🔄 AUTO-LOGIN: Socket not connected, connecting...");
            socket.connect();
            
            // Wait for connection then register
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (socket != null && socket.connected()) {
                    Log.d(TAG, "✅ AUTO-LOGIN: Socket connected, proceeding with registration");
                    registerUser(userId);
                } else {
                    Log.e(TAG, "❌ AUTO-LOGIN: Failed to connect socket for auto-login");
                    // Retry once more with forced connection
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        ensureConnection();
                        if (socket != null && socket.connected()) {
                            Log.d(TAG, "🔁 AUTO-LOGIN: Retry successful, registering user");
                            registerUser(userId);
                        } else {
                            Log.e(TAG, "❌ AUTO-LOGIN: Final retry failed");
                        }
                    }, 2000);
                }
            }, 1000);
        } else if (socket != null && socket.connected()) {
            // Socket is ready, register immediately
            Log.d(TAG, "✅ AUTO-LOGIN: Socket ready, registering user immediately");
            registerUser(userId);
        }
        
        // Additional safety measure: Force verification after registration
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!registrationVerified && currentUserId != null && currentUserId.equals(userId)) {
                Log.w(TAG, "⚠️ AUTO-LOGIN: Registration not verified after 3 seconds, forcing verification");
                verifyRegistration();
            }
        }, 3000);
    }
    
    // Check if a user is online before initiating calls
    public static void checkUserOnlineStatus(String userId, UserStatusCallback callback) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("requesterId", currentUserId);
                data.put("timestamp", System.currentTimeMillis());
                
                // Set up temporary listener for user status response
                socket.once("user-status-response", args -> {
                    try {
                        JSONObject response = (JSONObject) args[0];
                        String checkedUserId = response.getString("userId");
                        boolean isOnline = response.getBoolean("isOnline");
                        String status = response.optString("status", "unknown");
                        
                        Log.d(TAG, "👤 USER STATUS CHECK - User: " + checkedUserId + 
                                  ", Online: " + isOnline + ", Status: " + status);
                        
                        if (callback != null && checkedUserId.equals(userId)) {
                            callback.onUserStatusReceived(userId, isOnline, status);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user status response", e);
                        if (callback != null) {
                            callback.onUserStatusReceived(userId, false, "error");
                        }
                    }
                });
                
                socket.emit("check-user-status", data);
                Log.d(TAG, "❓ CHECKING USER STATUS - User: " + userId);
                
                // Set timeout for status check
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (callback != null) {
                        Log.w(TAG, "⏰ User status check timeout for user: " + userId);
                        callback.onUserStatusReceived(userId, false, "timeout");
                    }
                }, 5000); // 5 second timeout
                
            } catch (JSONException e) {
                Log.e(TAG, "Error checking user status", e);
                if (callback != null) {
                    callback.onUserStatusReceived(userId, false, "error");
                }
            }
        } else {
            Log.w(TAG, "⚠️ Cannot check user status - socket not connected");
            if (callback != null) {
                callback.onUserStatusReceived(userId, false, "no_connection");
            }
        }
    }
    
    // Interface for user status callback
    public interface UserStatusCallback {
        void onUserStatusReceived(String userId, boolean isOnline, String status);
    }
    
    // Check server reachability before attempting socket connection
    public static void checkServerReachability(ServerReachabilityCallback callback) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(SERVER_URL.replace("/socket.io", ""));
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                boolean isReachable = responseCode == 200 || responseCode == 404; // 404 is OK for socket.io endpoints
                
                Log.d(TAG, "🌐 Server reachability test - URL: " + url + ", Response: " + responseCode + ", Reachable: " + isReachable);
                
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onServerReachabilityResult(isReachable, responseCode, null));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Server reachability test failed: " + e.getMessage());
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onServerReachabilityResult(false, -1, e.getMessage()));
                }
            }
        }).start();
    }
    
    // Interface for server reachability callback
    public interface ServerReachabilityCallback {
        void onServerReachabilityResult(boolean isReachable, int responseCode, String error);
    }
    
    // Debug method to print complete socket state
    public static void printSocketState() {
        Log.d(TAG, "=================== SOCKET STATE DEBUG ===================");
        Log.d(TAG, "Server URL: " + SERVER_URL);
        Log.d(TAG, "Socket object: " + (socket != null ? "EXISTS" : "NULL"));
        if (socket != null) {
            Log.d(TAG, "Socket ID: " + socket.id());
            Log.d(TAG, "Socket connected: " + socket.connected());
        }
        Log.d(TAG, "Current User ID: " + currentUserId);
        Log.d(TAG, "Registration verified: " + registrationVerified);
        Log.d(TAG, "Registration callback: " + (registrationCallback != null ? "SET" : "NULL"));
        Log.d(TAG, "Ready for calls: " + isReadyForCalls());
        Log.d(TAG, "========================================================");
    }
    
    // Diagnostic method to check connection state before calls
    public static boolean isReadyForCalls() {
        boolean socketConnected = socket != null && socket.connected();
        boolean userRegistered = registrationVerified;
        boolean hasUserId = currentUserId != null && !currentUserId.isEmpty();
        
        Log.d(TAG, "📋 CALL READINESS CHECK:");
        Log.d(TAG, "  • Server URL: " + SERVER_URL);
        Log.d(TAG, "  • Socket exists: " + (socket != null));
        Log.d(TAG, "  • Socket connected: " + socketConnected);
        Log.d(TAG, "  • User registered: " + userRegistered);
        Log.d(TAG, "  • Has user ID: " + hasUserId + (hasUserId ? " (" + currentUserId + ")" : ""));
        Log.d(TAG, "  • Socket ID: " + (socket != null ? socket.id() : "null"));
        Log.d(TAG, "  • Ready for calls: " + (socketConnected && userRegistered && hasUserId));
        
        if (!socketConnected) {
            Log.w(TAG, "❌ Socket connection issue - Server: " + SERVER_URL);
            Log.w(TAG, "  • Socket null: " + (socket == null));
            Log.w(TAG, "  • Socket connected: " + (socket != null ? socket.connected() : "N/A"));
        }
        if (!userRegistered) {
            Log.w(TAG, "❌ User registration issue - User: " + currentUserId);
            Log.w(TAG, "  • Registration verified: " + registrationVerified);
        }
        if (!hasUserId) {
            Log.w(TAG, "❌ User ID issue - Current user: " + currentUserId);
        }
        
        // Force detailed logging for debugging
        Log.d(TAG, "🔍 FORCE DEBUG - Socket null: " + (socket == null) + 
                  ", Connected: " + (socket != null ? socket.connected() : false) + 
                  ", Registered: " + registrationVerified + 
                  ", UserID: " + currentUserId);
        
        return socketConnected && userRegistered && hasUserId;
    }
    
    // Enhanced connection testing with detailed diagnostics
    public static void performConnectionDiagnostics(String userId) {
        Log.d(TAG, "🔍 STARTING CONNECTION DIAGNOSTICS");
        Log.d(TAG, "  • Target Server: " + SERVER_URL);
        Log.d(TAG, "  • User ID: " + userId);
        Log.d(TAG, "  • Current Time: " + new java.util.Date());
        
        // First check server reachability
        checkServerReachability(new ServerReachabilityCallback() {
            @Override
            public void onServerReachabilityResult(boolean isReachable, int responseCode, String error) {
                if (isReachable) {
                    Log.d(TAG, "✅ Server is reachable (Response: " + responseCode + ")");
                    proceedWithSocketDiagnostics(userId);
                } else {
                    Log.e(TAG, "❌ Server is NOT reachable");
                    Log.e(TAG, "  • Response Code: " + responseCode);
                    Log.e(TAG, "  • Error: " + (error != null ? error : "Unknown"));
                    Log.e(TAG, "💡 TROUBLESHOOTING:");
                    Log.e(TAG, "   1. Check if server is running");
                    Log.e(TAG, "   2. Verify server URL: " + SERVER_URL);
                    Log.e(TAG, "   3. Check network connectivity");
                    Log.e(TAG, "   4. Check firewall/proxy settings");
                }
            }
        });
    }
    
    // Method specifically for call verification - waits for proper registration
    public static void verifyRegistrationForCall(String userId, RegistrationVerificationCallback callback) {
        Log.d(TAG, "📞 CALL VERIFICATION: Starting registration verification for calls");
        Log.d(TAG, "  • User ID: " + userId);
        Log.d(TAG, "  • Current Socket User: " + currentUserId);
        Log.d(TAG, "  • Registration Verified: " + registrationVerified);
        
        // Check if already properly registered
        if (registrationVerified && userId.equals(currentUserId) && socket != null && socket.connected()) {
            Log.d(TAG, "✅ CALL VERIFICATION: Already properly registered");
            if (callback != null) {
                callback.onVerificationResult(true, "Already registered");
            }
            return;
        }
        
        // Force registration for auto-login scenarios
        Log.d(TAG, "🔄 CALL VERIFICATION: Forcing registration for call");
        registerUserForAutoLogin(userId);
        
        // Wait and verify with timeout
        final int[] attempts = {0};
        final int maxAttempts = 10; // 10 attempts = 5 seconds
        
        android.os.Handler verificationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable verificationRunnable = new Runnable() {
            @Override
            public void run() {
                attempts[0]++;
                boolean isVerified = registrationVerified && userId.equals(currentUserId) && socket != null && socket.connected();
                
                Log.d(TAG, "🔍 CALL VERIFICATION: Attempt " + attempts[0] + "/" + maxAttempts + 
                          " - Verified: " + isVerified);
                
                if (isVerified) {
                    Log.d(TAG, "✅ CALL VERIFICATION: Registration verified successfully");
                    if (callback != null) {
                        callback.onVerificationResult(true, "Registration verified");
                    }
                } else if (attempts[0] >= maxAttempts) {
                    Log.e(TAG, "❌ CALL VERIFICATION: Timeout after " + maxAttempts + " attempts");
                    String error = "Registration timeout. Socket: " + (socket != null ? "exists" : "null") + 
                                  ", Connected: " + (socket != null ? socket.connected() : false) + 
                                  ", Verified: " + registrationVerified + 
                                  ", UserMatch: " + userId.equals(currentUserId);
                    if (callback != null) {
                        callback.onVerificationResult(false, error);
                    }
                } else {
                    // Continue checking
                    verificationHandler.postDelayed(this, 500);
                }
            }
        };
        
        verificationHandler.postDelayed(verificationRunnable, 500);
    }
    
    // Interface for registration verification callback specifically for calls
    public interface RegistrationVerificationCallback {
        void onVerificationResult(boolean isVerified, String message);
    }
    
    private static void proceedWithSocketDiagnostics(String userId) {
        // Check socket state
        if (socket == null) {
            Log.e(TAG, "❌ Socket is null - will attempt to initialize");
            initializeSocket();
            
            // Wait for initialization and test again
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                testSocketAfterInit(userId);
            }, 2000);
            return;
        }
        
        testSocketAfterInit(userId);
    }
    
    private static void testSocketAfterInit(String userId) {
        Log.d(TAG, "  • Socket ID: " + (socket != null ? socket.id() : "null"));
        Log.d(TAG, "  • Socket Connected: " + (socket != null ? socket.connected() : false));
        Log.d(TAG, "  • Registration Verified: " + registrationVerified);
        
        // Test basic connectivity
        if (socket != null && socket.connected()) {
            testConnection(userId, "diagnostic_test");
            
            // Test registration status
            if (!registrationVerified) {
                Log.w(TAG, "🔄 Registration not verified, attempting to register...");
                registerUser(userId);
            }
        } else if (socket != null) {
            Log.e(TAG, "❌ Socket not connected, attempting reconnection...");
            socket.connect();
            
            // Wait and test again
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (socket.connected()) {
                    Log.d(TAG, "✅ Reconnection successful");
                    registerUser(userId);
                } else {
                    Log.e(TAG, "❌ Reconnection failed - server may be unreachable");
                    Log.e(TAG, "💡 Check if server is running at: " + SERVER_URL);
                }
            }, 2000);
        } else {
            Log.e(TAG, "❌ Socket is null after initialization attempt");
        }
    }

    public static void testConnection(String userId, String chatId) {
        ensureUserRegistered();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("chatId", chatId);
                data.put("timestamp", System.currentTimeMillis());
                data.put("registrationVerified", registrationVerified);
                data.put("socketId", socket.id());
                socket.emit("test-connection", data);
                Log.d(TAG, "🧪 DETAILED Connection test sent for user: " + userId + ", chat: " + chatId + 
                          ", verified: " + registrationVerified + ", socket: " + socket.id());
            } catch (JSONException e) {
                Log.e(TAG, "Error sending connection test", e);
            }
        } else {
            Log.w(TAG, "🧪 Cannot test connection - socket not connected");
            Log.w(TAG, "📊 Socket state - Null: " + (socket == null) + ", Connected: " + (socket != null ? socket.connected() : "N/A"));
            ensureConnection();
        }
    }

    // Message Status Methods
    public static void emitMessageSent(String messageId, String chatId, String senderId) {
        ensureUserRegistered();
        ensureConnection();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("senderId", senderId);
                socket.emit("message-sent", data);
                Log.d(TAG, "📤 EMITTED message-sent - ID: " + messageId + ", Chat: " + chatId + ", Sender: " + senderId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-sent", e);
            }
        } else {
            Log.e(TAG, "❌ FAILED to emit message-sent - Socket not connected");
        }
    }

    public static void emitMessageDelivered(String messageId, String chatId, String userId) {
        ensureUserRegistered();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("message-delivered", data);
                Log.d(TAG, "📬 EMITTED message-delivered - ID: " + messageId + ", Chat: " + chatId + ", User: " + userId + ", Verified: " + registrationVerified);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-delivered", e);
            }
        }
    }

    public static void emitMessageRead(String messageId, String chatId, String userId) {
        ensureUserRegistered();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("chatId", chatId);
                data.put("userId", userId);
                socket.emit("message-read", data);
                Log.d(TAG, "📖 EMITTED message-read - ID: " + messageId + ", Chat: " + chatId + ", User: " + userId + ", Verified: " + registrationVerified);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-read", e);
            }
        }
    }

    // Emit real-time message for immediate delivery to other users in the chat
    public static void emitRealtimeMessage(String messageId, String content, String senderId, String senderName, String chatId) {
        ensureUserRegistered();
        ensureConnection();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("messageId", messageId);
                data.put("text", content);
                data.put("senderID", senderId);
                data.put("senderName", senderName);
                data.put("chatID", chatId);
                data.put("receiverID", ""); // Server will determine receivers from chat participants
                data.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(new java.util.Date())); // Use ISO format timestamp
                socket.emit("send-message", data);
                Log.d(TAG, "📡 EMITTED real-time message - ID: " + messageId + ", Chat: " + chatId + ", Sender: " + senderName + ", Content: '" + content + "'");
                
                // Log registration status for debugging
                Log.d(TAG, "📊 Message sent with registration status - User: " + currentUserId + ", Verified: " + registrationVerified);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting real-time message", e);
            }
        } else {
            Log.e(TAG, "❌ FAILED to emit real-time message - Socket not connected");
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
        // Main status update listener
        socket.on("message-status-update", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String messageId = data.getString("messageId");
                String status = data.getString("status");
                Log.d(TAG, "📥 RECEIVED message-status-update - ID: " + messageId + " → " + status);
                listener.onMessageStatusChanged(messageId, status);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing message-status-update", e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in message-status-update", e);
            }
        });

        // Listen for bulk status sync completion
        socket.on("status-sync-complete", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                int syncCount = data.optInt("count", 0);
                String userId = data.optString("userId", "");
                String chatId = data.optString("chatId", "");

                Log.d(TAG, "📥 STATUS SYNC COMPLETE - User: " + userId +
                        ", Chat: " + (chatId.isEmpty() ? "ALL" : chatId) +
                        ", Synced: " + syncCount + " messages");

                if (syncCount > 0) {
                    listener.onBulkStatusSync(syncCount);
                    Log.d(TAG, "✅ Notified UI about " + syncCount + " status updates");
                } else {
                    Log.d(TAG, "ℹ️ No status updates needed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing status-sync-complete", e);
            }
        });

        // Listen for sync errors
        socket.on("sync-error", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String error = data.optString("error", "Unknown sync error");
                String userId = data.optString("userId", "");
                Log.e(TAG, "❌ SYNC ERROR - User: " + userId + ", Error: " + error);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing sync-error", e);
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

        // Listen for incoming messages in real-time
        socket.on("receive-message", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String messageId = data.optString("messageId", "");
                String content = data.optString("text", "");
                String senderId = data.optString("senderID", "");
                String senderName = data.optString("senderName", "");
                String chatId = data.optString("chatID", "");
                String timestamp = data.optString("timestamp", "");

                Log.d(TAG, "📥 RECEIVED real-time message - ID: " + messageId +
                        ", From: " + senderName + " (" + senderId + ")" +
                        ", Chat: " + chatId +
                        ", Content: '" + content + "'");

                listener.onNewMessageReceived(messageId, content, senderId, senderName, chatId, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing receive-message", e);
            }
        });

        // Listen for real-time reactions
        socket.on("reaction-added", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String messageId = data.getString("messageId");
                String userId = data.getString("userId");
                String userName = data.getString("userName");
                String emoji = data.getString("emoji");

                Log.d(TAG, "😊 RECEIVED reaction-added - Message: " + messageId + ", User: " + userName + ", Emoji: " + emoji);

                listener.onReactionAdded(messageId, userId, userName, emoji);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing reaction-added", e);
            }
        });

        socket.on("reaction-removed", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String messageId = data.getString("messageId");
                String userId = data.getString("userId");
                String userName = data.getString("userName");
                String emoji = data.getString("emoji");

                Log.d(TAG, "😔 RECEIVED reaction-removed - Message: " + messageId + ", User: " + userName + ", Emoji: " + emoji);

                listener.onReactionRemoved(messageId, userId, userName, emoji);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing reaction-removed", e);
            }
        });

        Log.d(TAG, "✅ Socket event listeners setup complete");
    }
}

// Setup listeners for chat list updates
public static void setupChatListListeners(ChatListListener listener) {
    if (socket != null && listener != null) {
        // Listen for chat list updates
        socket.on("chat-list-update", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String chatId = data.optString("chatId", "");
                String lastMessage = data.optString("lastMessage", "");
                String senderName = data.optString("senderName", "");
                String timestamp = data.optString("timestamp", "");

                Log.d(TAG, "📋 CHAT LIST UPDATE - Chat: " + chatId +
                        ", Message: '" + lastMessage + "'" +
                        ", From: " + senderName);

                listener.onChatListMessageUpdate(chatId, lastMessage, senderName, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing chat-list-update", e);
            }
        });

        Log.d(TAG, "✅ Chat list listeners setup complete");
    }
}

// Interface for handling socket events
public interface MessageStatusListener {
    void onMessageStatusChanged(String messageId, String status);
    void onUserTyping(String userId, String userName, boolean isTyping);
    void onUserPresenceChanged(String userId, boolean isInChat);
    void onBulkStatusSync(int syncCount);
    void onNewMessageReceived(String messageId, String content, String senderId, String senderName, String chatId, String timestamp);
    void onReactionAdded(String messageId, String userId, String userName, String emoji);
    void onReactionRemoved(String messageId, String userId, String userName, String emoji);
}

// Interface for handling chat list updates
public interface ChatListListener {
    void onChatListMessageUpdate(String chatId, String lastMessage, String senderName, String timestamp);
}

// Call Methods
public static void emitCallInitiate(String callId, String chatId, String callerId, String receiverId, String callerName, boolean isVideoCall) {
    ensureUserRegistered();
    ensureConnection();
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("chatId", chatId);
            data.put("callerId", callerId);
            data.put("receiverId", receiverId);
            data.put("callerName", callerName);
            data.put("isVideoCall", isVideoCall);
            data.put("timestamp", System.currentTimeMillis());
            data.put("callerSocketId", socket.id()); // Add caller's socket ID
            data.put("platform", "android"); // Add platform info
            
            // Check if we're properly registered before initiating call
            if (!registrationVerified) {
                Log.w(TAG, "⚠️ CALL WARNING: Attempting to initiate call without verified registration");
                // Force registration check
                forceRegistrationCheck();
            }
            
            socket.emit("call-initiate", data);
            Log.d(TAG, "📞 DETAILED CALL INITIATE - Call ID: " + callId + ", Caller: " + callerName + " (" + callerId + ")" + 
                      ", Receiver: " + receiverId + ", Video: " + isVideoCall + ", Verified: " + registrationVerified + 
                      ", Socket: " + socket.id());
            Log.d(TAG, "📋 Call data: " + data.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-initiate", e);
        }
    } else {
        Log.e(TAG, "❌ FAILED to emit call-initiate - Socket not connected");
        Log.e(TAG, "📊 Socket state - Null: " + (socket == null) + ", Connected: " + (socket != null ? socket.connected() : "N/A"));
    }
}

public static void emitCallAnswer(String callId, String callerId, String receiverId) {
    ensureUserRegistered();
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("callerId", callerId);
            data.put("receiverId", receiverId);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("call-answer", data);
            Log.d(TAG, "✅ EMITTED call-answer - Call ID: " + callId + ", Verified: " + registrationVerified);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-answer", e);
        }
    }
}

public static void emitCallDecline(String callId, String callerId, String receiverId) {
    ensureUserRegistered();
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("callerId", callerId);
            data.put("receiverId", receiverId);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("call-decline", data);
            Log.d(TAG, "❌ EMITTED call-decline - Call ID: " + callId + ", Verified: " + registrationVerified);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-decline", e);
        }
    }
}

public static void emitCallEnd(String callId, String callerId, String receiverId) {
    ensureUserRegistered();
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("callerId", callerId);
            data.put("receiverId", receiverId);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("call-end", data);
            Log.d(TAG, "📞 EMITTED call-end - Call ID: " + callId + ", Verified: " + registrationVerified);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-end", e);
        }
    } else {
        Log.e(TAG, "❌ FAILED to emit call-end - Socket not connected");
    }
}

// Immediate call termination with force stop
public static void forceCallTermination(String callId, String callerId, String receiverId, String reason) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("callerId", callerId);
            data.put("receiverId", receiverId);
            data.put("reason", reason);
            data.put("forceStop", true);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("call-force-end", data);
            Log.d(TAG, "🚫 FORCE CALL TERMINATION - Call ID: " + callId + ", Reason: " + reason);
        } catch (JSONException e) {
            Log.e(TAG, "Error forcing call termination", e);
        }
    }
}

public static void emitCallMuteToggle(String callId, String userId, String participantId, boolean isMuted) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("userId", userId);
            data.put("participantId", participantId);
            data.put("isMuted", isMuted);
            socket.emit("call-mute", data);
            Log.d(TAG, "🔇 EMITTED call-mute - Call ID: " + callId + ", Muted: " + isMuted);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-mute", e);
        }
    }
}

public static void emitCallVideoToggle(String callId, String userId, String participantId, boolean isVideoOn) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("callId", callId);
            data.put("userId", userId);
            data.put("participantId", participantId);
            data.put("isVideoOn", isVideoOn);
            socket.emit("call-video-toggle", data);
            Log.d(TAG, "📹 EMITTED call-video-toggle - Call ID: " + callId + ", Video: " + isVideoOn);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting call-video-toggle", e);
        }
    }
}

// Setup listeners for call events
public static void setupCallListeners(CallListener listener) {
    if (socket != null && listener != null) {
        // Listen for incoming calls
        socket.on("incoming-call", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                String chatId = data.getString("chatId");
                String callerId = data.getString("callerId");
                String callerName = data.optString("callerName", "Unknown Caller");
                boolean isVideoCall = data.getBoolean("isVideoCall");
                long timestamp = data.getLong("timestamp");

                Log.d(TAG, "📲 INCOMING CALL - From: " + callerName + " (" + callerId + "), Video: " + isVideoCall);

                listener.onIncomingCall(callId, chatId, callerId, callerName, isVideoCall, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing incoming-call", e);
            }
        });

        // Listen for call answered
        socket.on("call-answered", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                long timestamp = data.getLong("timestamp");

                Log.d(TAG, "✅ CALL ANSWERED - Call ID: " + callId);

                listener.onCallAnswered(callId, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-answered", e);
            }
        });

        // Listen for call declined
        socket.on("call-declined", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                long timestamp = data.getLong("timestamp");

                Log.d(TAG, "❌ CALL DECLINED - Call ID: " + callId);

                listener.onCallDeclined(callId, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-declined", e);
            }
        });

        // Listen for call ended
        socket.on("call-ended", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                long timestamp = data.getLong("timestamp");

                Log.d(TAG, "📞 CALL ENDED - Call ID: " + callId);

                listener.onCallEnded(callId, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-ended", e);
            }
        });

        // Listen for call failed
        socket.on("call-failed", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                String reason = data.optString("reason", "Unknown error");
                String callerId = data.optString("callerId", "");
                String receiverId = data.optString("receiverId", "");
                String details = data.optString("details", "");

                Log.e(TAG, "❌ CALL FAILED - Call ID: " + callId + 
                          ", Reason: " + reason + 
                          ", Caller: " + callerId + 
                          ", Receiver: " + receiverId + 
                          (details.isEmpty() ? "" : ", Details: " + details));

                // Check if it's specifically an offline error
                if (reason.toLowerCase().contains("offline") || reason.toLowerCase().contains("not found")) {
                    Log.w(TAG, "🔍 OFFLINE ERROR: User " + receiverId + " appears to be offline or not registered");
                    Log.w(TAG, "💡 TIP: Check if both users are properly registered and connected");
                }

                listener.onCallFailed(callId, reason);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-failed", e);
            }
        });

        // Listen for mute status updates
        socket.on("call-mute-status", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                String userId = data.getString("userId");
                boolean isMuted = data.getBoolean("isMuted");

                Log.d(TAG, "🔇 CALL MUTE STATUS - Call ID: " + callId + ", User: " + userId + ", Muted: " + isMuted);

                listener.onCallMuteStatus(callId, userId, isMuted);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-mute-status", e);
            }
        });

        // Listen for video status updates
        socket.on("call-video-status", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                String userId = data.getString("userId");
                boolean isVideoOn = data.getBoolean("isVideoOn");

                Log.d(TAG, "📹 CALL VIDEO STATUS - Call ID: " + callId + ", User: " + userId + ", Video: " + isVideoOn);

                listener.onCallVideoStatus(callId, userId, isVideoOn);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-video-status", e);
            }
        });

        // Listen for force call termination
        socket.on("call-force-end", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callId = data.getString("callId");
                String reason = data.optString("reason", "Force terminated");

                Log.d(TAG, "🚫 CALL FORCE TERMINATED - Call ID: " + callId + ", Reason: " + reason);

                listener.onCallForceTerminated(callId, reason);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing call-force-end", e);
            }
        });

        Log.d(TAG, "✅ Call listeners setup complete");
    }
}

// Interface for handling call events
public interface CallListener {
    void onIncomingCall(String callId, String chatId, String callerId, String callerName, boolean isVideoCall, long timestamp);
    void onCallAnswered(String callId, long timestamp);
    void onCallDeclined(String callId, long timestamp);
    void onCallEnded(String callId, long timestamp);
    void onCallFailed(String callId, String reason);
    void onCallMuteStatus(String callId, String userId, boolean isMuted);
    void onCallVideoStatus(String callId, String userId, boolean isVideoOn);
    void onVoiceDataReceived(String audioData, String senderId);
    void onVideoDataReceived(String videoData, String senderId);
    void onCallForceTerminated(String callId, String reason); // New method for force termination
}

// Voice streaming methods - optimized for low latency
public static void emitVoiceData(String audioData, String chatId, String userId) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("audioData", audioData);
            data.put("chatId", chatId);
            data.put("userId", userId);
            data.put("timestamp", System.currentTimeMillis());
            data.put("priority", "high"); // Mark as high priority for low latency
            socket.emit("voice-data", data);
            // Remove verbose logging to reduce latency
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting voice data", e);
        }
    } else {
        Log.w(TAG, "Cannot emit voice data - socket not connected");
        ensureConnection();
    }
}

public static void emitVideoData(String videoData, String chatId, String userId) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("videoData", videoData);
            data.put("chatId", chatId);
            data.put("userId", userId);
            data.put("timestamp", System.currentTimeMillis());
            data.put("priority", "high"); // Mark as high priority for low latency
            socket.emit("video-data", data);
            // Reduced logging for performance
            if (videoData.length() > 50000) { // Only log large chunks
                Log.d(TAG, "📹 Emitted large video chunk - Size: " + videoData.length());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting video data", e);
        }
    } else {
        Log.w(TAG, "Cannot emit video data - socket not connected");
        ensureConnection();
    }
}

public static void setupVoiceListeners(CallListener listener) {
    if (socket == null || listener == null) return;

    // Optimized voice data listener for low latency
    socket.on("voice-data", args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String audioData = data.getString("audioData");
            String senderId = data.getString("userId");

            // Process immediately without extensive logging
            listener.onVoiceDataReceived(audioData, senderId);
        } catch (Exception e) {
            Log.e(TAG, "Error processing voice data", e);
        }
    });

    // Optimized video data listener for low latency
    socket.on("video-data", args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String videoData = data.getString("videoData");
            String senderId = data.getString("userId");

            // Process immediately without extensive logging
            listener.onVideoDataReceived(videoData, senderId);
        } catch (Exception e) {
            Log.e(TAG, "Error processing video data", e);
        }
    });
    
    Log.d(TAG, "✅ Voice/Video listeners setup with low-latency optimization");
}

// Reaction Methods
public static void emitReactionAdded(String messageId, String chatId, String userId, String userName, String emoji) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("messageId", messageId);
            data.put("chatId", chatId);
            data.put("userId", userId);
            data.put("userName", userName);
            data.put("emoji", emoji);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("reaction-added", data);
            Log.d(TAG, "😊 EMITTED reaction-added - Message: " + messageId + ", User: " + userName + ", Emoji: " + emoji);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting reaction-added", e);
        }
    }
}

public static void emitReactionRemoved(String messageId, String chatId, String userId, String userName, String emoji) {
    if (socket != null && socket.connected()) {
        try {
            JSONObject data = new JSONObject();
            data.put("messageId", messageId);
            data.put("chatId", chatId);
            data.put("userId", userId);
            data.put("userName", userName);
            data.put("emoji", emoji);
            data.put("timestamp", System.currentTimeMillis());
            socket.emit("reaction-removed", data);
            Log.d(TAG, "😔 EMITTED reaction-removed - Message: " + messageId + ", User: " + userName + ", Emoji: " + emoji);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting reaction-removed", e);
        }
    }
}
}