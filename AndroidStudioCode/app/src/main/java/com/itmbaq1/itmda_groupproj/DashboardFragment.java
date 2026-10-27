
package com.itmbaq1.itmda_groupproj;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itmbaq1.itmda_groupproj.databinding.FragmentDashboardBinding;
import com.itmbaq1.itmda_groupproj.databinding.ViewStatCardBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        showLoading();

        binding.btnStartSession.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_dashboard_to_session)
        );

        loadDashboard();
    }

    private void showLoading() {
        setCard(binding.cardAverage, "Average Score", "—");
        setCard(binding.cardSessions, "Sessions Completed", "—");
        setCard(binding.cardQuestions, "Questions Answered", "—");
        setCard(binding.cardLanguage, "Favourite Language", "—");

        showRecentLoading();
    }

    private void loadDashboard() {
        TokenManager tokenManager =
                new TokenManager(requireContext());

        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            showRecentStatus("Please log in again.");
            return;
        }

        if (!token.startsWith("Bearer ")) {
            token = "Bearer " + token;
        }
        ApiClient.getApiService().getDashboard(token).enqueue(
                new Callback<DashboardResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<DashboardResponse> call,
                            @NonNull Response<DashboardResponse> response) {

                        if (binding == null) {
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {
                            showRecentStatus(
                                    "Unable to load dashboard data."
                            );
                            return;
                        }

                        displayDashboard(response.body());
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<DashboardResponse> call,
                            @NonNull Throwable error) {

                        if (binding == null) {
                            return;
                        }

                        showRecentStatus(
                                "Could not connect to the server. "
                                        + "Please try again later."
                        );
                    }
                }
        );
    }

    private void displayDashboard(DashboardResponse data) {

        String average = data.averageScore == null
                ? "—"
                : String.format(
                Locale.getDefault(),
                "%.1f%%",
                data.averageScore
        );

        setCard(binding.cardAverage, "Average Score", average);

        setCard(
                binding.cardSessions,
                "Sessions Completed",
                String.valueOf(data.sessionsCompleted)
        );

        setCard(
                binding.cardQuestions,
                "Questions Answered",
                String.valueOf(data.questionsAnswered)
        );

        String language = data.favouriteLanguage == null
                || data.favouriteLanguage.trim().isEmpty()
                ? "—"
                : data.favouriteLanguage;

        setCard(
                binding.cardLanguage,
                "Favourite Language",
                language
        );

        List<SessionSummary> recent = new ArrayList<>();

        if (data.recentSessions != null) {
            for (DashboardResponse.RecentSession session
                    : data.recentSessions) {

                if (session == null) {
                    continue;
                }

                String sessionLanguage = session.language == null
                        ? "Unknown"
                        : session.language;

                String topic = session.topic == null
                        ? "General"
                        : session.topic;

                int score = session.score == null
                        ? 0
                        : session.score;

                recent.add(
                        new SessionSummary(
                                sessionLanguage,
                                topic,
                                score
                        )
                );
            }
        }

        showRecentSessions(recent);
    }

    private void showRecentLoading() {
        binding.progressRecent.setVisibility(View.VISIBLE);
        binding.tvRecentStatus.setVisibility(View.GONE);
        binding.rvRecent.setVisibility(View.GONE);
    }

    private void showRecentStatus(String message) {
        binding.progressRecent.setVisibility(View.GONE);
        binding.rvRecent.setVisibility(View.GONE);

        binding.tvRecentStatus.setText(message);
        binding.tvRecentStatus.setVisibility(View.VISIBLE);
    }

    private void showRecentSessions(List<SessionSummary> sessions) {
        binding.progressRecent.setVisibility(View.GONE);

        if (sessions == null || sessions.isEmpty()) {
            showRecentStatus("No recent sessions yet.");
            return;
        }

        binding.tvRecentStatus.setVisibility(View.GONE);
        binding.rvRecent.setVisibility(View.VISIBLE);

        binding.rvRecent.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvRecent.setAdapter(
                new SessionAdapter(sessions)
        );
    }

    private void setCard(
            ViewStatCardBinding card,
            String label,
            String value) {

        card.tvLabel.setText(label);
        card.tvValue.setText(value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}