package fpt.edu.vn.fchat_mobile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class VideoStreamer implements SurfaceHolder.Callback {
    private static final String TAG = "VideoStreamer";
    
    private static final int VIDEO_WIDTH = 320;
    private static final int VIDEO_HEIGHT = 240;
    private static final int FRAME_RATE = 15;
    private static final int JPEG_QUALITY = 50;
    private static final long FRAME_INTERVAL_MS = 1000 / FRAME_RATE; // 66ms for 15 FPS
    
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean isStreaming = false;
    private boolean isCameraReady = false;
    private long lastFrameTime = 0;
    private String chatId;
    private String userId;
    private Context context;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    
    public interface VideoStreamListener {
        void onVideoDataReady(String videoData, String chatId, String userId);
        void onCameraReady();
        void onCameraError(String error);
    }
    
    private VideoStreamListener streamListener;
    
    public VideoStreamer(Context context, String chatId, String userId) {
        this.context = context;
        this.chatId = chatId;
        this.userId = userId;
    }
    
    public void setStreamListener(VideoStreamListener listener) {
        this.streamListener = listener;
    }
    
    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void startStreaming() {
        if (isStreaming || !isCameraReady) {
            Log.d(TAG, "Cannot start streaming - streaming: " + isStreaming + ", camera ready: " + isCameraReady);
            return;
        }
        
        if (camera != null) {
            try {
                // Set preview callback for streaming
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (isStreaming && streamListener != null) {
                            processVideoFrame(data, camera);
                        }
                    }
                });
                
                isStreaming = true;
                Log.d(TAG, "Video streaming started successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start video streaming", e);
                if (streamListener != null) {
                    streamListener.onCameraError("Failed to start streaming: " + e.getMessage());
                }
            }
        }
    }
    
    private void processVideoFrame(byte[] data, Camera camera) {
        try {
            // Throttle frame rate to prevent socket overload
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime < FRAME_INTERVAL_MS) {
                return; // Skip this frame
            }
            lastFrameTime = currentTime;
            
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();
            
            // Convert YUV to JPEG
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, 
                previewSize.width, previewSize.height, null);
            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            
            Rect rect = new Rect(0, 0, previewSize.width, previewSize.height);
            yuvImage.compressToJpeg(rect, 80, tempStream); // Higher quality for processing
            
            // Convert to bitmap for processing
            byte[] tempJpegData = tempStream.toByteArray();
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(tempJpegData, 0, tempJpegData.length);
            tempStream.close();
            
            if (originalBitmap != null) {
                // Process the bitmap (rotate and scale)
                Bitmap processedBitmap = processFrame(originalBitmap);
                
                // Convert back to JPEG
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
                
                byte[] jpegData = outputStream.toByteArray();
                String encodedVideo = Base64.encodeToString(jpegData, Base64.NO_WRAP);
                
                Log.d(TAG, "Processed video frame: original=" + originalBitmap.getWidth() + "x" + originalBitmap.getHeight() + 
                    ", processed=" + processedBitmap.getWidth() + "x" + processedBitmap.getHeight() + 
                    ", size=" + jpegData.length + " bytes");
                
                if (streamListener != null) {
                    streamListener.onVideoDataReady(encodedVideo, chatId, userId);
                }
                
                outputStream.close();
                originalBitmap.recycle();
                processedBitmap.recycle();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing video frame", e);
        }
    }
    
    private Bitmap processFrame(Bitmap originalBitmap) {
        try {
            Matrix matrix = new Matrix();
            
            // Get rotation angle based on camera orientation
            int rotationAngle = getCameraRotationAngle();
            
            // Apply rotation
            if (rotationAngle != 0) {
                matrix.postRotate(rotationAngle);
            }
            
            // Calculate scaling to fit target size while maintaining aspect ratio
            float scaleX = (float) VIDEO_WIDTH / originalBitmap.getWidth();
            float scaleY = (float) VIDEO_HEIGHT / originalBitmap.getHeight();
            float scale = Math.min(scaleX, scaleY); // Use smaller scale to fit within bounds
            
            matrix.postScale(scale, scale);
            
            // Create the processed bitmap
            Bitmap processedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, 
                originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            
            // If the processed bitmap is larger than target, crop it to center
            if (processedBitmap.getWidth() > VIDEO_WIDTH || processedBitmap.getHeight() > VIDEO_HEIGHT) {
                int x = Math.max(0, (processedBitmap.getWidth() - VIDEO_WIDTH) / 2);
                int y = Math.max(0, (processedBitmap.getHeight() - VIDEO_HEIGHT) / 2);
                int width = Math.min(VIDEO_WIDTH, processedBitmap.getWidth());
                int height = Math.min(VIDEO_HEIGHT, processedBitmap.getHeight());
                
                Bitmap croppedBitmap = Bitmap.createBitmap(processedBitmap, x, y, width, height);
                if (croppedBitmap != processedBitmap) {
                    processedBitmap.recycle();
                }
                return croppedBitmap;
            }
            
            return processedBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            return originalBitmap; // Return original if processing fails
        }
    }
    
    private int getCameraRotationAngle() {
        if (context == null) return 0;
        
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = getCurrentCameraId();
        Camera.getCameraInfo(cameraId, info);
        
        // For video streaming, we need a simpler rotation approach
        // Most Android cameras need 90 degrees rotation for portrait mode
        int result = 0;
        
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // Front camera: usually needs 270 degrees (or -90) for proper orientation
            result = 270;
        } else {  // back-facing
            // Back camera: usually needs 90 degrees for proper orientation
            result = 90;
        }
        
        Log.d(TAG, "Camera rotation angle: " + result + " degrees for " + 
            (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back") + " camera");
        
        return result;
    }
    
    public void stopStreaming() {
        isStreaming = false;
        
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping camera preview", e);
            }
        }
    }
    
    public void releaseCamera() {
        stopStreaming();
        isCameraReady = false;
        
        if (camera != null) {
            try {
                camera.release();
                camera = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera", e);
            }
        }
    }
    
    public void switchCamera() {
        if (!isCameraReady) return;
        
        boolean wasStreaming = isStreaming;
        stopStreaming();
        releaseCamera();
        
        // Switch camera
        currentCameraId = (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) ? 
            Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        
        // Reinitialize camera
        initializeCamera();
        
        if (wasStreaming && isCameraReady) {
            startStreaming();
        }
    }
    
    // SurfaceHolder.Callback methods
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        initializeCamera();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed: " + width + "x" + height);
        if (camera != null && isCameraReady) {
            try {
                camera.stopPreview();
                setupCameraParameters();
                setCameraDisplayOrientation();
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error updating camera preview", e);
            }
        }
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        releaseCamera();
    }
    
    private void initializeCamera() {
        try {
            camera = openCamera();
            if (camera == null) {
                Log.e(TAG, "Failed to open camera");
                if (streamListener != null) {
                    streamListener.onCameraError("Failed to open camera");
                }
                return;
            }
            
            setupCameraParameters();
            setCameraDisplayOrientation();
            
            if (surfaceHolder != null) {
                camera.setPreviewDisplay(surfaceHolder);
            }
            
            camera.startPreview();
            isCameraReady = true;
            
            Log.d(TAG, "Camera initialized successfully");
            if (streamListener != null) {
                streamListener.onCameraReady();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera", e);
            if (streamListener != null) {
                streamListener.onCameraError("Camera initialization failed: " + e.getMessage());
            }
            releaseCamera();
        }
    }
    
    private void setupCameraParameters() {
        if (camera == null) return;
        
        try {
            Camera.Parameters parameters = camera.getParameters();
            
            // Find best preview size
            Camera.Size bestSize = getBestPreviewSize(parameters);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
                Log.d(TAG, "Using preview size: " + bestSize.width + "x" + bestSize.height);
            } else {
                parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
                Log.d(TAG, "Using default size: " + VIDEO_WIDTH + "x" + VIDEO_HEIGHT);
            }
            
            parameters.setPreviewFormat(ImageFormat.NV21);
            
            // Set focus mode if supported
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            
            // Set frame rate if supported
            List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
            if (frameRates != null && frameRates.contains(FRAME_RATE)) {
                parameters.setPreviewFrameRate(FRAME_RATE);
            }
            
            camera.setParameters(parameters);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting camera parameters", e);
        }
    }
    
    private Camera openCamera() {
        Camera cam = null;
        int numberOfCameras = Camera.getNumberOfCameras();
        
        // Try to open the preferred camera first
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            
            if (cameraInfo.facing == currentCameraId) {
                try {
                    cam = Camera.open(i);
                    Log.d(TAG, "Opened camera " + i + " (facing: " + 
                        (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back") + ")");
                    return cam;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to open camera " + i + ": " + e.getMessage());
                }
            }
        }
        
        // If preferred camera failed, try any available camera
        for (int i = 0; i < numberOfCameras; i++) {
            try {
                cam = Camera.open(i);
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                currentCameraId = cameraInfo.facing;
                Log.d(TAG, "Opened fallback camera " + i + " (facing: " + 
                    (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back") + ")");
                return cam;
            } catch (Exception e) {
                Log.w(TAG, "Failed to open camera " + i + ": " + e.getMessage());
            }
        }
        
        Log.e(TAG, "No cameras available");
        return null;
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    public boolean isCameraReady() {
        return isCameraReady;
    }
    
    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size bestSize = null;
        
        // Find the best size that fits our criteria
        for (Camera.Size size : sizes) {
            // Prefer sizes close to our target resolution but not too large
            if (size.width <= 640 && size.height <= 480) {
                if (bestSize == null || 
                    (Math.abs(size.width - VIDEO_WIDTH) + Math.abs(size.height - VIDEO_HEIGHT) <
                     Math.abs(bestSize.width - VIDEO_WIDTH) + Math.abs(bestSize.height - VIDEO_HEIGHT))) {
                    bestSize = size;
                }
            }
        }
        
        // If no suitable size found, use the smallest available
        if (bestSize == null && !sizes.isEmpty()) {
            bestSize = sizes.get(sizes.size() - 1); // Usually the smallest
        }
        
        return bestSize;
    }
    
    private void setCameraDisplayOrientation() {
        if (camera == null || context == null) return;
        
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(getCurrentCameraId(), info);
        
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        
        try {
            camera.setDisplayOrientation(result);
            Log.d(TAG, "Set camera orientation to: " + result + " degrees");
        } catch (Exception e) {
            Log.e(TAG, "Error setting camera orientation", e);
        }
    }
    
    private int getCurrentCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == currentCameraId) {
                return i;
            }
        }
        return 0; // fallback to first camera
    }
}
