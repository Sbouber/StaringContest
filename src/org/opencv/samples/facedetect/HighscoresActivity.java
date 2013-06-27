package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class HighscoresActivity extends Activity {

	final private String NAME = "name";
	final private String SCORE = "score";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_highscores);
		// Show the Up button in the action bar.
		setupActionBar();

		String nameEntry = "";
		String scoreEntry = "";
		boolean isTablet = isTabletDevice(this);

		SharedPreferences myPrefs = this.getSharedPreferences("myPrefs",
				MODE_PRIVATE);
		TableLayout table = (TableLayout) findViewById(R.id.tableLayout);

		for (int i = 0; i < 11; i++) {
			TextView rank = new TextView(this);
			TextView name = new TextView(this);
			TextView score = new TextView(this);

			if (i == 0) {
				rank.setText("RANK\t\t");
				name.setText("NAME");
				score.setText("SCORE");
			} else {
				nameEntry = NAME.concat(Integer.toString(i));
				scoreEntry = SCORE.concat(Integer.toString(i));

				rank.setText(Integer.toString(i));
				name.setText(myPrefs.getString(nameEntry, ""));
				score.setText("" + myPrefs.getLong(scoreEntry, 0));

				if (isTablet) {
					rank.setTextSize(40);
					name.setTextSize(40);
					score.setTextSize(40);
				}
			}

			TableRow rowHeader = new TableRow(this);

			rowHeader.addView(rank);
			rowHeader.addView(name);
			rowHeader.addView(score);

			table.addView(rowHeader);
		}

	}

	public void resetHigh(View view) {
		SharedPreferences myPrefs = this.getSharedPreferences("myPrefs",
				MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = myPrefs.edit();
		prefsEditor.clear();
		prefsEditor.commit();

		Intent intent = getIntent();
		finish();
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
		getMenuInflater().inflate(R.menu.highscores, menu);
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
			// NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onBackPressed() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	/* http://stackoverflow.com/questions/5832368/tablet-or-phone-android */
	public static boolean isTabletDevice(Context activityContext) {
		// Verifies if the Generalized Size of the device is XLARGE to be
		// considered a Tablet
		boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);

		// If XLarge, checks if the Generalized Density is at least MDPI
		// (160dpi)
		if (xlarge) {
			DisplayMetrics metrics = new DisplayMetrics();
			Activity activity = (Activity) activityContext;
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

			// MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160,
			// DENSITY_TV=213, DENSITY_XHIGH=320
			if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
					|| metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
					|| metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
					|| metrics.densityDpi == DisplayMetrics.DENSITY_TV
					|| metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {

				// Yes, this is a tablet!
				return true;
			}
		}

		// No, this is not a tablet!
		return false;
	}

}
