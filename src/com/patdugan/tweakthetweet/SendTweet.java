package com.patdugan.tweakthetweet;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.patdugan.tweakthetweet.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SendTweet extends Activity implements
LocationListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener {

// A request to connect to Location Services
private LocationRequest mLocationRequest;

// Stores the current instantiation of the location client in this object
private LocationClient mLocationClient;

// Handle to SharedPreferences for this app
SharedPreferences mPrefs;

// Handle to a SharedPreferences editor
SharedPreferences.Editor mEditor;

/*
* Note if updates have been turned on. Starts out as "false"; is set to "true" in the
* method handleRequestSuccess of LocationUpdateReceiver.
*
*/
boolean mUpdatesRequested = false;
	// Constants
	static String TWITTER_CONSUMER_KEY = "q3v34CLiPR7WhO9MwIsDw"; 
	static String TWITTER_CONSUMER_SECRET = "NfcsfyYqJ7G2Osa8w5HHY1AcCDIwHIDT82V0W0pFM"; 

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

	// Login button
	Button btnLoginTwitter;
	// Update status button
	Button btnUpdateStatus;
	// Logout button
	Button btnLogoutTwitter;
	// EditText for update
	EditText txtUpdate;
	// lbl update
	TextView lblUpdate;
	TextView lblUserName;
	TextView tweetCharacterCount;
	
	// Hashtag labels
	TextView needLabel;
	TextView offerLabel;
	TextView damageLabel;
	TextView shelterLabel;

	// Progress dialog
	ProgressDialog pDialog;

	// Twitter
	private static Twitter twitter;
	private static RequestToken requestToken;
	
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
	
	// Internet Connection detector
	private ConnectionDetector cd;
	
	// Alert Dialog Manager
	AlertDialogManager alert = new AlertDialogManager();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// Disable strict mode to run network on main thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_tweet);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Get actionbar
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);
		
		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(SendTweet.this, "Internet Connection Error",
					"Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}
		
		// Check if twitter keys are set
		if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
			// Internet Connection is not present
			alert.showAlertDialog(SendTweet.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
			// stop executing code by return
			return;
		}
		
		// Location calls
		// Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        mUpdatesRequested = false;

        // Open Shared Preferences
        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get an editor
        mEditor = mPrefs.edit();
   
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

		// All UI elements
		btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
		btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
		btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
		txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
		lblUpdate = (TextView) findViewById(R.id.lblUpdate);
		lblUserName = (TextView) findViewById(R.id.lblUserName);
		tweetCharacterCount = (TextView) findViewById(R.id.tweetCharacterCount);
		
		needLabel = (TextView) findViewById(R.id.need_label);
		offerLabel = (TextView) findViewById(R.id.offer_label);
		damageLabel = (TextView) findViewById(R.id.damage_label);
		shelterLabel = (TextView) findViewById(R.id.shelter_label);
		
		// getAddress();
		mLocationClient.connect();
		// look for onconnected() for rest of code here

		// Listener for character count
		txtUpdate.addTextChangedListener(mTextEditorWatcher);
		
		String eventName = "#SeaQuake #Loc";
		txtUpdate.append(eventName);

		// Shared Preferences
		mSharedPreferences = getApplicationContext().getSharedPreferences(
				"MyPref", 0);
		
		if (isTwitterLoggedInAlready()) {
			Uri uri = getIntent().getData();
			try {
				// oAuth verifier
				String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
				// Get the access token
				AccessToken accessToken;
				accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
				// Get userid
				long userID = accessToken.getUserId();
				User user = twitter.showUser(userID);
				// Get username
				String username = user.getName();
				// Set username in layout
				lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
			} catch (Exception e) {
				// Check log for login errors
				Log.e("Twitter Login Error", "> " + e.getMessage());
			}
		}
		
		needLabel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtUpdate.append("#need ");
			}
		});
		
		offerLabel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtUpdate.append("#offer ");
			}
		});
		
		shelterLabel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtUpdate.append("#shelter ");
			}
		});
		
		damageLabel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtUpdate.append("#damage ");
			}
		});
		
		/**
		 * Button click event to Update Status, will call updateTwitterStatus()
		 * function
		 * */
		btnUpdateStatus.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Call update status function
				// Get the status from EditText
				String status = txtUpdate.getText().toString();

				// Check for blank text
				if (status.trim().length() > 0) {
					// update status
					new updateTwitterStatus().execute(status);
				} else {
					// EditText is empty
					Toast.makeText(getApplicationContext(),
							"Please enter status message", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
	}

	/**
	 * Function to update status
	 * */
	class updateTwitterStatus extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(SendTweet.this);
			pDialog.setMessage("Updating to twitter...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting Places JSON
		 * */
		protected String doInBackground(String... args) {
			Log.d("Tweet Text", "> " + args[0]);
			String status = args[0];
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
				builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
				
				// Access Token 
				String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
				
				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
				
				// Update status
				twitter4j.Status response = twitter.updateStatus(status);
				
				Log.d("Status", "> " + response.getText());
			} catch (TwitterException e) {
				// Error in updating status
				Log.d("Twitter Update Error", e.getMessage());
			}
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog and show
		 * the data in UI Always use runOnUiThread(new Runnable()) to update UI
		 * from background thread, otherwise you will get error
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),
							"Status tweeted successfully", Toast.LENGTH_SHORT)
							.show();
					// Clearing EditText field
					txtUpdate.setText("");
				}
			});
		}

	}

	/**
	 * Check user already logged in your application using twitter Login flag is
	 * fetched from Shared Preferences
	 * */
	private boolean isTwitterLoggedInAlready() {
		// return twitter login status from Shared Preferences
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}

	protected void onResume() {
		super.onResume();
		
		// If the app already has a setting for getting location updates, get it
        if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);

        // Otherwise, turn off location updates until requested
        } else {
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            mEditor.commit();
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_send, menu);
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
	    		Intent goHome = new Intent(SendTweet.this, MainActivity.class);
	    	    startActivity(goHome);
	            return true;
	        case R.id.tweet_send:
	        	// Call update status function
				// Get the status from EditText
				String status = txtUpdate.getText().toString();

				// Check for blank text
				if (status.trim().length() > 0) {
					// update status
					new updateTwitterStatus().execute(status);
				} else {
					// EditText is empty
					Toast.makeText(getApplicationContext(),
							"Please enter status message", Toast.LENGTH_SHORT)
							.show();
				}
				mLocationClient.disconnect();
	        case android.R.id.home:
	            NavUtils.navigateUpFromSameTask(this);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {

        // If the client is connected
        if (mLocationClient.isConnected()) {
            // stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        super.onStop();
    }
    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {

        // Save the current setting for updates
        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();

        super.onPause();
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        // Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        // Display the result
                        // mConnectionState.setText(R.string.connected);
                        // mConnectionStatus.setText(R.string.resolved);
                    break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        // Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        // Display the result
                        // mConnectionState.setText(R.string.disconnected);
                        // mConnectionStatus.setText(R.string.no_resolution);

                    break;
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               // Log.d(LocationUtils.APPTAG,
               //        getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }
	
	private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            // Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
        }
        return false;
    }
	
	@SuppressLint("NewApi")
    public void getAddress(View v) {
        if (servicesConnected()) {
            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();
            // Turn the indefinite activity indicator on
            // mActivityIndicator.setVisibility(View.VISIBLE);
            // Start the background task
            (new SendTweet.GetAddressTask(this)).execute(currentLocation);
        }
    }
	
	private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
           //This sets a textview to the current length
        	Integer remainingCharacter = 140 - Integer.valueOf(s.length());
        	tweetCharacterCount.setText(String.valueOf(remainingCharacter));
        }

		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			
		}
	};

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		if (servicesConnected()) {
            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();
            // Turn the indefinite activity indicator on
            // mActivityIndicator.setVisibility(View.VISIBLE);
            // Start the background task
            (new SendTweet.GetAddressTask(this)).execute(currentLocation);
        }
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
	}
	   /**
     * An AsyncTask that calls getFromLocation() in the background.
     * The class uses the following generic types:
     * Location - A {@link android.location.Location} object containing the current location,
     *            passed as the input parameter to doInBackground()
     * Void     - indicates that progress units are not used by this subclass
     * String   - An address passed to onPostExecute()
     */
    protected class GetAddressTask extends AsyncTask<Location, Void, String> {

        // Store the context passed to the AsyncTask when the system instantiates it.
        Context localContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {

            // Required by the semantics of AsyncTask
            super();

            // Set a Context for the background task
            localContext = context;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected String doInBackground(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
            Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

            // Get the current location from the input parameter list
            Location location = params[0];

            // Create a list to contain the result address
            List <Address> addresses = null;

            // Try to get an address for the current location. Catch IO or network problems.
            try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
                addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1
                );

                // Catch network or other I/O problems.
                } catch (IOException exception1) {

                    // Log an error and return an error message
                    // Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));

                // Catch incorrect latitude or longitude values
                } catch (IllegalArgumentException exception2) {

                    // Construct a message containing the invalid arguments
                	
                }
                // If the reverse geocode returned an address
                if (addresses != null && addresses.size() > 0) {

                    // Get the first address
                    Address address = addresses.get(0);

                    // Format the first line of address
                    String addressText = getString(R.string.address_output_string,

                            // If there's a street address, add it
                            address.getMaxAddressLineIndex() > 0 ?
                                    address.getAddressLine(0) : "",

                            // Locality is usually a city
                            address.getLocality(),

                            // The country of the address
                            address.getCountryName()
                    );

                    // Return the text
                    return addressText;

                // If there aren't any addresses, post a message
                } else {
                  // no string found
                }
				return null;
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(String address) {

            // Turn off the progress bar
            // mActivityIndicator.setVisibility(View.GONE);

            // Set the address in the UI
            // mAddress.setText(address);
            txtUpdate.append(" " + address + " ");
        }
    }
}