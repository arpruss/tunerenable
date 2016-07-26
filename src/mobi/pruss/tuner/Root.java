package mobi.pruss.tuner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;

import android.os.Build;
import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;

	public boolean isValid() {
		return rootCommands != null;
	}
	
	static public String getRootVersion() {
		Process p = null;
		
		try {
			p = new ProcessBuilder() 
						.command("su", "-v")
						.redirectErrorStream(true)
						.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			line = r.readLine();
			r.close();
			if (line != null)
				return line.trim();
			return line;
		}
		catch (Exception e) {
			return null;
		}
		finally {
			if (p != null)
				p.destroy();
		}
	}
	
	static public boolean supportSecurityContext(String version) {
		if (version == null)
			return false;
		int index = version.toLowerCase().indexOf(":supersu");
		if (index < 0)
			return false;
		try {
			return Double.parseDouble(version.substring(0, index) ) > 1.89;
		}
		catch (Exception e) {
			Log.e("tuner","Error parsing su version");
			return false;
		}
	}

	static public String[] getSupportedSecurityContexts(String version, String preferred) {
		if (! supportSecurityContext(version)) 
			return new String[] { "" };
		
		if (! preferred.equals("u:r:init:s0")) { 
			return new String[] { "u:r:system_app:s0" , "u:r:init:s0" };
		}
		else {
			return new String[] { "u:r:init:s0" , "u:r:system_app:s0" };
		}
	}
	
	public Root() {
		this(getSupportedSecurityContexts(getRootVersion(), "")[0]);
	}

	public Root(String secCon) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			if (secCon.length() > 0) {
				// pb.command("su", "-cn", "u:r:init:s0"); 
				pb.command("su", "-cn", secCon);
			}
			else {
				pb.command("su");
			}
			pb.redirectErrorStream(true);
			rootShell = pb.start();
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
		}
		catch (Exception e) {
			rootCommands = null;
		}
	}
	
	public boolean execOne(String s, String successMarker, String failureMarker) {
		try {
			DataInputStream rootOutput = new DataInputStream(rootShell.getInputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(rootOutput));
			rootCommands.writeBytes(s + "; exit\n");
			rootCommands.close();
			if (successMarker == null) {
				br.close();
				return true;
			}
			String line;
			while((line=br.readLine())!=null) {
				if (line.trim().matches(successMarker)) {
					br.close();
					return true;
				}
				if (line.trim().matches(failureMarker)) {
					br.close();
					return false;
				}
			}
			return false;
		}
		catch (Exception e) {
			Log.e("tuner", "Error executing: "+e);
			return false;
		}
	}
	
	public static boolean test() {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public void close() {
		close(false);
	}
	
	public void close(boolean wait) {
		if (rootCommands != null) {
			try {
				rootCommands.close();
				if (wait)
					rootShell.waitFor();
			}
			catch (Exception e) {
			}
			rootCommands = null;
		}

		if (rootShell != null) {
			try {
					rootShell.destroy();
			}
			catch (Exception e) {
			}
			rootShell = null;
		}
	}
	
	public void exec( String s ) {
		try {
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
		}
		catch (Exception e) {
			Log.e("tuner", "Error executing: "+e);
		}
	}
	
	public static String execGetOneLine(String cmd) {
		try {
			Process p = new ProcessBuilder() 
			.command("su")
			.redirectErrorStream(true)
			.start();
			DataOutputStream c = new DataOutputStream(p.getOutputStream());
			c.writeChars(cmd + "\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String s = br.readLine();
			
			if (s == null)
				s = "";
			
			if(p.waitFor() != 0) {
				return null;
			}
			return s;
		}
		catch (Exception e) {
			return null;
		}
	}	
}
