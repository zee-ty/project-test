package com.itmbaq1.itmda_groupproj;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("api/dashboard")
    Call<DashboardResponse> getDashboard();

    @GET("api/progress")
    Call<ProgressResponse> getProgress();
}
