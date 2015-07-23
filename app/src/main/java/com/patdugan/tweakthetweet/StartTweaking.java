package com.patdugan.tweakthetweet;

// import twitter4j.Twitter;
// import twitter4j.auth.RequestToken;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class StartTweaking extends Activity {
	
	Button btnMap;
	Circle circle;
	GoogleMap mMap;
	
	static String TWITTER_CONSUMER_KEY = "q3v34CLiPR7WhO9MwIsDw"; 
	static String TWITTER_CONSUMER_SECRET = "NfcsfyYqJ7G2Osa8w5HHY1AcCDIwHIDT82V0W0pFM";
	static final LatLng SEATTLE = new LatLng(47.6594964, -122.3197063);

	// Preference Constants
	static String PREFERENCE_NAME = "twitter_oauth";
	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

	// Twitter oauth urls
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
	
	// Twitter
	// private static Twitter twitter;
	// private static RequestToken requestToken;
		
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
		
	// Internet Connection detector
	// private ConnectionDetector cd;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);   
        
        // Set icon padding
        ImageView view = (ImageView)findViewById(android.R.id.home);
		view.setPadding(0, 0, 10, 0);
		
		mSharedPreferences = getApplicationContext().getSharedPreferences(
				"MyPref", 0);
		
		// Initalize map
		setUpMapIfNeeded();
		
		// find map button
		btnMap = (Button) findViewById(R.id.mapButton);
		btnMap.setOnClickListener(new View.OnClickListener() {

			@Override
			// set button listener
			public void onClick(View arg0) {
				Intent circleMap = new Intent(StartTweaking.this, SendTweet.class);
	    	    startActivity(circleMap);
			}
		});	
    }

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
        	 mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                     .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
            	// Set circle on map
        		circle = mMap.addCircle(new CircleOptions()
        			// Seattle latlong
        	     	.center(new LatLng(47.6594964, -122.3197063))
        	     	.radius(10000)
        	     	.strokeColor(Color.TRANSPARENT)
        	     	// Fill color of the circle
                    // 0x represents, this is an hexadecimal code
                    // 55 represents percentage of transparency. For 100% transparency, specify 00.
                    // The remaining 6 characters(FF4D4D) specify the fill color
                    .fillColor(0x55FF4D4D));
        	     	// solid red fill color that we aren't using
        	     	// .fillColor(Color.RED));
        		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SEATTLE, 10));
            }
        }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.menu_logout:
	        	Editor e = mSharedPreferences.edit();
	    		e.remove(PREF_KEY_OAUTH_TOKEN);
	    		e.remove(PREF_KEY_OAUTH_SECRET);
	    		e.remove(PREF_KEY_TWITTER_LOGIN);
	    		e.commit();
	    		Intent goHome = new Intent(StartTweaking.this, MainActivity.class);
	    	    startActivity(goHome);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}