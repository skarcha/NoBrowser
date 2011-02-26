package com.skarcha.nobrowser;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

public class Network {
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
		//String UserAgent = "Mozilla/5.0 (compatible; NoBrowser for Android/" + getVersionName() + ")";
		String UserAgent = "Mozilla/5.0 (compatible; NoBrowser for Android/1.2.0)";

		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, UserAgent);

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

/*
	private String getVersionName() {
		PackageInfo pinfo;

		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return null;
		}

		return pinfo.versionName;
	}
*/
}
