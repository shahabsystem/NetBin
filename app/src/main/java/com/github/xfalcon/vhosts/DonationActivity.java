package com.github.xfalcon.vhosts;

import android.content.*;import android.net.Uri;import android.os.Bundle;import android.text.method.LinkMovementMethod;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;

public class DonationActivity extends AppCompatActivity {
 @Override protected void onCreate(Bundle b){AppTheme.apply(this);super.onCreate(b);setContentView(R.layout.activity_donation);if(getSupportActionBar()!=null){getSupportActionBar().setDisplayHomeAsUpEnabled(true);getSupportActionBar().setTitle(R.string.support_title);}
  findViewById(R.id.support_github).setOnClickListener(v->open("https://github.com/shahabsystem"));
  findViewById(R.id.support_repo).setOnClickListener(v->open("https://github.com/shahabsystem/timegoo"));
  findViewById(R.id.support_email).setOnClickListener(v->open("https://github.com/shahabsystem"));
 }
 private void open(String u){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception ignored){}}
 @Override public boolean onSupportNavigateUp(){finish();return true;}
}
