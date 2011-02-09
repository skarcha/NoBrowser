package com.skarcha.nobrowser;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class ViewContent extends Activity {

	// Progress Dialog que usamos para entretener al usuario
	private ProgressDialog progress;
	private String tmpUrl = null;
	private String ContentText = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		tmpUrl = "http://api.embed.ly/1/oembed?url=" + i.getDataString() + "&format=json";

		if (tmpUrl != null) {
			this.progress = ProgressDialog.show(this, "", getText(R.string.please_wait), false);

			Thread th = new Thread() {
				@Override
				public void run() {
					try {
						ContentText = getContent(tmpUrl);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						handler.sendEmptyMessage(0);
					}
				}
			};
			th.start();
		}
	}

	private String getContent (String url) throws IOException, IOException {
		String responseBody = null;

		HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(url);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseBody = httpclient.execute(httpget, responseHandler);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        if (responseBody == null) {
        	return null;
        }

        String description = null;
		try {
			JSONObject jsRespuesta = new JSONObject(responseBody);
			description = jsRespuesta.getString("description");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return description;
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			progress.dismiss();

			if (ContentText == null) {
				ContentText = (String) getText(R.string.fetching_error);
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(ViewContent.this);
			builder.setMessage(ContentText)
			       .setCancelable(false)
			       .setNeutralButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                ViewContent.this.finish();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
	};

	private void toast(String texto) {
		Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
	}
}
