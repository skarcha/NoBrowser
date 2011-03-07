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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Main extends Activity {

	private final String UserAgent = null;
	private boolean prefShowRedirect;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPrefs();
		Intent i = getIntent();
		String dataString = i.getDataString();

		if (isShortener(dataString)) {
			new getFinalUrlTask().execute(dataString);
		} else {
			processfinalUrl(dataString);
		}
	}

	private void finishActivity() {
		finish();
	}

	private void processfinalUrl (String url) {
		if (url != null) {
			if (url.startsWith("http://www.twitlonger.com/") ||
				url.startsWith("http://tl.gd/"))
			{
				new processTwitlongerTask().execute(url);
			}

			else if (url.contains("://market.android.com/")) {
				processMarket(url);
			}

			else {
				processDefault(url);
			}
		} else {
			finishActivity();
		}
	}

	private class processTwitlongerTask extends AsyncTask<String, Void, String> {
		private final ProgressDialog dialog = new ProgressDialog(Main.this);

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getString(R.string.please_wait));
			this.dialog.show();
		}

		@Override
		protected String doInBackground(final String... args) {
			final String tmpUrl = "http://api.embed.ly/1/oembed?url=" + args[0] + "&format=json";
			String ContentText = null;

			try {
				ContentText = Main.this.getTwitlongerContent(tmpUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ContentText;
		}

		@Override
		protected void onPostExecute(final String ContentText) {
			String tweetText = null;

			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}

			if (ContentText == null) {
				tweetText = getString(R.string.fetching_error);
			} else {
				tweetText = ContentText;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
			builder.setTitle("Twitlonger")
				   .setMessage(tweetText)
			       .setCancelable(false)
			       .setNeutralButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                Main.this.finishActivity();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	private String getTwitlongerContent (String url) throws IOException, IOException {
		String responseBody = null;

		HttpClient httpclient = new Network().getTolerantClient();
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

	private void processMarket (String url) {
		String MarketUrl = null;

		try {
			URI oldUri = new URI(url);
			String path = oldUri.getPath();
			String query = oldUri.getQuery();
			MarketUrl = "market:/" + path + "?" + query;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (MarketUrl != null) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(android.net.Uri.parse(MarketUrl));
			startActivity(intent);
		}

		finishActivity();
	}

	private void processDefault (String url) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(android.net.Uri.parse(url));
		startActivity(intent);

		finishActivity();
	}

	private boolean isShortener (String url) {

		if (url.startsWith("http://bit.ly/") ||
			url.startsWith("http://goo.gl/") ||
			url.startsWith("http://is.gd/") ||
			url.startsWith("http://j.mp/") ||
			url.startsWith("http://kcy.me/") ||
			url.startsWith("http://t.co/") ||
			url.startsWith("http://tinyurl.com/") ||
			url.startsWith("http://urlcorta.es/") ||
			url.startsWith("http://youtu.be/"))
		{
			return true;
		}

		return false;
	}

	private class getFinalUrlTask extends AsyncTask<String, Void, String> {
		private final ProgressDialog dialog = new ProgressDialog(Main.this);

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getString(R.string.please_wait));
			this.dialog.show();
		}

		@Override
		protected String doInBackground(final String... args) {
			String finalUrl = null;
			try {
				finalUrl = Main.this.getFinalURL(args[0]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return finalUrl;
		}

		@Override
		protected void onPostExecute(final String url) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (prefShowRedirect) {
				toast(getString(R.string.redirecting_to) + " " + url);
			}
			Main.this.processfinalUrl(url);
		}
	}

	private String getFinalURL (String url) throws IOException, IOException {
		HttpHost target;
		HttpRequest request;

		// Youtu.be special case
		if (url.startsWith("http://youtu.be/")) {
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

		//HttpClient httpclient = new DefaultHttpClient();
		HttpClient httpclient = new Network().getTolerantClient();
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
