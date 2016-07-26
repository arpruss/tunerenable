package mobi.pruss.tuner;

import java.lang.reflect.Modifier;
import java.text.DateFormat.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TunerActivity extends Activity {
	static final ComponentName component = ComponentName.unflattenFromString("com.android.systemui/.tuner.TunerActivity");
	
	private void message(String title, String msg, final boolean exit) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(exit) finish();
			} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {if(exit) finish();} });
		alertDialog.show();
	}
	
	boolean isEnabled() {
		int state = getPackageManager().getComponentEnabledSetting(component);
		return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v("tuner", "OnCreate");
		
		setContentView(R.layout.main);
		
	}
	
	public void update() {
		if (isEnabled()) {
			findViewById(R.id.launch).setVisibility(View.VISIBLE);
			findViewById(R.id.off).setVisibility(View.VISIBLE);
			findViewById(R.id.on).setVisibility(View.GONE);
		}
		else {
			findViewById(R.id.launch).setVisibility(View.INVISIBLE);
			findViewById(R.id.on).setVisibility(View.VISIBLE);
			findViewById(R.id.off).setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
	}
	
	public void clickedOn(View v) {
		new TweakTask(this).execute("pm enable com.android.systemui/.tuner.TunerActivity", "^Component.*", "^Error.*");
	}

	public void clickedOff(View v) {
		new TweakTask(this).execute("pm disable com.android.systemui/.tuner.TunerActivity",  "^Component.*", "^Error.*");
	}

	public void otherApps(View v) {
		MarketDetector.launch(this, true);
	}

	public void clickedLaunch(View v) {
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setComponent(component);
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(i);
		}
		catch(Exception e) {
			Toast.makeText(this, "Failure: Maybe not enabled?", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}	

	class TweakTask extends AsyncTask<String, String, Boolean> {
		final Context	 context;
		ProgressDialog progress;
		SharedPreferences options;

		TweakTask(Context c) {
			context = c;
			options = PreferenceManager.getDefaultSharedPreferences(context);
		}

		@Override
		protected Boolean doInBackground(String... opt) {
			Root r = new Root("");
			return r.execOne(opt[0], opt[1], opt[2]);
		}

		@Override
		protected void onProgressUpdate(String... message) {
			if (progress != null) {
				progress.setMessage(message[0]);
			}

		}

		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(context, "", "Making changes", true, false);
			progress.setCancelable(false);
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(context, "Failure", Toast.LENGTH_LONG).show();
			}

			try {
				progress.dismiss();
			}
			catch (Exception e) {					
			}
			
			TunerActivity.this.update();
		}
	}
}
