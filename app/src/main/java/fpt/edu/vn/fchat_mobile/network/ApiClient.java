package fpt.edu.vn.fchat_mobile.network;

import android.util.Log;

import fpt.edu.vn.fchat_mobile.BuildConfig;
import fpt.edu.vn.fchat_mobile.services.ApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String TAG = "ApiClient";

    public static ApiService getService() {
        if (retrofit == null) {
            String baseUrl = BuildConfig.BASE_URL;
            Log.d(TAG, "Creating Retrofit instance with base URL: " + baseUrl);
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
                    
            Log.d(TAG, "Retrofit instance created successfully");
        }
        return retrofit.create(ApiService.class);
    }
}