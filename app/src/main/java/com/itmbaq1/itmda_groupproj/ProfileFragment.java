
package com.itmbaq1.itmda_groupproj;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itmbaq1.itmda_groupproj.databinding.FragmentProfileBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private TokenManager tokenManager;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        tokenManager = new TokenManager(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        loadProfile();

        binding.tvEditProfile.setOnClickListener(v -> editName());

        binding.tvChangePassword.setOnClickListener(v -> changePassword());

        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private String getAuthToken() {
        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

    private void loadProfile() {
        String token = getAuthToken();

        if (token == null) {
            showMessage("Please log in again.");
            return;
        }

        ApiClient.sessions().getProfile(token).enqueue(
                new Callback<AuthDtos.AuthResponse>() {

                    @Override
                    public void onResponse(
                            Call<AuthDtos.AuthResponse> call,
                            Response<AuthDtos.AuthResponse> response) {

                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            binding.tvName.setText(response.body().fullName);
                            binding.tvEmail.setText(response.body().email);
                        } else if (response.code() == 401) {
                            tokenManager.clearToken();
                            showMessage("Session expired. Please log in again.");
                        } else {
                            showMessage("Could not load your profile.");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<AuthDtos.AuthResponse> call,
                            Throwable t) {
                        if (isAdded()) {
                            showMessage("Could not connect to the server.");
                        }
                    }
                });
    }

    private void editName() {
        EditText input = new EditText(requireContext());
        input.setHint("Enter your full name");
        input.setText(binding.tvName.getText().toString());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit profile")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();

                    if (name.isEmpty()) {
                        showMessage("Name cannot be empty.");
                        return;
                    }

                    saveName(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveName(String name) {
        String token = getAuthToken();

        if (token == null) {
            showMessage("Please log in again.");
            return;
        }

        AuthDtos.UpdateProfileRequest request =
                new AuthDtos.UpdateProfileRequest(name);

        ApiClient.sessions().updateProfile(token, request).enqueue(
                new Callback<AuthDtos.AuthResponse>() {

                    @Override
                    public void onResponse(
                            Call<AuthDtos.AuthResponse> call,
                            Response<AuthDtos.AuthResponse> response) {

                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            binding.tvName.setText(response.body().fullName);
                            showMessage("Profile updated.");
                        } else {
                            showMessage("Could not update profile.");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<AuthDtos.AuthResponse> call,
                            Throwable t) {
                        if (isAdded()) {
                            showMessage("Could not connect to the server.");
                        }
                    }
                });
    }

    private void changePassword() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        EditText currentPassword = new EditText(requireContext());
        currentPassword.setHint("Current password");
        currentPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText newPassword = new EditText(requireContext());
        newPassword.setHint("New password");
        newPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(currentPassword);
        layout.addView(newPassword);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change password")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String current = currentPassword.getText().toString();
                    String updated = newPassword.getText().toString();

                    if (current.isEmpty() || updated.length() < 8) {
                        showMessage(
                                "Enter your current password and a new password of at least 8 characters.");
                        return;
                    }

                    sendPasswordChange(current, updated);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordChange(String current, String updated) {
        String token = getAuthToken();

        if (token == null) {
            showMessage("Please log in again.");
            return;
        }

        AuthDtos.ChangePasswordRequest request =
                new AuthDtos.ChangePasswordRequest(current, updated);

        ApiClient.sessions().changePassword(token, request).enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response) {

                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            showMessage("Password changed successfully.");
                        } else {
                            showMessage(
                                    "Password change failed. Check your current password.");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded()) {
                            showMessage("Could not connect to the server.");
                        }
                    }
                });
    }

    private void logout() {
        String token = getAuthToken();

        // Clear the local session immediately.
        tokenManager.clearToken();

        if (token != null) {
            ApiClient.sessions().logout(token).enqueue(
                    new Callback<Void>() {
                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response) {
                            // Local logout has already happened.
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            // Local logout still works if the server is unavailable.
                        }
                    });
        }

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
