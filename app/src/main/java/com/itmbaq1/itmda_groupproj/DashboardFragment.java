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

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // TODO: replace dummy values with data from the backend
        setCard(binding.cardAverage, "Average Score", "78%");
        setCard(binding.cardSessions, "Sessions Completed", "12");
        setCard(binding.cardQuestions, "Questions Answered", "53");
        setCard(binding.cardLanguage, "Favourite Language", "Python");

        List<SessionSummary> recent = new ArrayList<>();
        recent.add(new SessionSummary("Python", "Loops", 82));
        recent.add(new SessionSummary("C#", "Classes", 72));
        recent.add(new SessionSummary("Python", "Functions", 92));
        binding.rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecent.setAdapter(new SessionAdapter(recent));

        binding.btnStartSession.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_dashboard_to_session));
    }

    private void setCard(ViewStatCardBinding card, String label, String value) {
        card.tvLabel.setText(label);
        card.tvValue.setText(value);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
