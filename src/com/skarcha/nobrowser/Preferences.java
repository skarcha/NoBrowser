package com.skarcha.nobrowser;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
//            PreferenceManager.setDefaultValues(Preferences.this, R.xml.preferences, false);
//            PreferenceManager.getDefaultSharedPreferences(Preferences.this);
//            SharedPreferences settings = getSharedPreferences("settings", 0);
    }
}
