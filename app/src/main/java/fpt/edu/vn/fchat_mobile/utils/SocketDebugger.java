package fpt.edu.vn.fchat_mobile.utils;

import android.util.Log;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketDebugger {
    private static final String TAG = "SocketDebugger";
    
    public static void enableDebugLogging() {
        Socket socket = SocketManager.getSocket();
        if (socket != null) {
            // Log all incoming events
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "🔌 DEBUG: Socket CONNECTED");
                }
            });
            
            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "🔌 DEBUG: Socket DISCONNECTED - Reason: " + (args.length > 0 ? args[0] : "Unknown"));
                }
            });
            
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "🔌 DEBUG: Socket CONNECTION ERROR - " + (args.length > 0 ? args[0] : "Unknown"));
                }
            });
            
            // Log all incoming message events
            socket.on("receive-message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "📥 DEBUG: receive-message event - Data: " + (args.length > 0 ? args[0].toString() : "No data"));
                }
            });
            
            socket.on("message-status-update", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "📥 DEBUG: message-status-update event - Data: " + (args.length > 0 ? args[0].toString() : "No data"));
                }
            });
            
            socket.on("chat-list-update", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "📥 DEBUG: chat-list-update event - Data: " + (args.length > 0 ? args[0].toString() : "No data"));
                }
            });
            
            Log.d(TAG, "🔧 DEBUG LOGGING ENABLED - Socket connected: " + socket.connected());
        } else {
            Log.e(TAG, "🔧 DEBUG: Socket is null!");
        }
    }
    
    public static void checkSocketStatus() {
        Socket socket = SocketManager.getSocket();
        Log.d(TAG, "📊 SOCKET STATUS CHECK:");
        Log.d(TAG, "   - Socket exists: " + (socket != null));
        Log.d(TAG, "   - Socket connected: " + (socket != null && socket.connected()));
        Log.d(TAG, "   - Socket ID: " + (socket != null ? socket.id() : "null"));
        Log.d(TAG, "   - IsConnected helper: " + SocketManager.isConnected());
    }
}
