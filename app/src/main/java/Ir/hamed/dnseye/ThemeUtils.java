package Ir.hamed.dnseye;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public final class ThemeUtils {
    private ThemeUtils() {}
    public static void apply(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = p.getString("THEME", "system");
        if ("dark".equals(theme)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if ("light".equals(theme)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
