package fpt.edu.vn.fchat_mobile.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static File saveBitmapToFile(Context context, Bitmap bitmap) {
        File file = new File(context.getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPath(Context context, Uri uri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}

