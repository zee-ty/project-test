
package com.itmbaq1.itmda_groupproj;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.itmbaq1.itmda_groupproj.databinding.FragmentProgressBinding;
import com.itmbaq1.itmda_groupproj.databinding.ViewLanguageRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentProgressBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        showEmptyProgress();

        binding.btnNewSession.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_progress_to_session)
        );

        loadProgress();
    }

    private void loadProgress() {

        ApiClient.getApiService().getProgress().enqueue(
                new Callback<ProgressResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ProgressResponse> call,
                            @NonNull Response<ProgressResponse> response) {

                        if (binding == null) {
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {
                            showErrorProgress(
                                    "Unable to load progress data."
                            );
                            return;
                        }

                        displayProgress(response.body());
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ProgressResponse> call,
                            @NonNull Throwable error) {

                        if (binding == null) {
                            return;
                        }

                        showErrorProgress(
                                "Could not connect to the server."
                        );
                    }
                }
        );
    }

    private void displayProgress(ProgressResponse data) {

        List<ProgressResponse.ScoreHistoryPoint> history =
                data.scoreHistory;

        float[] scores;
        double total = 0;
        int scoreCount = 0;

        if (history == null) {
            scores = new float[0];
        } else {
            List<Float> validScores = new ArrayList<>();

            for (ProgressResponse.ScoreHistoryPoint point : history) {
                if (point != null && point.score != null) {
                    float score = point.score.floatValue();

                    validScores.add(score);
                    total += score;
                    scoreCount++;
                }
            }

            scores = new float[validScores.size()];

            for (int i = 0; i < validScores.size(); i++) {
                scores[i] = validScores.get(i);
            }
        }

        if (scoreCount > 0) {
            binding.tvAverage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            total / scoreCount
                    )
            );
        } else {
            binding.tvAverage.setText("—");
        }

        if (data.improvementPercentage != null) {
            binding.tvImprovement.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%% change",
                            data.improvementPercentage
                    )
            );
        } else {
            binding.tvImprovement.setText("Not enough data yet");
        }

        setRow(binding.rowPython, "Python",
                findLanguageAverage(data, "Python"));

        setRow(binding.rowCSharp, "C#",
                findLanguageAverage(data, "C#"));

        setRow(binding.rowJava, "Java",
                findLanguageAverage(data, "Java"));

        setupChart(scores);

        List<SessionSummary> sessions = new ArrayList<>();

        if (data.sessionHistory != null) {
            for (ProgressResponse.SessionHistoryItem item
                    : data.sessionHistory) {

                if (item == null) {
                    continue;
                }

                String language = item.language == null
                        ? "Unknown" : item.language;

                String topic = item.topic == null
                        ? "General" : item.topic;

                int score = item.score == null
                        ? 0 : item.score;

                sessions.add(
                        new SessionSummary(language, topic, score)
                );
            }
        }

        binding.rvHistory.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvHistory.setAdapter(
                new SessionAdapter(sessions)
        );
    }

    private int findLanguageAverage(
            ProgressResponse data, String language) {

        if (data.languageAverages == null) {
            return 0;
        }

        for (ProgressResponse.LanguageAverage item
                : data.languageAverages) {

            if (item == null || item.language == null
                    || item.averageScore == null) {
                continue;
            }

            if (item.language.equalsIgnoreCase(language)
                    || (language.equalsIgnoreCase("C#")
                    && item.language.equalsIgnoreCase("CSharp"))) {

                return (int) Math.round(item.averageScore);
            }
        }

        return 0;
    }

    private void showEmptyProgress() {
        binding.tvAverage.setText("—");
        binding.tvImprovement.setText("Loading progress...");

        setRow(binding.rowPython, "Python", 0);
        setRow(binding.rowCSharp, "C#", 0);
        setRow(binding.rowJava, "Java", 0);

        setupChart(new float[0]);

        binding.rvHistory.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvHistory.setAdapter(
                new SessionAdapter(new ArrayList<>())
        );
    }

    private void showErrorProgress(String message) {
        binding.tvAverage.setText("—");
        binding.tvImprovement.setText(message);

        setRow(binding.rowPython, "Python", 0);
        setRow(binding.rowCSharp, "C#", 0);
        setRow(binding.rowJava, "Java", 0);

        setupChart(new float[0]);

        binding.rvHistory.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvHistory.setAdapter(
                new SessionAdapter(new ArrayList<>())
        );
    }

    private void setRow(
            ViewLanguageRowBinding row,
            String name,
            int percent) {

        int safePercent = Math.max(0, Math.min(percent, 100));

        row.tvName.setText(name);
        row.tvPercent.setText(safePercent + "%");
        row.progress.setProgress(safePercent);
    }

    private void setupChart(float[] scores) {

        if (scores == null || scores.length == 0) {
            binding.lineChart.clear();
            binding.lineChart.setNoDataText(
                    "No progress data available yet."
            );
            binding.lineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < scores.length; i++) {
            entries.add(new Entry(i + 1, scores[i]));
        }

        int dark = ContextCompat.getColor(
                requireContext(), R.color.brand_dark
        );

        LineDataSet set = new LineDataSet(
                entries, "Understanding score"
        );

        set.setColor(dark);
        set.setCircleColor(dark);
        set.setLineWidth(2f);
        set.setDrawValues(false);

        LineChart chart = binding.lineChart;

        chart.setData(new LineData(set));
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
