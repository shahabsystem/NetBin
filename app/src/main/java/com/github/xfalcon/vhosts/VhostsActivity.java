package com.github.xfalcon.vhosts;

import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.github.xfalcon.vhosts.util.LogUtils;import com.github.xfalcon.vhosts.vservice.VhostsService;

public class VhostsActivity extends AppCompatActivity {
 private boolean waitingForVPNStart; private TextView status,source;
 private final BroadcastReceiver vpnStateReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(VhostsService.BROADCAST_VPN_STATE.equals(i.getAction())){boolean r=i.getBooleanExtra("running",false);waitingForVPNStart=false;updateUi(r);}}};
 @Override protected void onCreate(Bundle b){AppTheme.apply(this);super.onCreate(b);setContentView(R.layout.activity_vhosts);LogUtils.context=getApplicationContext();
  androidx.appcompat.widget.SwitchCompat vpn=findViewById(R.id.button_start_vpn); Button select=findViewById(R.id.button_select_hosts);
  Button settings=findViewById(R.id.button_settings), support=findViewById(R.id.button_support), boot=findViewById(R.id.button_boot); status=findViewById(R.id.text_status);source=findViewById(R.id.text_source);
  vpn.setOnCheckedChangeListener((v,checked)->{if(checked){if(checkHostUri()<0)showDialog();else startVPN();}else shutdownVPN();});
  select.setOnClickListener(v->selectFile());settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));support.setOnClickListener(v->startActivity(new Intent(this,DonationActivity.class)));
  boot.setOnClickListener(v->{boolean x=!BootReceiver.getEnabled(this);BootReceiver.setEnabled(this,x);boot.setText(x?R.string.boot_on:R.string.boot_off);});
  if(BootReceiver.getEnabled(this))boot.setText(R.string.boot_on); updateUi(VhostsService.isRunning());
  LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,new IntentFilter(VhostsService.BROADCAST_VPN_STATE));
 }
 private void updateUi(boolean running){if(status==null)return;status.setText(running?R.string.status_running:R.string.status_stopped);status.setCompoundDrawablesWithIntrinsicBounds(running?android.R.drawable.presence_online:android.R.drawable.presence_offline,0,0,0);SharedPreferences p=androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);boolean ad=p.getBoolean(SettingsFragment.ADBLOCK_ENABLED,true);source.setText(ad?R.string.source_adblock:R.string.source_hosts_only);}
 private int checkHostUri(){SharedPreferences p=androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);if(p.getBoolean(SettingsFragment.ADBLOCK_ENABLED,true))return 2;if(p.getBoolean(SettingsFragment.IS_NET,false)){try{openFileInput(SettingsFragment.NET_HOST_FILE).close();return 2;}catch(Exception e){return -2;}}String u=p.getString(SettingsFragment.HOSTS_URI,null);if(u==null)return -1;try{getContentResolver().openInputStream(Uri.parse(u)).close();return 1;}catch(Exception e){return -1;}}
 private void selectFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/*");i.addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,SettingsFragment.SELECT_FILE_CODE);}catch(Exception e){Toast.makeText(this,R.string.file_select_error,Toast.LENGTH_LONG).show();}}
 private void setUri(Intent i){Uri u=i.getData();if(u==null)return;try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).edit().putString(SettingsFragment.HOSTS_URI,u.toString()).putBoolean(SettingsFragment.IS_NET,false).apply();Toast.makeText(this,R.string.hosts_selected,Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,R.string.permission_error,Toast.LENGTH_LONG).show();}}
 private void startVPN(){new Thread(()->{try{if(AdBlockManager.isEnabled(this)){try{java.io.File cache=new java.io.File(getFilesDir(),AdBlockManager.CACHE_FILE);if(!cache.exists())AdBlockManager.update(this);AdBlockManager.prepareEffectiveHosts(this);}catch(Exception ignored){}}runOnUiThread(()->requestVpnPermission());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,R.string.start_error,Toast.LENGTH_LONG).show());}}).start();}
 private void requestVpnPermission(){waitingForVPNStart=false;Intent i=VhostsService.prepare(this);if(i!=null)startActivityForResult(i,SettingsFragment.VPN_REQUEST_CODE);else onActivityResult(SettingsFragment.VPN_REQUEST_CODE,RESULT_OK,null);}
 private void shutdownVPN(){if(VhostsService.isRunning())startService(new Intent(this,VhostsService.class).setAction(VhostsService.ACTION_DISCONNECT));updateUi(false);}
 @Override protected void onActivityResult(int r,int result,Intent d){super.onActivityResult(r,result,d);if(r==SettingsFragment.VPN_REQUEST_CODE&&result==RESULT_OK){waitingForVPNStart=true;startService(new Intent(this,VhostsService.class).setAction(VhostsService.ACTION_CONNECT));updateUi(true);}else if(r==SettingsFragment.SELECT_FILE_CODE&&result==RESULT_OK)setUri(d);}
 private void showDialog(){new AlertDialog.Builder(this).setTitle(R.string.dialog_title).setMessage(R.string.dialog_message).setPositiveButton(R.string.dialog_confirm,(d,w)->selectFile()).setNegativeButton(R.string.dialog_cancel,null).show();}
 @Override protected void onDestroy(){LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnStateReceiver);super.onDestroy();}
}
