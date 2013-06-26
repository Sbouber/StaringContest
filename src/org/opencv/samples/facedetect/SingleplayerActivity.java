package org.opencv.samples.facedetect;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SingleplayerActivity extends Activity {
	
	private long time;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_singleplayer);
		// Show the Up button in the action bar.
		setupActionBar();
		
		final Button testButton = (Button) findViewById(R.id.button1);
		testButton.setVisibility(View.GONE);
		
		new CountDownTimer(5000, 100) {
			TextView textView = (TextView) findViewById(R.id.textView1);

		     public void onTick(long millisUntilFinished) {
		         textView.setText("" + ((millisUntilFinished / 1000) + 1));
		     }

		     public void onFinish() {
		    	 textView.setText("Start!");
		    	 
		    	 Timer t = new Timer(false);
		    	
		    	 t.schedule(new TimerTask() {		    	 
		    		 @Override
		    		 public void run() {
			        	
		    			 runOnUiThread(new Runnable() {
		    				 public void run() {
		    					 textView.setVisibility(View.INVISIBLE);
		    				 }
		    			 });
		    		 }
		    	 }, 1000);
		    	
		        time = System.currentTimeMillis();
		        stopTime(testButton);
		     }
		}.start();
	}
	
	public void stopTime(Button testButton) {
		testButton.setVisibility(View.VISIBLE);
		testButton.setOnClickListener( new View.OnClickListener() {

			@Override
			public void onClick (View v) {
					time = System.currentTimeMillis() - time;
					sendScore(v);
			} 
		});
	}
	
	public void sendScore(View view) {
	    Intent intent = new Intent(this, NewScoreActivity.class);
	    intent.putExtra(null, time);
	    startActivity(intent);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.singleplayer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
//			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/*public void timer(View view) {
		if (!start) {
			
		} else {
			
		}

		Intent intent = new Intent(this, SingleplayerActivity.class);
		//EditText editText = (EditText) findViewById(R.id.edit_message);
		//String message = editText.getText().toString();
		//intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
	} */

}
