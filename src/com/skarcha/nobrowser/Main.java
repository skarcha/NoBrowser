package com.skarcha.nobrowser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Main extends Activity {

	private boolean prefShowRedirect;
	private ProgressDialog progress;
	private String ContentText = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPrefs();
		Intent i = getIntent();
		String dataString = i.getDataString();
		String finalUrl = null;
		boolean finishIntent = true;

		if (isShortener(dataString)) {
			try {
				finalUrl = getFinalURL (dataString);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (prefShowRedirect) {
				toast(getString(R.string.redirecting_to) + " " + finalUrl);
			}
		} else {
			finalUrl = dataString;
		}

		if (finalUrl != null) {
			if (finalUrl.startsWith("http://www.twitlonger") ||
				finalUrl.startsWith("http://tl.gd"))
			{
				finishIntent = processTwitlonger(finalUrl);
			}

			else {
				finishIntent = processDefault(finalUrl);
			}
		}

		if (finishIntent) {
			finish();
		}
	}

	private boolean processTwitlonger(String url) {
		final String tmpUrl = "http://api.embed.ly/1/oembed?url=" + url + "&format=json";

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

		return false;
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			progress.dismiss();

			if (ContentText == null) {
				ContentText = (String) getText(R.string.fetching_error);
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
			builder.setTitle("Twitlonger")
				   .setMessage(ContentText)
			       .setCancelable(false)
			       .setNeutralButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                Main.this.finish();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
	};

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

	private boolean processDefault (String url) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(android.net.Uri.parse(url));
		startActivity(intent);

		return true;
	}

	private boolean isShortener (String url) {

		if (url.startsWith("http://bit.ly") ||
			url.startsWith("http://goo.gl") ||
			url.startsWith("http://is.gd") ||
			url.startsWith("http://kcy.me") ||
			url.startsWith("http://t.co") ||
			url.startsWith("http://tinyurl.com") ||
			url.startsWith("http://urlcorta.es") ||
			url.startsWith("http://youtu.be"))
		{
			return true;
		}

		return false;
	}

	private String getFinalURL (String url) throws IOException, IOException {
		HttpHost target;
		HttpRequest request;

		// Youtu.be special case
		if (url.startsWith("http://youtu.be")) {
			String YoutubeUrl = null;
			try {
				URI oldUri = new URI(url);
				YoutubeUrl = oldUri.getScheme() + "://www.youtube.com/watch?v=" + oldUri.getPath().substring(1);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return YoutubeUrl;
		}

		HttpClient httpclient = new DefaultHttpClient();
		try {
			HttpHead httphead = new HttpHead(url);
			HttpContext localContext = new BasicHttpContext();
			HttpResponse response = httpclient.execute(httphead, localContext);
			target  = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			request = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}

		if (target == null) {
			return null;
		}

		return target.toString() + request.getRequestLine().getUri();
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
