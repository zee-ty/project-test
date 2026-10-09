package com.itmbaq1.itmda_groupproj;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itmbaq1.itmda_groupproj.databinding.FragmentProfileBinding;

import java.util.function.Consumer;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // TODO: load real name/email from the backend or saved login data
        binding.tvName.setText("Student");
        binding.tvEmail.setText("student@example.com");

        binding.tvEditProfile.setOnClickListener(v ->
                showInputDialog("Edit name", "New name", false, newName -> {
                    if (!newName.isEmpty()) binding.tvName.setText(newName); // TODO: save to backend
                }));

        binding.tvChangePassword.setOnClickListener(v ->
                showInputDialog("Change password", "New password", true, newPass -> {
                    if (newPass.length() < 6)
                        Toast.makeText(requireContext(), "Min 6 characters", Toast.LENGTH_SHORT).show();
                    // must do: send to backend
                }));

        binding.btnLogout.setOnClickListener(v -> {
            // must do: clear saved token
            Intent i = new Intent(requireActivity(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void showInputDialog(String title, String hint, boolean password, Consumer<String> onOk) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        if (password) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title).setView(input)
                .setPositiveButton("Save", (d, w) -> onOk.accept(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
