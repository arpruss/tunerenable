package mobi.pruss.tuner;

import java.lang.reflect.Modifier;
import java.text.DateFormat.Field;
import java.util.ArrayList;
import java.util.List;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.method.DigitsKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TunerActivity extends Activity {
	static final ComponentName component = ComponentName.unflattenFromString("com.android.systemui/.tuner.TunerActivity");
	static final String INTENT_PREFIX = "intent(";
	static final String TILE_SETTING = "sysui_qs_tiles";
	static final String defaults = "wifi,bt,dnd,rotation,airplane,saver,fingerprint,extsaver,cell,inversion,location,hotspot,flashlight,vowifi,"+INTENT_PREFIX;
	static final String descriptions = "WiFi,Bluetooth,Do not disturb,Rotation,Airplane mode,Power Saver,Fingerprint,Extreme Saver,Cell Data,Invert Screen,Location,Hotspot,Flashlight,Voice over WiFi,Broadcast Tile";
	
	private void addRawQSTile(String currentTileSetting, String string) {
		if (currentTileSetting.length() > 0)
			currentTileSetting = currentTileSetting + "," + string;
		else
			currentTileSetting = "";
		new TweakTask(this).execute("settings put secure "+TILE_SETTING+" '"+currentTileSetting+"' && pwd", "^/.*", "^[^/]");
	}
	
	private static boolean validateIntent(String s) {
		return s.matches("^[0-9A-Za-z_.]+$");
	}

	private void addQSTile(final String currentTileSetting, String string) {
		if (string.equals(INTENT_PREFIX)) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			final EditText input = new EditText(this);
			b.setView(input);
			b.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String i = input.getText().toString();
					if (i.length() > 0 && validateIntent(i))
						addRawQSTile(currentTileSetting, INTENT_PREFIX+i+")");
					else
						Toast.makeText(TunerActivity.this, "Invalid intent", Toast.LENGTH_LONG).show();
					dialog.cancel();
				}
			});
			
			b.setNegativeButton("Cancel", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			b.show();
		}
		else {
			addRawQSTile(currentTileSetting, string);
		}
	}

	public void addSetting(View v) {
		final String currentTileSetting = Secure.getString(getContentResolver(), TILE_SETTING);
		String[] currentTiles = (currentTileSetting == null) ? new String[0] : currentTileSetting.split(",");
		final List<String> addableTiles = new ArrayList<String>();
		List<String> addableDescriptions = new ArrayList<String>();
		final String[] defaultArray = defaults.split(",");
		String[] descriptionArray = descriptions.split(",");
		for (int i = 0 ; i < defaultArray.length ; i++) {
			boolean found = false;
			for (String c : currentTiles) {
				if (c.equals(defaultArray[i])) {
					found = true;
					break;
				}
			}
			if (!found) {
				addableTiles.add(defaultArray[i]);
				addableDescriptions.add(descriptionArray[i]);
			}
		}
				
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setItems(addableDescriptions.toArray(new String[0]), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addQSTile(currentTileSetting, addableTiles.get(which));
			}
		});
		b.show();
	}
	
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
			findViewById(R.id.add).setVisibility(View.VISIBLE);
			findViewById(R.id.off).setVisibility(View.VISIBLE);
			findViewById(R.id.on).setVisibility(View.GONE);
		}
		else {
			findViewById(R.id.launch).setVisibility(View.INVISIBLE);
			findViewById(R.id.add).setVisibility(View.INVISIBLE);
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
		MarketDetector.selectApp(this);
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
			if (opt.length >= 3) {
				Root r = new Root("");
				return r.execOne(opt[0], opt[1], opt[2]);
			}
			else {
				Root r = new Root("");
				r.exec(opt[0]);
				r.close(true);
				return true;
			}
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
