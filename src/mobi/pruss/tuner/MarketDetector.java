package mobi.pruss.tuner;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MarketDetector {
	public static final int MARKET = 0;
	public static final int APPSTORE = 1;
	public static final String[] marketNames = { "Play", "Appstore" };
	public static final String[][] apps = {
		   { "mobi.omegacentauri.red", "Color Changer Free [root]", 
			"Completely remap screen colors, for fun or to preserve night vision. Red mode, green mode, black and white, sepia, etc. [Free lite version and trial of pro]" },
			{ "mobi.omegacentauri.ScreenDim.Trial", "ScreenDim", 
			"Dim screen backlight, often below factory settings.  [Trial of full version]" },
			{ "mobi.omegacentauri.LunarMap.Lite", "LunarMap",
				"Many different maps of the moon for amateur astronomy and fun. [Free lite version]"
			},
			{ "mobi.omegacentauri.SpeakerBoost", "SpeakerBoost", "Boost sound volume on some devices. [Completely free with no ads, but there is also a donation version]"
			}
			
	};

	public static void launch(Context c, String app) {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (detect(c) == MARKET) {
    		if (app==null) 
    			i.setData(Uri.parse("market://search?q=pub:\"Omega Centauri Software\""));
    		else
    			i.setData(Uri.parse("market://details?id="+app));
    	}
    	else {
    		if (app == null)
    			app = "mobi.omegacentauri.red&showAll=1";
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p="+app));
    	}
    	c.startActivity(i);    	
	}
	
	public static void selectApp(final Activity activity) {
		AlertDialog.Builder b = new AlertDialog.Builder(activity);
		View v = (View)activity.getLayoutInflater().inflate(R.layout.apps, null);
		b.setView(v);
		ListView lv = (ListView)v.findViewById(R.id.apps);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,R.layout.twoline) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				
				
				if (convertView == null) {
	                v = View.inflate(activity, R.layout.twoline, null);
	            }
				else {
					v = convertView;
				}
				
				TextView tv = (TextView)v.findViewById(R.id.text1);
				tv.setText(apps[position][1]);
				tv = (TextView)v.findViewById(R.id.text2);
				tv.setText(apps[position][2]);
				return v;
			}				
		};
		lv.setAdapter(adapter);
		for (int i = 0 ; i < apps.length ; i++)
			adapter.add(apps[i][0]);
		final AlertDialog dialog = b.create();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				dialog.cancel();
				launch(activity, apps[position][0]);
			}
		});
		dialog.show();
	}
	
	public static int detect(Context c) {
		PackageManager pm = c.getPackageManager();
				
		String installer = pm.getInstallerPackageName(c.getPackageName());
		
		if (installer != null && installer.equals("com.android.vending")) 
			return MARKET;
		
		if (Build.MODEL.equalsIgnoreCase("Kindle Fire")) 
			return APPSTORE;

		try {
			if (pm.getPackageInfo("com.amazon.venezia", 0) != null) 
				return APPSTORE;
		} catch (NameNotFoundException e) {
		}
		
		return MARKET;
	}
	
	public static String getMarketName(Context c) {
		return marketNames[detect(c)];
	}
}
