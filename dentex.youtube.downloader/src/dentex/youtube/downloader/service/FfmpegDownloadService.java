package dentex.youtube.downloader.service;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.utils.Utils;

public class FfmpegDownloadService extends Service {
	
	private static final String DEBUG_TAG = "FfmpegDownloadService";
	public static Context nContext;
	public long enqueue;
	public String ffmpegFilename = FfmpegController.LIB_ASSETS[0];
	private int cpuVers;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		nContext = getBaseContext();
		Utils.logger("d", "service created", DEBUG_TAG);
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
		String link = "http://sourceforge.net/projects/ytdownloader/files/ffmpeg/download";
		//TODO upload on SF two ffmpeg binaries with "_arm7" and "_arm5" (or similar) suffix
		Utils.logger("d", "link will be: http://sourceforge.net/projects/ytdownloader/files/ffmpeg_arm" + cpuVers + "/download", DEBUG_TAG);
		
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = new Request(Uri.parse(link));
        request.setDestinationInExternalFilesDir(nContext, null, ffmpegFilename);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("Downloading FFmpeg binary");
        enqueue = dm.enqueue(request);
	}
	
	BroadcastReceiver ffmpegReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO copy ffmpeg to /data/data/dentex.youtube.downloader/app_bin
		}
	
	};
	
}
