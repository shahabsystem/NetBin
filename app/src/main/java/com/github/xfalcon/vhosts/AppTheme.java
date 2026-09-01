package com.github.xfalcon.vhosts;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public final class AppTheme {
    public static final String KEY_THEME = "THEME_MODE";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";
    public static final String SYSTEM = "system";
    private AppTheme() {}
    public static void apply(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = p.getString(KEY_THEME, SYSTEM);
        int value = SYSTEM.equals(mode) ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM :
                (DARK.equals(mode) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(value);
    }
}
