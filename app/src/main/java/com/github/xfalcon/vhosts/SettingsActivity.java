package com.github.xfalcon.vhosts;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
 @Override protected void onCreate(Bundle b){AppTheme.apply(this);super.onCreate(b);setContentView(R.layout.activity_settings);if(getSupportActionBar()!=null){getSupportActionBar().setDisplayHomeAsUpEnabled(true);getSupportActionBar().setTitle(R.string.settings_title);}}
 @Override public boolean onOptionsItemSelected(MenuItem i){if(i.getItemId()==android.R.id.home){finish();return true;}return super.onOptionsItemSelected(i);}
}
