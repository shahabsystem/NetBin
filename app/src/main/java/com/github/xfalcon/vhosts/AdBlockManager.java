package com.github.xfalcon.vhosts;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import com.github.xfalcon.vhosts.util.FileUtils;
import com.github.xfalcon.vhosts.util.HttpUtils;

import java.io.*;
import java.util.*;

/** Builds a merged hosts file from the user's source plus an optional ad/tracker blocklist. */
public final class AdBlockManager {
    public static final String ENABLED = "ADBLOCK_ENABLED";
    public static final String URL = "ADBLOCK_URL";
    public static final String CACHE_FILE = "adblock_hosts";
    private static final String EFFECTIVE_FILE = "effective_hosts";
    private AdBlockManager() {}

    public static boolean isEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(ENABLED, true);
    }

    public static int update(Context c) throws Exception {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        String url = p.getString(URL, c.getString(R.string.default_adblock_url));
        String data = HttpUtils.get(url);
        FileUtils.writeFile(c.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE), data);
        return countBlockDomains(data);
    }

    public static File prepareEffectiveHosts(Context c) throws Exception {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        StringBuilder out = new StringBuilder();
        Set<String> domains = new HashSet<>();
        boolean hasBase = false;
        boolean net = p.getBoolean(SettingsFragment.IS_NET, false);
        if (net) {
            try {
                String base = readAll(c.openFileInput(SettingsFragment.NET_HOST_FILE));
                out.append(base); hasBase = !base.trim().isEmpty();
            } catch (Exception ignored) {}
        } else {
            String uri = p.getString(SettingsFragment.HOSTS_URI, null);
            if (uri != null) {
                try {
                    String base = readAll(c.getContentResolver().openInputStream(Uri.parse(uri)));
                    out.append(base); hasBase = !base.trim().isEmpty();
                } catch (Exception ignored) {}
            }
        }
        if (!isEnabled(c)) return null;
        try {
            String block = readAll(c.openFileInput(CACHE_FILE));
            for (String line : block.split("\\r?\\n")) {
                String domain = parseDomain(line);
                if (domain != null && domains.add(domain)) out.append("\n0.0.0.0 ").append(domain).append("\n");
            }
        } catch (Exception ignored) {}
        if (!hasBase && domains.isEmpty()) return null;
        File f = new File(c.getFilesDir(), EFFECTIVE_FILE);
        FileUtils.writeFile(new FileOutputStream(f), out.toString());
        return f;
    }

    public static InputStream getEffectiveInput(Context c) throws Exception {
        File f = new File(c.getFilesDir(), EFFECTIVE_FILE);
        if (isEnabled(c) && f.exists()) return new FileInputStream(f);
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        if (p.getBoolean(SettingsFragment.IS_NET, false)) return c.openFileInput(SettingsFragment.NET_HOST_FILE);
        return c.getContentResolver().openInputStream(Uri.parse(p.getString(SettingsFragment.HOSTS_URI, null)));
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        StringBuilder b = new StringBuilder(); String s;
        while ((s=r.readLine()) != null) b.append(s).append('\n');
        r.close(); return b.toString();
    }

    private static String parseDomain(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) return null;
        String[] p = line.split("\\s+");
        if (p.length >= 2) {
            String ip=p[0]; String d=p[1].toLowerCase(Locale.US);
            if (ip.equals("0.0.0.0") || ip.equals("127.0.0.1") || ip.equals("::") || ip.equals("::1"))
                return d.endsWith(".") ? d.substring(0,d.length()-1) : d;
        }
        if (line.matches("(?i)^[a-z0-9][a-z0-9._-]+\\.[a-z]{2,}$")) return line;
        return null;
    }

    private static int countBlockDomains(String data) {
        int n=0; for(String line:data.split("\\r?\\n")) if(parseDomain(line)!=null) n++; return n;
    }
}
