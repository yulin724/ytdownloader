package android.ffmpeg.cmdline;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.util.Log;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

public class SuCommand {
	
	//public final static File sdcard = Environment.getExternalStorageDirectory();
	//public static SharedPreferences settings = ShareActivity.settings;
	//public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	private final static String DEBUG_TAG = "SuCommand";
	static boolean BB = RootTools.isBusyboxAvailable();
	static boolean SU = RootTools.isRootAvailable();
	
	public static void suCmd(String cmd) {
		int res = 4;
		if (BB && SU) {
			CommandCapture command = new CommandCapture(0, cmd);
			Log.d(DEBUG_TAG, cmd);
			
			try {
				RootTools.getShell(true).add(command).waitForFinish();
			} catch (InterruptedException e) {
				res = res - 1;
			} catch (IOException e) {
				res = res - 1;
			} catch (TimeoutException e) {
				res = res - 1;
			} catch (RootDeniedException e) {
				res = res - 1;
			} finally {
				if (res == 4) {
					Log.i(DEBUG_TAG, "su_command_ok");
				} else {
					Log.e(DEBUG_TAG, "error executing su command");
					//Toast.makeText(context, "error executing su command", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

}
