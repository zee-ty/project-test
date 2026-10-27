
package com.itmbaq1.itmda_groupproj;

public final class AuthDtos {

    private AuthDtos() {
    }

    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class RegisterRequest {
        public String fullName;
        public String email;
        public String password;

        public RegisterRequest(
                String fullName,
                String email,
                String password) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public int id;
        public String fullName;
        public String email;
        public String token;
        public String error;
    }

    public static class UpdateProfileRequest {
        public String fullName;

        public UpdateProfileRequest(String fullName) {
            this.fullName = fullName;
        }
    }

    public static class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;

        public ChangePasswordRequest(
                String currentPassword,
                String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }
}
