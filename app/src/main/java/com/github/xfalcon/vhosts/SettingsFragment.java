package com.github.xfalcon.vhosts;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import androidx.preference.*;
import com.github.xfalcon.vhosts.util.FileUtils;
import com.github.xfalcon.vhosts.util.HttpUtils;
import com.github.xfalcon.vhosts.vservice.DnsChange;
import org.json.JSONObject;
import org.xbill.DNS.Address;

import java.io.*;
import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int VPN_REQUEST_CODE=0x0F, SELECT_FILE_CODE=0x05, EXPORT_CODE=0x06, IMPORT_CODE=0x07;
    public static final String PREFS_NAME=SettingsFragment.class.getName();
    public static final String IS_NET="IS_NET", HOSTS_URL="HOSTS_URL", HOSTS_URI="HOST_URI", NET_HOST_FILE="net_hosts";
    public static final String IPV4_DNS="IPV4_DNS", IPV4_DNS2="IPV4_DNS2", IS_CUS_DNS="IS_CUS_DNS";
    public static final String ADBLOCK_ENABLED="ADBLOCK_ENABLED", ADBLOCK_URL="ADBLOCK_URL";

    @Override public void onCreatePreferences(Bundle state,String rootKey){
        setPreferencesFromResource(R.xml.preferences,rootKey);
        SharedPreferences p=getPreferenceManager().getSharedPreferences();
        Preference dns=findPreference(IPV4_DNS), dns2=findPreference(IPV4_DNS2), url=findPreference(HOSTS_URL);
        ListPreference preset=findPreference("DNS_PRESET");
        if(preset!=null) preset.setOnPreferenceChangeListener((pr,v)->{String x=String.valueOf(v);SharedPreferences.Editor e=p.edit();if("cloudflare".equals(x)){e.putString(IPV4_DNS,"1.1.1.1").putString(IPV4_DNS2,"1.0.0.1");}else if("google".equals(x)){e.putString(IPV4_DNS,"8.8.8.8").putString(IPV4_DNS2,"8.8.4.4");}else if("quad9".equals(x)){e.putString(IPV4_DNS,"9.9.9.9").putString(IPV4_DNS2,"149.112.112.112");}else if("adguard".equals(x)){e.putString(IPV4_DNS,"94.140.14.14").putString(IPV4_DNS2,"94.140.15.15");}e.apply();return true;});
        Preference export=findPreference("EXPORT_SETTINGS"), imp=findPreference("IMPORT_SETTINGS"), update=findPreference("UPDATE_ADBLOCK");
        if(dns!=null) dns.setOnPreferenceChangeListener((pr,v)->validIp((String)v,pr.getContext()));
        if(dns2!=null) dns2.setOnPreferenceChangeListener((pr,v)->validIp((String)v,pr.getContext()));
        if(url!=null) url.setOnPreferenceChangeListener((pr,v)->validUrl((String)v));
        if(export!=null) export.setOnPreferenceClickListener(pr->{exportSettings();return true;});
        if(imp!=null) imp.setOnPreferenceClickListener(pr->{importSettings();return true;});
        if(update!=null) update.setOnPreferenceClickListener(pr->{downloadAdBlock();return true;});
        updateSummaries(getPreferenceScreen(),p);
    }
    private boolean validIp(String s,Context c){try{Address.getByAddress(s);return true;}catch(Exception e){Toast.makeText(c,R.string.dns4_error,Toast.LENGTH_LONG).show();return false;}}
    private boolean validUrl(String s){if(s!=null && (s.startsWith("http://")||s.startsWith("https://")))return true;Toast.makeText(requireContext(),R.string.url_error,Toast.LENGTH_LONG).show();return false;}
    private void downloadAdBlock(){
        Toast.makeText(requireContext(),R.string.adblock_updating,Toast.LENGTH_SHORT).show();
        new Thread(()->{try{int n=AdBlockManager.update(requireContext());requireActivity().runOnUiThread(()->Toast.makeText(requireContext(),getString(R.string.adblock_updated,n),Toast.LENGTH_LONG).show());}catch(Exception e){requireActivity().runOnUiThread(()->Toast.makeText(requireContext(),R.string.adblock_update_error,Toast.LENGTH_LONG).show());}}).start();
    }
    private void updateSummaries(PreferenceGroup g,SharedPreferences p){for(int i=0;i<g.getPreferenceCount();i++){Preference x=g.getPreference(i);if(x instanceof PreferenceGroup)updateSummaries((PreferenceGroup)x,p);else if(x.getKey()!=null && !(x instanceof CheckBoxPreference) && !(x instanceof ListPreference)) x.setSummary(p.getString(x.getKey(),x.getSummary()==null?"":x.getSummary().toString()));}}
    private void exportSettings(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"vhosts-settings.json");startActivityForResult(i,EXPORT_CODE);
    }
    private void importSettings(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT_CODE);}
    @Override public void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=Activity.RESULT_OK||data==null)return;try{if(req==EXPORT_CODE){JSONObject o=new JSONObject();for(Map.Entry<String,?> e:getPreferenceManager().getSharedPreferences().getAll().entrySet()){Object v=e.getValue();if(v instanceof String||v instanceof Boolean||v instanceof Integer||v instanceof Long||v instanceof Float)o.put(e.getKey(),v);}OutputStream out=requireContext().getContentResolver().openOutputStream(data.getData());out.write(o.toString(2).getBytes("UTF-8"));out.close();Toast.makeText(requireContext(),R.string.settings_exported,Toast.LENGTH_LONG).show();}else if(req==IMPORT_CODE){InputStream in=requireContext().getContentResolver().openInputStream(data.getData());BufferedReader r=new BufferedReader(new InputStreamReader(in));StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);r.close();JSONObject o=new JSONObject(b.toString());SharedPreferences.Editor e=getPreferenceManager().getSharedPreferences().edit();java.util.Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.get(k);if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Double)e.putFloat(k,((Double)v).floatValue());else e.putString(k,String.valueOf(v));}e.apply();Toast.makeText(requireContext(),R.string.settings_imported,Toast.LENGTH_LONG).show();requireActivity().recreate();}}catch(Exception e){Toast.makeText(requireContext(),R.string.settings_file_error,Toast.LENGTH_LONG).show();}}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){if(AppTheme.KEY_THEME.equals(key))AppTheme.apply(requireContext());}
    @Override public void onResume(){super.onResume();getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);}
    @Override public void onPause(){getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);super.onPause();}
}
