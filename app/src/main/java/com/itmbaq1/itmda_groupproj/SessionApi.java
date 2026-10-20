package com.itmbaq1.itmda_groupproj;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SessionApi {
    @POST("api/sessions")
    Call<SessionDtos.StartSessionResponse> startSession(@Body SessionDtos.StartSessionRequest body);

    @POST("api/sessions/{id}/answers")
    Call<SessionDtos.AnswerResponse> submitAnswer(
            @Path("id") int id,
            @Body SessionDtos.SubmitAnswerRequest body);

    @POST("api/sessions/{id}/finish")
    Call<SessionDtos.FinishResponse> finishSession(@Path("id") int id);
}
