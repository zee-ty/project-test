package com.itmbaq1.itmda_groupproj;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.itmbaq1.itmda_groupproj.databinding.FragmentResultsBinding;

public class ResultsFragment extends Fragment {
    private FragmentResultsBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            binding.tvLanguage.setText("DETECTED LANGUAGE: " + args.getString("language", "-").toUpperCase());
            binding.tvScore.setText(args.getInt("score") + "%");
            binding.tvAnswered.setText(args.getInt("answered") + "/" + args.getInt("total") + " Questions answered");
            // must do: set tvFeedback from the AI evaluation
        }
        NavController nav = NavHostFragment.findNavController(this);
        binding.btnNewSession.setOnClickListener(v -> nav.navigate(R.id.action_results_to_session));
        binding.btnViewProgress.setOnClickListener(v -> nav.navigate(R.id.action_results_to_progress));
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
