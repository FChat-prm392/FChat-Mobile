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
                
                // Add connection event listeners for debugging
                socket.on(Socket.EVENT_CONNECT, args -> {
                    Log.d(TAG, "🔌 SOCKET CONNECTED to " + SERVER_URL);
                });
                
                socket.on(Socket.EVENT_DISCONNECT, args -> {
                    Log.d(TAG, "🔌 SOCKET DISCONNECTED");
                });
                
                socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                    Log.e(TAG, "🔌 SOCKET CONNECTION ERROR: " + (args.length > 0 ? args[0].toString() : "Unknown"));
                });
                
                socket.connect();
                Log.d(TAG, "Socket initialized and connecting to " + SERVER_URL);
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
                Log.d(TAG, "📤 EMITTED message-sent - ID: " + messageId + ", Chat: " + chatId + ", Sender: " + senderId);
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
                Log.d(TAG, "📬 EMITTED message-delivered - ID: " + messageId + ", Chat: " + chatId + ", User: " + userId);
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
                Log.d(TAG, "📖 EMITTED message-read - ID: " + messageId + ", Chat: " + chatId + ", User: " + userId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting message-read", e);
            }
        }
    }
    
    // Emit real-time message for immediate delivery to other users in the chat
    public static void emitRealtimeMessage(String messageId, String content, String senderId, String senderName, String chatId) {
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
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting real-time message", e);
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
                socket.emit("call-initiate", data);
                Log.d(TAG, "📞 EMITTED call-initiate - Call ID: " + callId + ", Caller: " + callerName + ", Receiver: " + receiverId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting call-initiate", e);
            }
        }
    }

    public static void emitCallAnswer(String callId, String callerId, String receiverId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("callId", callId);
                data.put("callerId", callerId);
                data.put("receiverId", receiverId);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("call-answer", data);
                Log.d(TAG, "✅ EMITTED call-answer - Call ID: " + callId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting call-answer", e);
            }
        }
    }

    public static void emitCallDecline(String callId, String callerId, String receiverId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("callId", callId);
                data.put("callerId", callerId);
                data.put("receiverId", receiverId);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("call-decline", data);
                Log.d(TAG, "❌ EMITTED call-decline - Call ID: " + callId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting call-decline", e);
            }
        }
    }

    public static void emitCallEnd(String callId, String callerId, String receiverId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("callId", callId);
                data.put("callerId", callerId);
                data.put("receiverId", receiverId);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("call-end", data);
                Log.d(TAG, "📞 EMITTED call-end - Call ID: " + callId);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting call-end", e);
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
                    
                    Log.d(TAG, "❌ CALL FAILED - Call ID: " + callId + ", Reason: " + reason);
                    
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
    }
    
    // Voice streaming methods
    public static void emitVoiceData(String audioData, String chatId, String userId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("audioData", audioData);
                data.put("chatId", chatId);
                data.put("userId", userId);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("voice-data", data);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting voice data", e);
            }
        } else {
            Log.w(TAG, "Cannot emit voice data - socket not connected");
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
                socket.emit("video-data", data);
                Log.d(TAG, "Emitted video data - Chat: " + chatId + ", User: " + userId + ", Size: " + videoData.length());
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting video data", e);
            }
        } else {
            Log.w(TAG, "Cannot emit video data - socket not connected");
        }
    }
    
    public static void setupVoiceListeners(CallListener listener) {
        if (socket == null || listener == null) return;
        
        socket.on("voice-data", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String audioData = data.getString("audioData");
                String senderId = data.getString("userId");
                
                listener.onVoiceDataReceived(audioData, senderId);
            } catch (Exception e) {
                Log.e(TAG, "Error processing voice data", e);
            }
        });
        
        socket.on("video-data", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String videoData = data.getString("videoData");
                String senderId = data.getString("userId");
                
                listener.onVideoDataReceived(videoData, senderId);
            } catch (Exception e) {
                Log.e(TAG, "Error processing video data", e);
            }
        });
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