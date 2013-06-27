package org.opencv.samples.facedetect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void goToSingleplayer(View view) {
		Intent intent = new Intent(this, FdActivity.class);
		startActivity(intent);
	}

	public void goToMultiplayer(View view) {
		// Intent intent = new Intent(this, MultiplayerActivity.class);
		// startActivity(intent);

		Toast toast = Toast.makeText(getApplicationContext(),
				"Multiplayer is not available yet...", Toast.LENGTH_SHORT);
		toast.show();
	}

	public void goToHighscores(View view) {
		Intent intent = new Intent(this, HighscoresActivity.class);
		startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog
				.setMessage("Exit this app")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent startMain = new Intent(Intent.ACTION_MAIN);
						startMain.addCategory(Intent.CATEGORY_HOME);
						startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(startMain);
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// User cancelled the dialog
							}
						});
		alertDialog.show();
	}

	public void goToSettings(MenuItem item) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

}
