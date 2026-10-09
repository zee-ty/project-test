package com.itmbaq1.itmda_groupproj;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itmbaq1.itmda_groupproj.databinding.FragmentSessionBinding;

public class SessionFragment extends Fragment {
    private FragmentSessionBinding binding;
    private final ChatAdapter adapter = new ChatAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean codeSubmitted = false;
    private int questionsAnswered = 0;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChat.setAdapter(adapter);
        binding.btnAnalyse.setOnClickListener(v -> onSend());
        binding.btnEnd.setOnClickListener(v -> endSession());
    }

    private void onSend() {
        String text = binding.etInput.getText().toString().trim();
        if (text.isEmpty()) {
            binding.etInput.setError("Please enter something first");
            return;
        }
        addMessage(text, true);
        binding.etInput.setText("");

        if (!codeSubmitted) {
            codeSubmitted = true;
            binding.btnAnalyse.setEnabled(false);
            addMessage("Analysing your code...", false);

            // must do: send code to backend, detect language + generate questions
            handler.postDelayed(() -> {
                if (binding == null) return;
                binding.tvTitle.setText("CODE UNDERSTANDING SESSION");
                binding.tvSubtitle.setText("DETECTED LANGUAGE: PYTHON");
                binding.tvInputLabel.setText("Type your answers:");
                binding.etInput.setHint("Type your answer");
                binding.btnAnalyse.setEnabled(true);
                addMessage("What does range(5) do in this program?", false);
            }, 1500);
        } else {
            questionsAnswered++;
            // Needed next: send answer to backend and get evaluation + next question
            addMessage("Thanks! AI feedback will appear here.", false);
        }
    }

    private void addMessage(String text, boolean fromUser) {
        binding.tvChatPlaceholder.setVisibility(View.GONE);
        adapter.addMessage(new ChatMessage(text, fromUser));
        binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void endSession() {
        if (!codeSubmitted) {
            Toast.makeText(requireContext(), "Submit some code first", Toast.LENGTH_SHORT).show();
            return;
        }
        // Needed: get the final score + feedback
        Bundle args = new Bundle();
        args.putString("language", "Python");
        args.putInt("score", 100);
        args.putInt("answered", questionsAnswered);
        args.putInt("total", questionsAnswered);
        NavHostFragment.findNavController(this).navigate(R.id.action_session_to_results, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
