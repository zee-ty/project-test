
package com.itmbaq1.itmda_groupproj;

import android.content.Context;
import android.content.SharedPreferences;

public final class TokenManager {

    private static final String PREFS_NAME = "CodeVerifyAuth";
    private static final String TOKEN_KEY = "auth_token";

    private final SharedPreferences preferences;

    public TokenManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        preferences.edit()
                .putString(TOKEN_KEY, token)
                .apply();
    }

    public String getToken() {
        return preferences.getString(TOKEN_KEY, null);
    }

    public boolean hasToken() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearToken() {
        preferences.edit()
                .remove(TOKEN_KEY)
                .apply();
    }
}
