package com.ne0fhyklabs.freeflight.activities;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import com.ne0fhyklabs.freeflight.R;
import android.support.v4.app.Fragment;

public class SettingsActivity extends FragmentActivity{
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		if(savedInstanceState == null){
			Fragment settings = new SettingsDialog(this);
			getSupportFragmentManager().beginTransaction().add(R.id.settings_screen, settings).commit();
		}
	}
}
