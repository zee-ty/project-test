package com.itmbaq1.itmda_groupproj;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.itmbaq1.itmda_groupproj.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String pass = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        if (name.isEmpty()) { binding.tilName.setError("Name is required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.setError("Enter a valid email"); return; }
        if (pass.length() < 6) { binding.tilPassword.setError("Min 6 characters"); return; }
        if (!pass.equals(confirm)) { binding.tilConfirm.setError("Passwords do not match"); return; }

        // must do:  call backend register endpoint
        Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
