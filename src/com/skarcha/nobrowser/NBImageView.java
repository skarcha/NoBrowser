package com.skarcha.nobrowser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;

public class NBImageView extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.nbimageview);

		Intent i = getIntent();
		String dataString = i.getDataString();

		if (dataString.startsWith("http://yfrog.com/")) {
			dataString += ":android";

		} else if (dataString.startsWith("http://twitpic.com/")) {
			dataString = "http://twitpic.com/show/full/" + URI.create(dataString).getPath();

		} else if (dataString.startsWith("http://picplz.com/")) {
			dataString = get_picplz_image_url(URI.create(dataString).getPath().substring(1));

		} else {
			finish();
		}

		if (dataString == null) {
			finish();
		} else {
			new getImageTask().execute(dataString);
		}
    }

	private void showImage (Drawable drawable) {
		ImageView imgView =(ImageView)findViewById(R.id.imageView1);
		imgView.setImageDrawable(drawable);
	}

	private String get_picplz_image_url(String id) {
		String url = "http://api.picplz.com/api/v2/pic.json?shorturl_id=" + id + "&pic_formats=640r";
		String responseBody = null;

		HttpClient httpclient = new Network().getTolerantClient();

		try {
			HttpGet httpget = new HttpGet(url);

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}

		if (responseBody == null) {
			return null;
		}

		String image_url = null;
		try {
			JSONObject jsRespuesta = new JSONObject(responseBody);
			String result = jsRespuesta.getString("result");
			if (result.equals("ok")) {
				image_url = jsRespuesta.getJSONObject("value").getJSONArray("pics").getJSONObject(0).getJSONObject("pic_files").getJSONObject("640r").getString("img_url");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return image_url;
	}

	private class getImageTask extends AsyncTask<String, Void, Drawable> {
		private final ProgressDialog dialog = new ProgressDialog(NBImageView.this);

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getString(R.string.please_wait));
			this.dialog.show();
		}

		@Override
		protected Drawable doInBackground(final String... args) {
			Drawable drawable = null;
			drawable = NBImageView.this.LoadImageFromWebOperations(args[0]);
			return drawable;
		}

		@Override
		protected void onPostExecute(final Drawable drawable) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}

			NBImageView.this.showImage(drawable);
		}
	}

    private Drawable LoadImageFromWebOperations(String url) {
		try {
			InputStream is = (InputStream) new URL(url).getContent();
			Drawable d = Drawable.createFromStream(is, "src name");
			return d;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
