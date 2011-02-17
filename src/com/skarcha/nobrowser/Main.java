package com.skarcha.nobrowser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
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
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Main extends Activity {

	private String UserAgent = null;
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
		boolean finishActivity = true;

		UserAgent = "Mozilla/5.0 (compatible; NoBrowser for Android/" + getVersionName() + ")";

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
			if (finalUrl.startsWith("http://www.twitlonger.com/") ||
				finalUrl.startsWith("http://tl.gd/"))
			{
				finishActivity = processTwitlonger(finalUrl);
			}

			else if (finalUrl.contains("://market.android.com/")) {
				finishActivity = processMarket(finalUrl);
			}

			else {
				finishActivity = processDefault(finalUrl);
			}
		}

		if (finishActivity) {
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
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, UserAgent);
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

	private boolean processMarket (String url) {
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

		return true;
	}

	private boolean processDefault (String url) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(android.net.Uri.parse(url));
		startActivity(intent);

		return true;
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
		HttpClient httpclient = getTolerantClient();
		try {
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, UserAgent);
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

	private String getVersionName() {
		PackageInfo pinfo;

		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return null;
		}

		return pinfo.versionName;
	}

	private void toast(String texto) {
		Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
	}

/*
 * Black box for me at the moment.
 * Extracted from: http://stackoverflow.com/questions/3135679/android-httpclient-hostname-in-certificate-didnt-match-example-com-exa/3136980#3136980
 *
 * This is the ugly hack to this message:
 * javax.net.ssl.SSLException: hostname in certificate didn't match <market.android.com> != <*.google.com>
 *
 * Ugly hack start here...
 */

	class MyVerifier extends AbstractVerifier {

		private final X509HostnameVerifier delegate;

		public MyVerifier(final X509HostnameVerifier delegate) {
			this.delegate = delegate;
		}

		@Override
		public void verify(String host, String[] cns, String[] subjectAlts)
				throws SSLException {
/*
			boolean ok = false;
			try {
				delegate.verify(host, cns, subjectAlts);
			} catch (SSLException e) {
				for (String cn : cns) {
					if (cn.startsWith("*.")) {
						try {
							delegate.verify(host, new String[] {
								cn.substring(2) }, subjectAlts);
							ok = true;
						} catch (Exception e1) { }
					}
				}
				if(!ok) throw e;
			}
*/
		}
	}

	public DefaultHttpClient getTolerantClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory) client
				.getConnectionManager().getSchemeRegistry().getScheme("https")
				.getSocketFactory();
		final X509HostnameVerifier delegate = sslSocketFactory
				.getHostnameVerifier();
		if (!(delegate instanceof MyVerifier)) {
			sslSocketFactory.setHostnameVerifier(new MyVerifier(delegate));
		}
		return client;
	}

/*
 * Ugly hack ends here...
 */
}
