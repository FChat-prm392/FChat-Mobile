package fpt.edu.vn.fchat_mobile.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

public class VideoRenderer {
    private static final String TAG = "VideoRenderer";
    
    private ImageView videoView;
    
    public VideoRenderer(ImageView videoView) {
        this.videoView = videoView;
        // Set scale type to center crop for better video display
        if (videoView != null) {
            videoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
    
    public void renderFrame(String encodedVideoData) {
        if (videoView == null || encodedVideoData == null) return;
        
        try {
            byte[] videoData = Base64.decode(encodedVideoData, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(videoData, 0, videoData.length);
            
            if (bitmap != null) {
                Log.d(TAG, "Rendering video frame: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                videoView.post(() -> {
                    videoView.setImageBitmap(bitmap);
                });
            } else {
                Log.e(TAG, "Failed to decode bitmap from video data");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rendering video frame", e);
        }
    }
}
