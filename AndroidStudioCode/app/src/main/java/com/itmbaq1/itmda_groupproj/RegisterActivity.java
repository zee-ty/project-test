
package com.itmbaq1.itmda_groupproj;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.itmbaq1.itmda_groupproj.databinding.ActivityRegisterBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private boolean registrationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        if (registrationInProgress) return;

        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        if (name.isEmpty()) {
            binding.tilName.setError("Name is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }

        if (password.length() < 8) {
            binding.tilPassword.setError("Password must be at least 8 characters");
            return;
        }

        if (!password.equals(confirm)) {
            binding.tilConfirm.setError("Passwords do not match");
            return;
        }

        registrationInProgress = true;
        binding.btnRegister.setEnabled(false);

        AuthDtos.RegisterRequest request =
                new AuthDtos.RegisterRequest(name, email, password);

        ApiClient.sessions().register(request).enqueue(
                new Callback<AuthDtos.AuthResponse>() {

                    @Override
                    public void onResponse(
                            Call<AuthDtos.AuthResponse> call,
                            Response<AuthDtos.AuthResponse> response) {

                        registrationInProgress = false;
                        binding.btnRegister.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Account created! Please log in.",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                        } else {
                            String message;

                            if (response.code() == 409) {
                                message = "An account with this email already exists.";
                            } else if (response.code() == 400) {
                                message = "Please check your details and try again.";
                            } else {
                                message = "Registration failed. Please try again.";
                            }

                            Toast.makeText(
                                    RegisterActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<AuthDtos.AuthResponse> call,
                            Throwable t) {

                        registrationInProgress = false;
                        binding.btnRegister.setEnabled(true);

                        Toast.makeText(
                                RegisterActivity.this,
                                "Cannot connect to the server. Check that the backend is running.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}