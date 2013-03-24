package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.ffmpeg.cmdline.FfmpegController;
import android.ffmpeg.cmdline.ShellUtils;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final static String DEBUG_TAG = "DownloadsService";
	public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	public boolean copy;
	public boolean audio;
	public static int ID;
	public static Context nContext;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		nContext = getBaseContext();
		Utils.logger("d", "service created", DEBUG_TAG);
		registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
	
	public static Context getContext() {
        return nContext;
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		copy = intent.getBooleanExtra("COPY", false);
		if (copy == true) {
			Utils.logger("d", "Copy to extSdcard: true", DEBUG_TAG);
		} else {
			Utils.logger("d", "Copy to extSdcard: false", DEBUG_TAG);
		}
		
		audio = intent.getBooleanExtra("AUDIO", false);
		if (audio == true) {
			Log.d(DEBUG_TAG, "Audio extraction: true");
		} else {
			Log.d(DEBUG_TAG, "Audio extraction: false");
		}
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Utils.logger("d", "service destroyed", DEBUG_TAG);
	    unregisterReceiver(downloadComplete);
	}

	BroadcastReceiver downloadComplete = new BroadcastReceiver() {
    	
    	private NotificationManager cNotificationManager;
		private NotificationCompat.Builder cBuilder;
		private Intent intent2;

		@Override
    	public void onReceive(Context context, Intent intent) {
    		Utils.logger("d", "downloadComplete: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		String vfilename = settings.getString(String.valueOf(id), "video");
    		
			Query query = new Query();
			query.setFilterById(id);
			Cursor c = ShareActivity.dm.query(query);
			if (c.moveToFirst()) {
				
				int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
				int status = c.getInt(statusIndex);
				int reason = c.getInt(reasonIndex);
				
				//long size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)); // see TODO below

				switch (status) {
				
				case DownloadManager.STATUS_SUCCESSFUL:
					Utils.logger("d", "_ID " + id + " SUCCESSFUL (status " + status + ")", DEBUG_TAG);
					ID = (int) id;
					
					cBuilder =  new NotificationCompat.Builder(context);
					cNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					
			    	cBuilder.setSmallIcon(R.drawable.icon_nb);
					cBuilder.setContentTitle(vfilename);
					
					if (copy == true) {
						File src = new File(ShareActivity.dir_Downloads, vfilename);
						File dst = new File(ShareActivity.path, vfilename);
						
						Toast.makeText(context, context.getString(R.string.copy_progress), Toast.LENGTH_SHORT).show();
				        cBuilder.setContentText(context.getString(R.string.copy_progress));
						cNotificationManager.notify(ID, cBuilder.build());
						Utils.logger("i", "_ID " + ID + " Copy in progress...", DEBUG_TAG);
						
						if (settings.getBoolean("enable_own_notification", true) == true) {
							try {
								removeIdUpdateNotification(id);
							} catch (NullPointerException e) {
								Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
							}
						}
							
						intent2 = new Intent(Intent.ACTION_VIEW);

						try {
							Utils.copyFile(src, dst, context);
							
							Toast.makeText(context,  vfilename + ": " + context.getString(R.string.copy_ok), Toast.LENGTH_SHORT).show();
					        cBuilder.setContentText(context.getString(R.string.copy_ok));
					        intent2.setDataAndType(Uri.fromFile(dst), "video/*");
							Utils.logger("i", "_ID " + ID + " Copy OK", DEBUG_TAG);
							
							if (ShareActivity.dm.remove(id) == 0) {
								Toast.makeText(context, getString(R.string.download_remove_failed), Toast.LENGTH_LONG).show();
								Log.e(DEBUG_TAG, "temp download file NOT removed");
								
				        	} else { 
				        		Utils.logger("v", "temp download file removed", DEBUG_TAG);
				        		
				        		// TODO fix: `dst` seems not to return a valid `File`
				        		// Uri dstUri = Uri.fromFile(dst); // <-- tried also this (1)

				        		/*Utils.logger("i", "dst: " + dst.getAbsolutePath());
				        		ShareActivity.dm.addCompletedDownload(filename, 
				        				getString(R.string.ytd_video), 
				        				true, 
				        				"video/*", 
				        				dst.getAbsolutePath(), // <-- dstUri.getEncodedPath(), // (1) 
				        				size,
				        				false);*/
				        	}	        	
						} catch (IOException e) {
							Toast.makeText(context, vfilename + ": " + getString(R.string.copy_error), Toast.LENGTH_LONG).show();
							cBuilder.setContentText(getString(R.string.copy_error));
							intent2.setDataAndType(Uri.fromFile(src), "video/*");
							Log.e(DEBUG_TAG, "_ID " + ID + "Copy to extSdCard FAILED");
						} finally {
							PendingIntent contentIntent = PendingIntent.getActivity(nContext, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			        		cBuilder.setContentIntent(contentIntent);
							cNotificationManager.notify(ID, cBuilder.build());
						}
					}
					
					if (audio == true) {
						Utils.logger("i", ">>>>>>>>>>> starting ffmpeg test...", DEBUG_TAG);
						FfmpegController ffmpeg = null;
					    try {
					    	ffmpeg = new FfmpegController(context);
					    } catch (IOException ioe) {
					    	Log.e(DEBUG_TAG, "Error loading ffmpeg. " + ioe.getMessage());
					    }

					    ShellDummy shell = new ShellDummy();
						List<String> cmd = new ArrayList<String>();
						
						//File in = new File(ShareActivity.dir_Downloads, filename);
						File in = new File(ShareActivity.path, vfilename);
						
						String afilename = settings.getString(vfilename + "FF", "audio");
						File out = new File(ShareActivity.path, afilename);
						
						//File in = new File("/storage/sdcard0/v.mp4");
						//File out = new File("/storage/sdcard0/a.aac");
						
						cmd.add(ffmpeg.mFfmpegBinPath);
						
						// -i /storage/sdcard0/v.mp4 -vn -acodec copy /storage/sdcard0/a.aac");
						cmd.add("-i");
						cmd.add(in.getPath());
						Utils.logger("i", "ffmpeg input: " + in, DEBUG_TAG);
						//cmd.add("-vn");
						cmd.add("-acodec");
						cmd.add("copy");
						cmd.add(out.getPath());
						Utils.logger("i", "ffmpeg output: " + out, DEBUG_TAG);
						
						ffmpeg.execProcess(cmd, shell);
					}
					
					break;
				case DownloadManager.STATUS_FAILED:
					Log.e(DEBUG_TAG, "_ID " + id + " FAILED (status " + status + ")");
					Log.e(DEBUG_TAG, " Reason: " + reason);
					Toast.makeText(context,  vfilename + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
					break;
					
				default:
					Utils.logger("w", "_ID " + id + " completed with status " + status, DEBUG_TAG);
				}
				
				if (settings.getBoolean("enable_own_notification", true) == true) {
					try {
						removeIdUpdateNotification(id);
					} catch (NullPointerException e) {
						Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
					}
				}
	        }
    	}
    };
    
    public static void removeIdUpdateNotification(long id) {
		if (id != 0) {
			if (ShareActivity.sequence.remove(id)) {
				Utils.logger("d", "_ID " + id + " REMOVED from Notification", DEBUG_TAG);
			} else {
				Utils.logger("d", "_ID " + id + " Already REMOVED from Notification", DEBUG_TAG);
			}
		} else {
			Log.e(DEBUG_TAG, "_ID  not found!");
		}
		
		if (ShareActivity.sequence.size() > 0) {
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			Utils.logger("i", "No downloads in progress; stopping FileObserver and DownloadsService", DEBUG_TAG);
			ShareActivity.fileObserver.stopWatching();
			nContext.stopService(new Intent(DownloadsService.getContext(), DownloadsService.class));
		}
	}
    
    private class ShellDummy implements ShellUtils.ShellCallback {

		@Override
		public void shellOut(String shellLine) {
			
			Log.d(DEBUG_TAG, "FFmpeg cmd message: " + shellLine);
		}

		@Override
		public void processComplete(int exitValue) {
			Log.e(DEBUG_TAG, "FFmpeg process exit value: " + exitValue);
		}
    	
    }
}
