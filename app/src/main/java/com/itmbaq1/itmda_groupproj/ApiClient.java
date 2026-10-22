package com.itmbaq1.itmda_groupproj;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    // Keeping the backend port used by the incoming backend branch.
    public static final String BASE_URL = "http://10.0.2.2:5212/";

    private static Retrofit retrofit;

    private ApiClient() {}

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(70, TimeUnit.SECONDS)
                    .writeTimeout(70, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofit().create(ApiService.class);
    }

    public static SessionApi sessions() {
        return getRetrofit().create(SessionApi.class);
    }
}

