package com.itmbaq1.itmda_groupproj;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itmbaq1.itmda_groupproj.databinding.FragmentSessionBinding;

public class SessionFragment extends Fragment {
    private FragmentSessionBinding binding;
    private SessionViewModel viewModel;
    private final ChatAdapter adapter = new ChatAdapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        binding.btnAnalyse.setOnClickListener(v -> {
            String text = binding.etInput.getText().toString();
            if (text.trim().isEmpty()) {
                binding.etInput.setError("Please enter something first");
                return;
            }
            binding.etInput.setError(null);
            viewModel.send(text);
        });
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.btnEnd.setOnClickListener(v -> viewModel.finishSession());

        viewModel.messages.observe(getViewLifecycleOwner(), items -> {
            adapter.setMessages(items);
            binding.tvChatPlaceholder.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            if (!items.isEmpty()) {
                binding.rvChat.scrollToPosition(items.size() - 1);
            }
        });
        viewModel.title.observe(getViewLifecycleOwner(), binding.tvTitle::setText);
        viewModel.subtitle.observe(getViewLifecycleOwner(), binding.tvSubtitle::setText);
        viewModel.inputLabel.observe(getViewLifecycleOwner(), binding.tvInputLabel::setText);
        viewModel.inputHint.observe(getViewLifecycleOwner(), binding.etInput::setHint);
        viewModel.loading.observe(getViewLifecycleOwner(), busy -> {
            updateButtons(busy);
            if (Boolean.TRUE.equals(busy)) {
                binding.etInput.setText("");
                hideKeyboard();
            }
        });
        viewModel.canSend.observe(getViewLifecycleOwner(), enabled -> updateButtons(viewModel.loading.getValue()));
        viewModel.showRetry.observe(getViewLifecycleOwner(), show -> updateButtons(viewModel.loading.getValue()));
        viewModel.toast.observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            if (message.equals("Please enter something first")) {
                binding.etInput.setError(message);
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
            viewModel.toast.setValue(null);
        });
        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null || binding == null) return;
            Bundle args = new Bundle();
            args.putString("language", result.language == null ? "-" : result.language);
            args.putInt("score", result.overallScore);
            args.putInt("answered", result.questionsAnswered);
            args.putInt("total", result.totalQuestions);
            args.putString("feedback", result.feedback == null ? "" : result.feedback);
            viewModel.consumeResult();
            NavHostFragment.findNavController(this).navigate(R.id.action_session_to_results, args);
        });
    }

    private void updateButtons(Boolean loadingValue) {
        if (binding == null) return;
        boolean busy = Boolean.TRUE.equals(loadingValue);
        boolean canSend = Boolean.TRUE.equals(viewModel.canSend.getValue());
        boolean showRetry = Boolean.TRUE.equals(viewModel.showRetry.getValue());
        binding.btnAnalyse.setEnabled(!busy && canSend);
        binding.btnEnd.setEnabled(!busy);
        binding.btnRetry.setVisibility(!busy && showRetry ? View.VISIBLE : View.GONE);
        binding.etInput.setEnabled(!busy);
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager manager = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(binding.etInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
