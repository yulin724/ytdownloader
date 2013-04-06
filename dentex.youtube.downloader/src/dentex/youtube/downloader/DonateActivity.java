package dentex.youtube.downloader;

import java.util.Locale;

import org.apache.http.util.EncodingUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import dentex.youtube.downloader.utils.Utils;

public class DonateActivity extends Activity {
	
	public static final String DEBUG_TAG = "DonateActivity";
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_donate);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.donate, false);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new DonateFragment())
                .commit();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
	public static class DonateFragment extends PreferenceFragment /*implements OnSharedPreferenceChangeListener*/ {
    	
		private Preference pp;
		private WebView webview;
		
		@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            webview = new WebView(getActivity());
            webview.getSettings().setJavaScriptEnabled(true);
            getActivity().getWindow().requestFeature(Window.FEATURE_PROGRESS);

            addPreferencesFromResource(R.xml.donate);
            
            /*for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }*/
	        
	        pp = (Preference) findPreference("paypal");
	        pp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	        	/*
	        	 * uriBuilder code adapted from:
	        	 *   https://github.com/dentex/android-donations-lib/blob/master/
	        	 *   org_donations/src/org/donations/DonationsFragment.java
	        	 *   Licensed under the Apache License, Version 2.0 (the "License");
	        	 * by:
	        	 *   Dominik Schürmann <dominik@dominikschuermann.de>
	        	 *   
	        	 *   ----------------------------------------------------------------
	        	 *   
	        	 * webview code adapted from:
	        	 *   http://misha.beshkin.lv/android-add-paypal-donation-page-to-app/
	        	 * by:
	        	 *   Moishe Beshkin
	        	 */
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	Uri.Builder uriBuilder = new Uri.Builder();
	                //uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr");
	                uriBuilder.appendQueryParameter("cmd", "_donations");
	                uriBuilder.appendQueryParameter("business", getString(R.string.paypal_user));
	                uriBuilder.appendQueryParameter("lc", "US");
	                uriBuilder.appendQueryParameter("item_name", getString(R.string.paypal_item_name));
	                uriBuilder.appendQueryParameter("no_note", "1");
	                uriBuilder.appendQueryParameter("no_shipping", "1");
	                uriBuilder.appendQueryParameter("currency_code", getString(R.string.paypal_currency_code));
	                
	                Uri payPalUri = uriBuilder.build();
	                String postText = payPalUri.getQuery();
	                byte[] post = null;
	        		post = EncodingUtils.getBytes(postText, "BASE64");

	                final Activity activity = getActivity();
	                webview.setWebChromeClient(new WebChromeClient() {
	                	public void onProgressChanged(WebView view, int progress) {
	                		activity.setProgress(progress * 100);
	                	}
	                });
	                webview.setWebViewClient(new WebViewClient() {  
	                    @Override  
	                    public boolean shouldOverrideUrlLoading(WebView view, String url) {  
	                    	view.loadUrl(url);  
	                    	return true;  
	                    }
	                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
	                        Toast.makeText(activity, getString(R.string.error) + description, Toast.LENGTH_SHORT).show();
	                    }
	                });  
	                activity.setContentView(webview);

	        		webview.postUrl("https://www.paypal.com/cgi-bin/webscr", post);

	                // -------------- browser alternative to webview: --------------
	                //Intent viewIntent = new Intent(Intent.ACTION_VIEW, payPalUri);
	                //startActivity(viewIntent);
	        		// -------------------------------------------------------------

	                return true;
	            }
	        });
		}
		
		/*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	updatePrefSummary(findPreference(key));
		}

		private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory)p;
        		for(int i=0;i<pCat.getPreferenceCount();i++){
        			initSummary(pCat.getPreference(i));
        	    }
        	}else{
        		updatePrefSummary(p);
        	}
        }
        
        private void updatePrefSummary(Preference p){
        	if (p instanceof ListPreference) {
        		ListPreference listPref = (ListPreference) p;
        	    p.setSummary(listPref.getEntry());
        	}
        }*/
    }
}
