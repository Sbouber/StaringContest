package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class NewScoreActivity extends Activity {

	private long time;
	final private String NAME = "name";
	final private String SCORE = "score";
	private int entryIndex = 0;
	private long highscore = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_score);
		// Show the Up button in the action bar.
		setupActionBar();

		// Get the time from the intent
		Intent intent = getIntent();
		time = intent.getLongExtra(null, 1);

		// Create the text view
		TextView textView = (TextView) findViewById(R.id.textView1);
		textView.setTextSize(40);
		textView.setText("" + time);

		SharedPreferences myPrefs = this.getSharedPreferences("myPrefs",
				MODE_PRIVATE);

		long score = 0;
		long newScore = time;
		String scoreEntry = "";

		/*
		 * Puts the scores in the right order and checks whether there is a
		 * highscore.
		 */
		for (int i = 1; i <= 10; i++) {
			scoreEntry = SCORE.concat(Integer.toString(i));

			score = myPrefs.getLong(scoreEntry, 0);

			if (newScore > score) {
				highscore = newScore;
				entryIndex = i;

				break;
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.new_score, menu);
		return true;
	}

	public void resetHigh() {
		Log.e("NewScore", "RESET!");
		SharedPreferences myPrefs = this.getSharedPreferences("myPrefs",
				MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = myPrefs.edit();
		prefsEditor.clear();
		prefsEditor.commit();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(false);

	}

	public void sendName(View view) {
		Intent intent = new Intent(this, HighscoresActivity.class);
		SharedPreferences myPrefs = this.getSharedPreferences("myPrefs",
				MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = myPrefs.edit();

		EditText editText = (EditText) findViewById(R.id.edit_message);
		String name = editText.getText().toString();
		long score = highscore;
		String nameEntry = "";
		String scoreEntry = "";
		String tmpName = "";
		long tmpScore = 0;
		int index = entryIndex;

		for (int i = 10; i >= index; i--) {
			nameEntry = NAME.concat(Integer.toString(i));
			scoreEntry = SCORE.concat(Integer.toString(i));
			tmpName = myPrefs.getString(nameEntry, "");
			tmpScore = myPrefs.getLong(scoreEntry, 0);

			nameEntry = NAME.concat(Integer.toString(i + 1));
			scoreEntry = SCORE.concat(Integer.toString(i + 1));
			prefsEditor.putString(nameEntry, tmpName);
			prefsEditor.putLong(scoreEntry, tmpScore);
			prefsEditor.commit();

		}

		nameEntry = NAME.concat(Integer.toString(index));
		scoreEntry = SCORE.concat(Integer.toString(index));
		prefsEditor.putString(nameEntry, name);
		prefsEditor.putLong(scoreEntry, score);
		prefsEditor.commit();

		Log.e("Highscores", "Name: " + name);

		startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
}
