
package com.itmbaq1.itmda_groupproj;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface SessionApi {

    // Login and registration
    @POST("api/auth/login")
    Call<AuthDtos.AuthResponse> login(
            @Body AuthDtos.LoginRequest body);

    @POST("api/auth/register")
    Call<AuthDtos.AuthResponse> register(
            @Body AuthDtos.RegisterRequest body);

    // Profile
    @GET("api/auth/profile")
    Call<AuthDtos.AuthResponse> getProfile(
            @Header("Authorization") String token);

    @PUT("api/auth/profile")
    Call<AuthDtos.AuthResponse> updateProfile(
            @Header("Authorization") String token,
            @Body AuthDtos.UpdateProfileRequest body);

    // Password and logout
    @POST("api/auth/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body AuthDtos.ChangePasswordRequest body);

    @POST("api/auth/logout")
    Call<Void> logout(
            @Header("Authorization") String token);

    // Existing code-session functionality
    @POST("api/sessions")
    Call<SessionDtos.StartSessionResponse> startSession(
            @Body SessionDtos.StartSessionRequest body);

    @POST("api/sessions/{id}/answers")
    Call<SessionDtos.AnswerResponse> submitAnswer(
            @Path("id") int id,
            @Body SessionDtos.SubmitAnswerRequest body);

    @POST("api/sessions/{id}/finish")
    Call<SessionDtos.FinishResponse> finishSession(
            @Path("id") int id);
}
