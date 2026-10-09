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

public class ProgressFragment extends Fragment {
    private FragmentProgressBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //****This is all placement data and needs to be
        // replaced by the backend Ai DB datra!!!****
        binding.tvAverage.setText("78%");
        binding.tvImprovement.setText("6% improvement");

        setRow(binding.rowPython, "Python", 85);
        setRow(binding.rowCSharp, "C#", 72);
        setRow(binding.rowJava, "Java", 64);

        setupChart(new float[]{63, 71, 67, 78, 76, 83, 92, 100});

        List<SessionSummary> history = new ArrayList<>();
        history.add(new SessionSummary("Python", "Loop", 100));
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(new SessionAdapter(history));

        binding.btnNewSession.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_progress_to_session));
    }

    private void setRow(ViewLanguageRowBinding row, String name, int percent) {
        row.tvName.setText(name);
        row.tvPercent.setText(percent + "%");
        row.progress.setProgress(percent);
    }

    private void setupChart(float[] scores) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) entries.add(new Entry(i + 1, scores[i]));

        int dark = ContextCompat.getColor(requireContext(), R.color.brand_dark);
        LineDataSet set = new LineDataSet(entries, "Understanding score");
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
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
