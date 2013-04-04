package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.utils.Utils;

public class FfmpegDownloadService extends Service {
	
	private static final String DEBUG_TAG = "FfmpegDownloadService";
	public static Context nContext;
	public long enqueue;
	public String ffmpegBinName = FfmpegController.ffmpegBinName;
	private int cpuVers;
	private DownloadManager dm;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		nContext = getBaseContext();
		Utils.logger("d", "service created", DEBUG_TAG);
		registerReceiver(ffmpegReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	public static Context getContext() {
        return nContext;
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		cpuVers = intent.getIntExtra("CPU", 7);
		Utils.logger("d", "arm CPU version: " + cpuVers, DEBUG_TAG);
		
		downloadFfmpeg();
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Utils.logger("d", "service destroyed", DEBUG_TAG);
		unregisterReceiver(ffmpegReceiver);
	}
	
	private void downloadFfmpeg() {
		String link = getString(R.string.ffmpeg_download_dialog_msg_link, cpuVers);

		Utils.logger("d", "FFmpeg download link: " + link, DEBUG_TAG);
		
        Request request = new Request(Uri.parse(link));
        request.setDestinationInExternalFilesDir(nContext, null, ffmpegBinName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("Downloading FFmpeg binary");
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        enqueue = dm.enqueue(request);
	}
	
	BroadcastReceiver ffmpegReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Utils.logger("d", "ffmpegReceiver: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		
    		if (enqueue != -1 && id == enqueue) {
	    		Query query = new Query();
				query.setFilterById(id);
				Cursor c = dm.query(query);
				if (c.moveToFirst()) {
				
					int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
					int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
					int status = c.getInt(statusIndex);
					int reason = c.getInt(reasonIndex);
					
					//long size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
	
					switch (status) {
					
					case DownloadManager.STATUS_SUCCESSFUL:
	    		
						File src = new File(nContext.getExternalFilesDir(null), ffmpegBinName);
						File dst = new File(nContext.getDir("bin", 0), ffmpegBinName);
						try {
							Utils.copyFile(src, dst, nContext);
							Utils.logger("i", "trying to copy FFmpeg binary to private App dir", DEBUG_TAG);
						} catch (IOException e) {
							Toast.makeText(context, getString(R.string.ffmpeg_install_failed), Toast.LENGTH_LONG).show();
							Log.e(DEBUG_TAG, "ffmpeg copy to app_bin failed. " + e.getMessage());
						}
						break;
						
					case DownloadManager.STATUS_FAILED:
						Log.e(DEBUG_TAG, ffmpegBinName + ", _ID " + id + " FAILED (status " + status + ")");
						Log.e(DEBUG_TAG, " Reason: " + reason);
						Toast.makeText(context,  ffmpegBinName + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
						break;
						
					default:
						Utils.logger("w", ffmpegBinName + ", _ID " + id + " completed with status " + status, DEBUG_TAG);
					}
				}
    		}
    		stopSelf();
		}
	};
	
}
