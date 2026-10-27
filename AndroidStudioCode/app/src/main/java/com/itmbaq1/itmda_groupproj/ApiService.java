
package com.itmbaq1.itmda_groupproj;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ApiService {

    @GET("api/dashboard")
    Call<DashboardResponse> getDashboard(
            @Header("Authorization") String token);

    @GET("api/progress")
    Call<ProgressResponse> getProgress(
            @Header("Authorization") String token);
}
