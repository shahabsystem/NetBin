/*
 **Copyright (C) 2017  xfalcon
 **
 **This program is free software: you can redistribute it and/or modify
 **it under the terms of the GNU General Public License as published by
 **the Free Software Foundation, either version 3 of the License, or
 **(at your option) any later version.
 **
 **This program is distributed in the hope that it will be useful,
 **but WITHOUT ANY WARRANTY; without even the implied warranty of
 **MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **GNU General Public License for more details.
 **
 **You should have received a copy of the GNU General Public License
 **along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package Ir.hamed.dnseye;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import android.widget.Toast;
import androidx.preference.*;
import Ir.hamed.dnseye.util.FileUtils;
import Ir.hamed.dnseye.util.HttpUtils;
import Ir.hamed.dnseye.util.LogUtils;
import Ir.hamed.dnseye.vservice.DnsChange;
import org.xbill.DNS.Address;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static String TAG = SettingsFragment.class.getName();

    public static final int VPN_REQUEST_CODE = 0x0F;
    public static final int SELECT_FILE_CODE = 0x05;
    public static final String PREFS_NAME = SettingsFragment.class.getName();
    public static final String IS_NET = "IS_NET";
    public static final String HOSTS_URL = "HOSTS_URL";
    public static final String HOSTS_URI = "HOST_URI";
    public static final String NET_HOST_FILE = "net_hosts";
    public static final String IPV4_DNS = "IPV4_DNS";
    public static final String IS_CUS_DNS = "IS_CUS_DNS";
    public static final String IPV4_DNS2 = "IPV4_DNS2";
    public static final String ADBLOCK_ENABLED = "ADBLOCK_ENABLED";
    public static final String ADBLOCK_URL = "ADBLOCK_URL";
    public static final String CUSTOM_DNS_LIST = "CUSTOM_DNS_LIST";
    public static final String AUTO_DNS = "AUTO_DNS";
    public static final String DNS_TEST_TIMEOUT = "DNS_TEST_TIMEOUT";
    public static final String DNS_ONLY_BUNDLED = "DNS_ONLY_BUNDLED";
    public static final String DNS_LIST_URL = "DNS_LIST_URL";
    public static final String SETTINGS_URL = "SETTINGS_URL";

    private Handler handler = null;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        final SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        handeleSummary(prefScreen, sharedPreferences);
        Preference urlCustomPref = findPreference(HOSTS_URL);
        Preference dnsCustomPref = findPreference(IPV4_DNS);

        dnsCustomPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String ipv4_dns = (String)newValue;
                try {
                    Address.getByAddress(ipv4_dns);
                    return true;
                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    Toast.makeText(preference.getContext(), getString(R.string.dns4_error), Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });


//        dnsCustomPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//
//            public boolean onPreferenceClick(Preference preference) {
//                String ipv4_dns = sharedPreferences.getString(IPV4_DNS, "");
//                try {
//                    Address.getByAddress(ipv4_dns);
//                    return true;
//                } catch (Exception e) {
//                    LogUtils.e(TAG, e.getMessage(), e);
//                    Toast.makeText(preference.getContext(), getString(R.string.url_error), Toast.LENGTH_LONG).show();
//                }
//                return false;
//            }
//        });

        urlCustomPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String url = (String)newValue;
                if (isUrl(url)) {
                    setProgressDialog(preference.getContext(), url, NET_HOST_FILE);
                    return true;
                } else {
                    Toast.makeText(preference.getContext(), getString(R.string.url_error), Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        Preference dns2 = findPreference(IPV4_DNS2);
        dns2.setOnPreferenceChangeListener((preference, newValue) -> {
            try { Address.getByAddress((String)newValue); return true; }
            catch (Exception e) { Toast.makeText(preference.getContext(), R.string.dns4_error, Toast.LENGTH_LONG).show(); return false; }
        });

        Preference dnsListUrl = findPreference(DNS_LIST_URL);
        dnsListUrl.setOnPreferenceChangeListener((preference, newValue) -> {
            String url = String.valueOf(newValue).trim();
            if (!url.isEmpty() && !isUrl(url)) { Toast.makeText(preference.getContext(), R.string.url_error, Toast.LENGTH_LONG).show(); return false; }
            return true;
        });
        Preference settingsUrl = findPreference(SETTINGS_URL);
        settingsUrl.setOnPreferenceChangeListener((preference, newValue) -> {
            String url = String.valueOf(newValue).trim();
            if (!isUrl(url)) { Toast.makeText(preference.getContext(), R.string.url_error, Toast.LENGTH_LONG).show(); return false; }
            return true;
        });

        Preference adblockUrl = findPreference(ADBLOCK_URL);
        adblockUrl.setOnPreferenceChangeListener((preference, newValue) -> {
            String url=(String)newValue;
            if (!isUrl(url)) { Toast.makeText(preference.getContext(), R.string.url_error, Toast.LENGTH_LONG).show(); return false; }
            setProgressDialog(preference.getContext(), url, "adblock_hosts");
            return true;
        });

        Preference manageDns = findPreference("MANAGE_DNS");
        manageDns.setOnPreferenceClickListener(p -> { showDnsManager(); return true; });
        findPreference("TEST_DNS").setOnPreferenceClickListener(p -> { testDnsFromSettings(); return true; });
        findPreference("LOAD_DNS_URL").setOnPreferenceClickListener(p -> { loadDnsListFromUrl(); return true; });
        findPreference("RESET_DNS").setOnPreferenceClickListener(p -> { resetDnsSettings(); return true; });
        findPreference("LOAD_SETTINGS_URL").setOnPreferenceClickListener(p -> { loadSettingsFromUrl(); return true; });
        findPreference("EXPORT_SETTINGS").setOnPreferenceClickListener(p -> { exportSettings(); return true; });
        findPreference("IMPORT_SETTINGS").setOnPreferenceClickListener(p -> { importSettings(); return true; });
        findPreference("SUPPORT_GITHUB").setOnPreferenceClickListener(p -> { openSupportUrl("https://github.com/shahabsystem"); return true; });
        findPreference("SUPPORT_COFFEE").setOnPreferenceClickListener(p -> { openSupportUrl("https://coffeebede.com/shahabsystem"); return true; });
        findPreference("SUPPORT_REYMIT").setOnPreferenceClickListener(p -> { openSupportUrl("https://reymit.ir/shahabsystem"); return true; });
        findPreference("SUPPORT_EMAIL").setOnPreferenceClickListener(p -> { openSupportUrl("mailto:hamedmohammadinikche@gmail.com"); return true; });

//        urlCustomPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//
//            public boolean onPreferenceClick(Preference preference) {
//                String url = sharedPreferences.getString(HOSTS_URL, "");
//                if (isUrl(url)) {
//                    setProgressDialog(preference.getContext(), url);
//                    return true;
//                } else {
//                    Toast.makeText(preference.getContext(), getString(R.string.url_error), Toast.LENGTH_LONG).show();
//                    return false;
//                }
//
//            }
//        });
    }


    private JSONArray getCustomDns() {
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
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_manage_dns)
                .setView(scroll)
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
    private void openSupportUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(requireContext(), url, Toast.LENGTH_LONG).show(); }
    }

    private void exportSettings() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "netbin-settings.json");
        startActivityForResult(intent, 1001);
    }

    private void importSettings() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1002);
    }

    private JSONObject settingsToJson(SharedPreferences prefs) throws Exception {
        JSONObject root = new JSONObject();
        for (String key : prefs.getAll().keySet()) {
            Object value = prefs.getAll().get(key);
            if (value instanceof Boolean || value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Float)
                root.put(key, value);
        }
        root.put("format", 1);
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == 1001) {
                OutputStream out = requireContext().getContentResolver().openOutputStream(data.getData());
                if (out == null) throw new Exception("output");
                out.write(settingsToJson(getPreferenceScreen().getSharedPreferences()).toString(2).getBytes(StandardCharsets.UTF_8));
                out.close();
                Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_LONG).show();
            } else if (requestCode == 1002) {
                InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                if (in == null) throw new Exception("input");
                byte[] bytes = new byte[8192]; int len, total=0; java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                while ((len=in.read(bytes))!=-1 && total < 1024*1024) { buffer.write(bytes,0,len); total+=len; }
                in.close();
                JSONObject root = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
                SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                java.util.Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String key=keys.next(); if ("format".equals(key)) continue;
                    Object value=root.get(key);
                    if (value instanceof Boolean) editor.putBoolean(key,(Boolean)value);
                    else if (value instanceof Integer) editor.putInt(key,(Integer)value);
                    else if (value instanceof Long) editor.putLong(key,(Long)value);
                    else if (value instanceof Number) editor.putString(key,String.valueOf(value));
                    else if (value instanceof String) editor.putString(key,(String)value);
                }
                editor.apply();
                Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_LONG).show();
                requireActivity().recreate();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "settings import/export", e);
            Toast.makeText(requireContext(), R.string.import_error, Toast.LENGTH_LONG).show();
        }
    }

    public void setProgressDialog(final Context context, final String url, final String targetFile) {

        int llPadding = 30;
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(context);
        tvText.setText(getString(R.string.download_alert));
                tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(ll);

        final AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    String result = HttpUtils.get(url);
                    FileUtils.writeFile(context.openFileOutput(targetFile, Context.MODE_PRIVATE), result);
                    Toast.makeText(context, getString(R.string.down_success), Toast.LENGTH_LONG).show();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                        }
                    });
                    Looper.loop();
                } catch (Exception e) {
                    Toast.makeText(context, getString(R.string.down_error), Toast.LENGTH_LONG).show();
                    LogUtils.e(TAG, e.getMessage(), e);
                }

            }
        }).start();
        dialog.show();

    }

    private void handeleSummary(PreferenceGroup preferenceGroup, SharedPreferences sharedPreferences) {
        int count = preferenceGroup.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            Preference p = preferenceGroup.getPreference(i);
            if (p instanceof PreferenceCategory) {
                handeleSummary((PreferenceCategory) p, sharedPreferences);
            }
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(value);
        }
    }

    public boolean isUrl(String str) {
        String regex = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference preference = findPreference(key);
        if ("THEME".equals(key)) {
            ThemeUtils.apply(requireContext());
            requireActivity().recreate();
            return;
        }
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // create ContextThemeWrapper from the original Activity Context with the custom theme

        // clone the inflater using the ContextThemeWrapper
        inflater.getContext().setTheme(R.style.AppPreferenceSettingsFragmentTheme);

        // inflate the layout using the cloned inflater, not default inflater
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
