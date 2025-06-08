package com.example.screenrotationhelper;

import android.content.Context;
import android.content.SharedPreferences;

// Bu sınıf, servisin aktif olup olmadığını telefon hafızasına kaydeder ve okur.
public class ServiceStateRepository {
    private static final String PREFERENCES_NAME = "service_state_prefs";
    private static final String KEY_IS_SERVICE_ENABLED = "is_service_enabled_by_user";

    private final SharedPreferences sharedPreferences;

    public ServiceStateRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void setServiceState(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_IS_SERVICE_ENABLED, isEnabled).apply();
    }

    public boolean isServiceEnabled() {
        // Varsayılan olarak false, yani kullanıcı başlatmadan servis çalışmaz.
        return sharedPreferences.getBoolean(KEY_IS_SERVICE_ENABLED, false);
    }
}