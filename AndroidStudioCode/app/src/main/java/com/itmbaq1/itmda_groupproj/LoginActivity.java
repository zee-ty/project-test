
package com.itmbaq1.itmda_groupproj;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.itmbaq1.itmda_groupproj.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private TokenManager tokenManager;
    private boolean loginInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        // If a token is already saved, continue to the main screen.
        if (tokenManager.hasToken()) {
            openMainActivity();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        if (loginInProgress) return;

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }

        if (password.length() < 8) {
            binding.tilPassword.setError(
                    "Password must be at least 8 characters");
            return;
        }

        loginInProgress = true;
        binding.btnLogin.setEnabled(false);

        AuthDtos.LoginRequest request =
                new AuthDtos.LoginRequest(email, password);

        ApiClient.sessions().login(request).enqueue(
                new Callback<AuthDtos.AuthResponse>() {

                    @Override
                    public void onResponse(
                            Call<AuthDtos.AuthResponse> call,
                            Response<AuthDtos.AuthResponse> response) {

                        loginInProgress = false;
                        binding.btnLogin.setEnabled(true);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().token != null
                                && !response.body().token.isEmpty()) {

                            tokenManager.saveToken(response.body().token);
                            openMainActivity();

                        } else {
                            String message = "Login failed. Check your details.";

                            if (response.code() == 401) {
                                message = "Incorrect email or password.";
                            } else if (response.code() == 400) {
                                message = "Please check your email and password.";
                            }

                            Toast.makeText(
                                    LoginActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<AuthDtos.AuthResponse> call,
                            Throwable t) {

                        loginInProgress = false;
                        binding.btnLogin.setEnabled(true);

                        Toast.makeText(
                                LoginActivity.this,
                                "Cannot connect to the server. Check that the backend is running.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}