
package com.itmbaq1.itmda_groupproj;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Temporary address for local testing.
    // Will update the port once the final backend is ready.
    private static final String BASE_URL = "http://10.0.2.2:5000/";

    private static ApiService apiService;

    public static ApiService getApiService() {
        if (apiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }

        return apiService;
    }
}

