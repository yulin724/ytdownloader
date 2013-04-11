/* class SingleMediaScanner from Stack Overflow:
 * 
 * http://stackoverflow.com/questions/4646913/android-how-to-use-mediascannerconnection-scanfile/5815005#5815005
 * 
 * Q: http://stackoverflow.com/users/538837/erik
 * A: http://stackoverflow.com/users/233947/petrus
 */
package dentex.youtube.downloader.utils;

import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class SingleMediaScanner implements MediaScannerConnectionClient {
	
	private String DEBUG_TAG = "SingleMediaScanner";

	private MediaScannerConnection mMs;
	private File mFile;
	private String mMime;
	
	public SingleMediaScanner(Context context, File f, String mime) {
	    mFile = f;
	    mMime = mime;
	    mMs = new MediaScannerConnection(context, this);
	    mMs.connect();
	}
	
	@Override
	public void onMediaScannerConnected() {
	    mMs.scanFile(mFile.getAbsolutePath(), mMime);
	}
	
	@Override
	public void onScanCompleted(String path, Uri uri) {
		Utils.logger("d", "Scanned " + path + ":", DEBUG_TAG);
		Utils.logger("d", "-> uri: " + uri, DEBUG_TAG);
	    mMs.disconnect();
	}
}
