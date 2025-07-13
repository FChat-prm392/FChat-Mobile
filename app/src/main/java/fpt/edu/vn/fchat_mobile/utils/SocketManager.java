package fpt.edu.vn.fchat_mobile.utils;

import android.util.Log;
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

    public static boolean isConnected() {
        return socket != null && socket.connected();
    }
}