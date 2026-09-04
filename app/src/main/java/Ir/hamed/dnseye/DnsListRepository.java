package Ir.hamed.dnseye;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads the bundled DNS.TXT-derived list and exposes it to the DNS manager/benchmark. */
public final class DnsListRepository {
    private DnsListRepository() {}

    public static final class Entry {
        public final String name;
        public final String primary;
        public final String secondary;
        public Entry(String name, String primary, String secondary) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    public static List<Entry> load(Context context) {
        File remote = new File(context.getFilesDir(), "dns_list_remote.txt");
        if (remote.exists() && remote.length() > 0) {
            List<Entry> result = parse(remoteInput(remote));
            if (!result.isEmpty()) return result;
        }
        try {
            return parse(context.getResources().openRawResource(R.raw.dns_list));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static InputStream remoteInput(File file) {
        try { return new FileInputStream(file); } catch (Exception e) { return null; }
    }

    private static List<Entry> parse(InputStream input) {
        List<Entry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (input == null) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String name = line.substring(0, eq).trim();
                String[] parts = line.substring(eq + 1).split(",", -1);
                if (parts.length < 1) continue;
                String p = parts[0].trim();
                String s = parts.length > 1 ? parts[1].trim() : "";
                if (p.isEmpty() || !isIpv4(p) || (!s.isEmpty() && !isIpv4(s))) continue;
                String key = p + "|" + s;
                if (seen.add(key)) out.add(new Entry(name, p, s));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static List<String> allServers(Context context) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Entry e : load(context)) {
            if (seen.add(e.primary)) out.add(e.primary);
            if (!e.secondary.isEmpty() && seen.add(e.secondary)) out.add(e.secondary);
        }
        return out;
    }

    private static boolean isIpv4(String value) {
        String[] p = value.split("\\.", -1);
        if (p.length != 4) return false;
        try {
            for (String s : p) {
                if (s.isEmpty() || s.length() > 3) return false;
                int n = Integer.parseInt(s);
                if (n < 0 || n > 255) return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }
}
