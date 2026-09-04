from pathlib import Path
import re, json
root=Path('/mnt/data/work/netbin')
# Decode user DNS file (UTF-16 with BOM), normalize to UTF-8 app resource
raw=Path('/mnt/data/work/DNS.TXT').read_bytes()
text=raw.decode('utf-16')
lines=[]
for line in text.splitlines():
    line=line.strip()
    if line:
        lines.append(line)
(root/'app/src/main/res/raw').mkdir(parents=True, exist_ok=True)
(root/'app/src/main/res/raw/dns_list.txt').write_text('\n'.join(lines)+'\n', encoding='utf-8')

# New repository class
(root/'app/src/main/java/Ir/hamed/dnseye/DnsListRepository.java').write_text(r'''package Ir.hamed.dnseye;

import android.content.Context;

import java.io.BufferedReader;
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
        List<Entry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.dns_list), StandardCharsets.UTF_8))) {
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
''', encoding='utf-8')

# DnsBenchmark: use bundled DNS file and context
p=root/'app/src/main/java/Ir/hamed/dnseye/DnsBenchmark.java'
s=p.read_text()
s=s.replace('import android.content.SharedPreferences;','import android.content.Context;\nimport android.content.SharedPreferences;')
start=s.index('    public static List<String> getCandidateServers(')
end=s.index('\n    public static List<Result> test', start)
new=r'''    public static List<String> getCandidateServers(Context context, SharedPreferences prefs) {
        List<String> servers = new ArrayList<>();
        for (String ip : DnsListRepository.allServers(context)) {
            if (!servers.contains(ip)) servers.add(ip);
        }
        // A few stable public fallbacks are kept in addition to the bundled list.
        Collections.addAll(servers,
                "1.1.1.1", "1.0.0.1",
                "8.8.8.8", "8.8.4.4",
                "9.9.9.9", "149.112.112.112",
                "94.140.14.14", "94.140.15.15",
                "208.67.222.222", "208.67.220.220",
                "185.228.168.9", "185.228.169.9",
                "76.76.2.0", "76.76.10.0");
        try {
            JSONArray arr = new JSONArray(prefs.getString(SettingsFragment.CUSTOM_DNS_LIST, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String ip = arr.getJSONObject(i).optString("ip", "").trim();
                if (!ip.isEmpty() && !servers.contains(ip)) servers.add(ip);
            }
        } catch (Exception ignored) {}
        return servers;
    }
'''
s=s[:start]+new+s[end:]
p.write_text(s, encoding='utf-8')

# Update service call
p=root/'app/src/main/java/Ir/hamed/dnseye/vservice/VhostsService.java'
s=p.read_text().replace('DnsBenchmark.getCandidateServers(settings)', 'DnsBenchmark.getCandidateServers(VhostsService.this, settings)')
p.write_text(s, encoding='utf-8')

# SettingsFragment targeted replacement
p=root/'app/src/main/java/Ir/hamed/dnseye/SettingsFragment.java'
s=p.read_text()
s=s.replace('public static final String AUTO_DNS = "AUTO_DNS";', '''public static final String AUTO_DNS = "AUTO_DNS";
    public static final String DNS_TEST_TIMEOUT = "DNS_TEST_TIMEOUT";
    public static final String DNS_ONLY_BUNDLED = "DNS_ONLY_BUNDLED";
    public static final String DNS_LIST_URL = "DNS_LIST_URL";
    public static final String SETTINGS_URL = "SETTINGS_URL";''')
s=s.replace('''        findPreference("TEST_DNS").setOnPreferenceClickListener(p -> { testDnsFromSettings(); return true; });
        findPreference("EXPORT_SETTINGS").setOnPreferenceClickListener(p -> { exportSettings(); return true; });
        findPreference("IMPORT_SETTINGS").setOnPreferenceClickListener(p -> { importSettings(); return true; });''','''        findPreference("TEST_DNS").setOnPreferenceClickListener(p -> { testDnsFromSettings(); return true; });
        findPreference("LOAD_DNS_URL").setOnPreferenceClickListener(p -> { loadDnsListFromUrl(); return true; });
        findPreference("RESET_DNS").setOnPreferenceClickListener(p -> { resetDnsSettings(); return true; });
        findPreference("LOAD_SETTINGS_URL").setOnPreferenceClickListener(p -> { loadSettingsFromUrl(); return true; });
        findPreference("EXPORT_SETTINGS").setOnPreferenceClickListener(p -> { exportSettings(); return true; });
        findPreference("IMPORT_SETTINGS").setOnPreferenceClickListener(p -> { importSettings(); return true; });''')
# Replace manager methods section
start=s.index('    private static final String[] BUILTIN_DNS = {')
end=s.index('    private void testDnsFromSettings()', start)
new=r'''    private JSONArray getCustomDns() {
        try { return new JSONArray(getPreferenceScreen().getSharedPreferences().getString(CUSTOM_DNS_LIST, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void showDnsManager() {
        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        final LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(24, 12, 24, 8);
        TextView hint = new TextView(requireContext());
        hint.setText(getString(R.string.dns_manage_hint));
        hint.setPadding(0, 0, 0, 16);
        list.addView(hint);
        final android.widget.RadioGroup group = new android.widget.RadioGroup(requireContext());
        group.setOrientation(android.widget.RadioGroup.VERTICAL);
        String active = prefs.getString(IPV4_DNS, "1.1.1.1");
        for (DnsListRepository.Entry item : DnsListRepository.load(requireContext()))
            addDnsRow(group, item.name, item.primary, item.secondary, active, false);
        JSONArray custom = getCustomDns();
        for (int i = 0; i < custom.length(); i++) {
            try {
                JSONObject o = custom.getJSONObject(i);
                addDnsRow(group, o.optString("name", "سفارشی"), o.getString("ip"), "", active, true);
            } catch (Exception ignored) {}
        }
        list.addView(group);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_manage_dns)
                .setView(list)
                .setPositiveButton(R.string.dns_add, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> showAddDnsDialog(dialog)));
        dialog.show();
    }

    private void addDnsRow(android.widget.RadioGroup group, String name, String primary, String secondary, String active, boolean removable) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.RadioButton rb = new android.widget.RadioButton(requireContext());
        String label = name + "  •  " + primary + (secondary.isEmpty() ? "" : " / " + secondary);
        rb.setText(label);
        rb.setTextSize(15);
        rb.setChecked(primary.equals(active));
        row.addView(rb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (removable) {
            android.widget.Button del = new android.widget.Button(requireContext());
            del.setText("×");
            del.setMinWidth(48);
            del.setOnClickListener(v -> { removeCustomDns(primary); showDnsManager(); });
            row.addView(del, new LinearLayout.LayoutParams(56, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        rb.setOnClickListener(v -> {
            SharedPreferences.Editor e = getPreferenceScreen().getSharedPreferences().edit();
            e.putString(IPV4_DNS, primary).putString(IPV4_DNS2, secondary.isEmpty() ? primary : secondary)
                    .putBoolean(IS_CUS_DNS, true).apply();
            Preference p = findPreference(IPV4_DNS);
            Preference p2 = findPreference(IPV4_DNS2);
            if (p != null) p.setSummary(primary);
            if (p2 != null) p2.setSummary(secondary.isEmpty() ? primary : secondary);
            Toast.makeText(requireContext(), R.string.dns_selected, Toast.LENGTH_SHORT).show();
        });
        group.addView(row);
    }

    private void showAddDnsDialog(final AlertDialog parent) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 8, 40, 0);
        android.widget.EditText name = new android.widget.EditText(requireContext());
        name.setHint(R.string.dns_name);
        android.widget.EditText ip = new android.widget.EditText(requireContext());
        ip.setHint(R.string.dns_address);
        ip.setSingleLine(true);
        ip.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        box.addView(name); box.addView(ip);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.dns_add).setView(box)
                .setPositiveButton(R.string.dialog_confirm, (d, w) -> {
                    String address = ip.getText().toString().trim();
                    try { Address.getByAddress(address); }
                    catch (Exception e) { Toast.makeText(requireContext(), R.string.dns_invalid, Toast.LENGTH_LONG).show(); return; }
                    JSONArray arr = getCustomDns();
                    for (int i=0;i<arr.length();i++) try { if (address.equals(arr.getJSONObject(i).optString("ip"))) return; } catch(Exception ignored){}
                    JSONObject o = new JSONObject();
                    try { o.put("name", name.getText().toString().trim().isEmpty() ? "DNS سفارشی" : name.getText().toString().trim()); o.put("ip", address); arr.put(o); } catch(Exception ignored){}
                    getPreferenceScreen().getSharedPreferences().edit().putString(CUSTOM_DNS_LIST, arr.toString()).apply();
                    Toast.makeText(requireContext(), R.string.dns_added, Toast.LENGTH_SHORT).show();
                    parent.dismiss(); showDnsManager();
                }).setNegativeButton(R.string.dialog_cancel, null).show();
    }

    private void removeCustomDns(String ip) {
        JSONArray old = getCustomDns(), out = new JSONArray();
        for (int i=0;i<old.length();i++) try { JSONObject o=old.getJSONObject(i); if(!ip.equals(o.optString("ip"))) out.put(o); } catch(Exception ignored){}
        getPreferenceScreen().getSharedPreferences().edit().putString(CUSTOM_DNS_LIST, out.toString()).apply();
        Toast.makeText(requireContext(), R.string.dns_removed, Toast.LENGTH_SHORT).show();
    }

    private void resetDnsSettings() {
        getPreferenceScreen().getSharedPreferences().edit()
                .remove(CUSTOM_DNS_LIST).putBoolean(AUTO_DNS, false).putBoolean(DNS_ONLY_BUNDLED, false)
                .putString(IPV4_DNS, "1.1.1.1").putString(IPV4_DNS2, "9.9.9.9").apply();
        handeleSummary(getPreferenceScreen(), getPreferenceScreen().getSharedPreferences());
        Toast.makeText(requireContext(), R.string.dns_reset, Toast.LENGTH_SHORT).show();
    }

    private void testDnsFromSettings() {
        final Preference test = findPreference("TEST_DNS");
        if (test != null) test.setSummary(R.string.dns_testing);
        new Thread(() -> {
            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            List<String> servers = DnsBenchmark.getCandidateServers(requireContext(), prefs);
            int timeout = 1200;
            try { timeout = Math.max(300, Math.min(5000, Integer.parseInt(prefs.getString(DNS_TEST_TIMEOUT, "1200")))); } catch (Exception ignored) {}
            java.util.List<DnsBenchmark.Result> results = DnsBenchmark.test(servers, timeout);
            requireActivity().runOnUiThread(() -> {
                if (test == null) return;
                if (results.isEmpty()) { test.setSummary(R.string.dns_no_result); return; }
                DnsBenchmark.Result best = results.get(0);
                test.setSummary(getString(R.string.dns_fastest, best.server, best.latencyMs));
                Toast.makeText(requireContext(), getString(R.string.dns_fastest, best.server, best.latencyMs), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void loadDnsListFromUrl() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        String url = prefs.getString(DNS_LIST_URL, "").trim();
        if (!isUrl(url)) { Toast.makeText(requireContext(), R.string.url_error, Toast.LENGTH_LONG).show(); return; }
        new Thread(() -> {
            try {
                String result = HttpUtils.get(url);
                FileUtils.writeFile(requireContext().openFileOutput("dns_list_remote.txt", Context.MODE_PRIVATE), result);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.dns_list_loaded, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                LogUtils.e(TAG, "load DNS list", e);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.down_error, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadSettingsFromUrl() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        String url = prefs.getString(SETTINGS_URL, "").trim();
        if (!isUrl(url)) { Toast.makeText(requireContext(), R.string.url_error, Toast.LENGTH_LONG).show(); return; }
        new Thread(() -> {
            try {
                String result = HttpUtils.get(url);
                JSONObject root = new JSONObject(result);
                SharedPreferences.Editor editor = prefs.edit();
                java.util.Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); if ("format".equals(key)) continue;
                    Object value = root.get(key);
                    if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer)value);
                    else if (value instanceof Long) editor.putLong(key, (Long)value);
                    else if (value instanceof Number) editor.putString(key, String.valueOf(value));
                    else if (value instanceof String) editor.putString(key, (String)value);
                }
                editor.apply();
                requireActivity().runOnUiThread(() -> { Toast.makeText(requireContext(), R.string.remote_settings_loaded, Toast.LENGTH_LONG).show(); ThemeUtils.apply(requireContext()); requireActivity().recreate(); });
            } catch (Exception e) {
                LogUtils.e(TAG, "load remote settings", e);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.import_error, Toast.LENGTH_LONG).show());
            }
        }).start();
    }
'''
s=s[:start]+new+s[end:]
s=s.replace('DnsBenchmark.getCandidateServers(getPreferenceScreen().getSharedPreferences())','DnsBenchmark.getCandidateServers(requireContext(), getPreferenceScreen().getSharedPreferences())')
s=s.replace('intent.putExtra(Intent.EXTRA_TITLE, "dnseye-settings.json");','intent.putExtra(Intent.EXTRA_TITLE, "netbin-settings.json");')
# Theme listener
s=s.replace('''        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {''','''        if ("THEME".equals(key)) {
            ThemeUtils.apply(requireContext());
            requireActivity().recreate();
            return;
        }
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {''')
# remove static black text for dark theme
s=s.replace('tvText.setTextColor(Color.parseColor("#000000"));\n','')
p.write_text(s, encoding='utf-8')

# Preferences XML rewrite
pref=root/'app/src/main/res/xml/preferences.xml'
pref.write_text('''<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    <PreferenceCategory android:title="@string/pref_ps_security">
        <CheckBoxPreference android:key="ADBLOCK_ENABLED" android:defaultValue="true"
            android:title="@string/pref_adblock" android:summary="@string/pref_adblock_summary" />
        <EditTextPreference android:key="ADBLOCK_URL"
            android:defaultValue="https://raw.githubusercontent.com/AdAway/adaway.github.io/master/hosts.txt"
            android:singleLine="true" android:selectAllOnFocus="true"
            android:title="@string/pref_adblock_url" android:summary="@string/pref_adblock_url_summary" />
    </PreferenceCategory>
    <PreferenceCategory android:title="@string/pref_ps_set_hosts_url">
        <EditTextPreference android:key="HOSTS_URL"
            android:defaultValue="https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/hosts.txt"
            android:selectAllOnFocus="true" android:title="@string/title_pref_remote_hosts"
            android:summary="@string/pref_remote_hosts" />
        <CheckBoxPreference android:key="IS_NET" android:defaultValue="true"
            android:title="@string/pref_is_net" />
    </PreferenceCategory>
    <PreferenceCategory android:title="@string/pref_ps_set_cus_dns">
        <CheckBoxPreference android:key="IS_CUS_DNS" android:defaultValue="true"
            android:title="@string/pref_dns_title" />
        <EditTextPreference android:key="IPV4_DNS" android:defaultValue="1.1.1.1"
            android:singleLine="true" android:selectAllOnFocus="true" android:title="@string/pref_dns" />
        <EditTextPreference android:key="IPV4_DNS2" android:defaultValue="9.9.9.9"
            android:singleLine="true" android:selectAllOnFocus="true" android:title="@string/pref_dns2" />
        <Preference android:key="MANAGE_DNS" android:title="@string/pref_manage_dns"
            android:summary="@string/pref_manage_dns_summary" />
        <CheckBoxPreference android:key="AUTO_DNS" android:defaultValue="false"
            android:title="@string/pref_auto_dns" android:summary="@string/pref_auto_dns_summary" />
        <CheckBoxPreference android:key="DNS_ONLY_BUNDLED" android:defaultValue="false"
            android:title="@string/pref_dns_only_bundled" android:summary="@string/pref_dns_only_bundled_summary" />
        <EditTextPreference android:key="DNS_TEST_TIMEOUT" android:defaultValue="1200"
            android:singleLine="true" android:selectAllOnFocus="true" android:title="@string/pref_dns_timeout"
            android:summary="@string/pref_dns_timeout_summary" android:inputType="number" />
        <Preference android:key="TEST_DNS" android:title="@string/pref_test_dns"
            android:summary="@string/pref_test_dns_summary" />
        <EditTextPreference android:key="DNS_LIST_URL" android:defaultValue=""
            android:singleLine="true" android:selectAllOnFocus="true" android:title="@string/pref_dns_list_url"
            android:summary="@string/pref_dns_list_url_summary" />
        <Preference android:key="LOAD_DNS_URL" android:title="@string/pref_load_dns_url"
            android:summary="@string/pref_load_dns_url_summary" />
        <Preference android:key="RESET_DNS" android:title="@string/pref_reset_dns"
            android:summary="@string/pref_reset_dns_summary" />
    </PreferenceCategory>
    <PreferenceCategory android:title="@string/pref_ps_appearance">
        <ListPreference android:key="THEME" android:defaultValue="system" android:title="@string/pref_theme"
            android:entries="@array/theme_entries" android:entryValues="@array/theme_values" />
    </PreferenceCategory>
    <PreferenceCategory android:title="@string/pref_ps_backup">
        <EditTextPreference android:key="SETTINGS_URL"
            android:defaultValue="https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/netbin-settings.json"
            android:singleLine="true" android:selectAllOnFocus="true" android:title="@string/pref_settings_url"
            android:summary="@string/pref_settings_url_summary" />
        <Preference android:key="LOAD_SETTINGS_URL" android:title="@string/pref_load_settings_url"
            android:summary="@string/pref_load_settings_url_summary" />
        <Preference android:key="EXPORT_SETTINGS" android:title="@string/pref_export" />
        <Preference android:key="IMPORT_SETTINGS" android:title="@string/pref_import" />
    </PreferenceCategory>
    <PreferenceCategory android:title="@string/pref_ps_support">
        <Preference android:key="SUPPORT_GITHUB" android:title="@string/pref_github" android:summary="@string/support_github" />
        <Preference android:key="SUPPORT_EMAIL" android:title="@string/pref_email" android:summary="@string/support_email" />
        <Preference android:key="SUPPORT_COFFEE" android:title="@string/pref_coffee" android:summary="@string/support_coffee" />
        <Preference android:key="SUPPORT_REYMIT" android:title="@string/pref_reymit" android:summary="@string/support_reymit" />
    </PreferenceCategory>
</PreferenceScreen>
''', encoding='utf-8')

# Strings replace and add
sp=root/'app/src/main/res/values/strings.xml'
s=sp.read_text()
s=s.replace('<string name="app_name">DNS Eye</string>','<string name="app_name">NETBIN</string>')
s=s.replace('DNS Eye','NETBIN')
insert='''    <string name="pref_dns_only_bundled">فقط DNSهای فهرست NETBIN</string>\n    <string name="pref_dns_only_bundled_summary">در تست سرعت فقط DNSهای فایل داخلی/بارگذاری‌شده بررسی می‌شوند.</string>\n    <string name="pref_dns_timeout">مهلت تست DNS (میلی‌ثانیه)</string>\n    <string name="pref_dns_timeout_summary">مقدار پیشنهادی ۱۲۰۰ میلی‌ثانیه است.</string>\n    <string name="pref_dns_list_url">آدرس فهرست DNS آنلاین</string>\n    <string name="pref_dns_list_url_summary">URL فهرست DNS برای ذخیره و به‌روزرسانی آنلاین.</string>\n    <string name="pref_load_dns_url">بارگذاری فهرست DNS از لینک</string>\n    <string name="pref_load_dns_url_summary">فهرست DNS موجود در لینک را دریافت می‌کند.</string>\n    <string name="pref_reset_dns">بازنشانی تنظیمات DNS</string>\n    <string name="pref_reset_dns_summary">DNSهای سفارشی و حالت خودکار را به مقدار پیش‌فرض برمی‌گرداند.</string>\n    <string name="dns_reset">تنظیمات DNS بازنشانی شد.</string>\n    <string name="dns_list_loaded">فهرست DNS از لینک دریافت و ذخیره شد.</string>\n    <string name="pref_settings_url">آدرس تنظیمات آنلاین</string>\n    <string name="pref_settings_url_summary">لینک فایل JSON تنظیمات NETBIN.</string>\n    <string name="pref_load_settings_url">بارگذاری تنظیمات از لینک</string>\n    <string name="pref_load_settings_url_summary">تنظیمات JSON را از لینک دریافت و روی برنامه اعمال می‌کند.</string>\n    <string name="remote_settings_loaded">تنظیمات آنلاین بارگذاری شد.</string>\n'''
s=s.replace('    <string name="adblock_download">دریافت فهرست مسدودسازی</string>', insert+'    <string name="adblock_download">دریافت فهرست مسدودسازی</string>')
sp.write_text(s, encoding='utf-8')

# SettingsActivity: ensure mode is applied and activity gets day/night theme cleanly
p=root/'app/src/main/java/Ir/hamed/dnseye/SettingsActivity.java'
s=p.read_text().replace('''        ThemeUtils.apply(this);
        setTheme(R.style.AppPreferenceTheme);
        super.onCreate(savedInstanceState);''','''        ThemeUtils.apply(this);
        setTheme(R.style.AppPreferenceTheme);
        super.onCreate(savedInstanceState);''')
p.write_text(s, encoding='utf-8')

# Change progress dialog color to theme attribute by deleting static import if unused
p=root/'app/src/main/java/Ir/hamed/dnseye/SettingsFragment.java'
s=p.read_text().replace('import android.graphics.Color;\n','')
p.write_text(s, encoding='utf-8')

# Create remote settings template and update docs/workflow names
settings={
  'format':2,
  'ADBLOCK_ENABLED':True,
  'ADBLOCK_URL':'https://raw.githubusercontent.com/AdAway/adaway.github.io/master/hosts.txt',
  'HOSTS_URL':'https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/hosts.txt',
  'IS_NET':True,
  'IS_CUS_DNS':True,
  'IPV4_DNS':'1.1.1.1','IPV4_DNS2':'9.9.9.9',
  'AUTO_DNS':False,'DNS_ONLY_BUNDLED':False,'DNS_TEST_TIMEOUT':'1200',
  'DNS_LIST_URL':'https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/app/src/main/res/raw/dns_list.txt',
  'THEME':'system'
}
(root/'netbin-settings.json').write_text(json.dumps(settings,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Rename visible project docs only
for fn in ['README.md','CHANGELOG.md','privacy.md','hosts.txt','.github/workflows/build.yml']:
    p=root/fn
    if p.exists():
        t=p.read_text(encoding='utf-8')
        t=t.replace('DNS Eye','NETBIN').replace('DNSEye','NETBIN')
        p.write_text(t,encoding='utf-8')
