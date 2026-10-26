
package com.itmbaq1.itmda_groupproj;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    public static final String BASE_URL = "http://10.0.2.2:5212/";

    private static Retrofit retrofit;
    private static SessionApi sessionApi;
    private static ApiService apiService;

    private ApiClient() {
    }

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
                    .addConverterFactory(
                            GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static SessionApi sessions() {
        if (sessionApi == null) {
            sessionApi = getRetrofit()
                    .create(SessionApi.class);
        }

        return sessionApi;
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit()
                    .create(ApiService.class);
        }

        return apiService;
    }
}
