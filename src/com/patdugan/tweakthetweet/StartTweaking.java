package com.patdugan.tweakthetweet;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class StartTweaking extends Activity {	

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);   
        
        // Set icon padding
        ImageView view = (ImageView)findViewById(android.R.id.home);
		view.setPadding(0, 0, 10, 0);
        
    }
}