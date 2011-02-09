package com.skarcha.nobrowser;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Main extends Activity {

	private boolean prefShowRedirect;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String tmpUrl = null;

		getPrefs();
		Intent i = getIntent();

		try {
			URI oldUri = new URI(i.getDataString());
			tmpUrl = oldUri.getScheme() + "://www.youtube.com/watch?v=" + oldUri.getPath().substring(1);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (tmpUrl != null) {
			if (prefShowRedirect) {
				toast(getString(R.string.redirecting_to) + " " + tmpUrl);
			}

			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(android.net.Uri.parse(tmpUrl));
			startActivity(intent);
		}

		finish();
	}

	private void getPrefs() {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefShowRedirect = prefs.getBoolean("show_redirect", true);
	}

	private void toast(String texto) {
		Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
	}
}
