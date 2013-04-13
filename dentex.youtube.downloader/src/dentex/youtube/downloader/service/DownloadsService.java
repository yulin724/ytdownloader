package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.ffmpeg.ShellUtils.ShellCallback;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final static String DEBUG_TAG = "DownloadsService";
	public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	public boolean copy;
	public String audio;
	public static int ID;
	public static Context nContext;
	public String aSuffix = ".audio";
	public String vfilename;
	protected File out;
	protected String aBaseName;
	private NotificationManager cNotificationManager;
	private NotificationCompat.Builder cBuilder;
	protected String acodec;
	protected String aFileName;
	public boolean audioQualitySuffixEnabled;
	protected MediaScannerConnection scanner;
	public File copyDst;

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
		Utils.logger("d", "Copy to extSdcard: " + copy, DEBUG_TAG);
		
		audio = intent.getStringExtra("AUDIO");
		Utils.logger("d", "Audio extraction: " + audio, DEBUG_TAG);
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Utils.logger("d", "service destroyed", DEBUG_TAG);
	    unregisterReceiver(downloadComplete);
	}

	BroadcastReceiver downloadComplete = new BroadcastReceiver() {

		private Intent intent2;

		@Override
    	public void onReceive(Context context, Intent intent) {
    		Utils.logger("d", "downloadComplete: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		vfilename = settings.getString(String.valueOf(id), "video");
    		
			Query query = new Query();
			query.setFilterById(id);
			Cursor c = ShareActivity.dm.query(query);
			if (c.moveToFirst()) {
				
				int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
				int status = c.getInt(statusIndex);
				int reason = c.getInt(reasonIndex);
				
				//long size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

				switch (status) {
				
				case DownloadManager.STATUS_SUCCESSFUL:
					Utils.logger("d", "_ID " + id + " SUCCESSFUL (status " + status + ")", DEBUG_TAG);
					ID = (int) id;
					
					cBuilder =  new NotificationCompat.Builder(context);
					cNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					
			    	cBuilder.setSmallIcon(R.drawable.icon_nb);
					cBuilder.setContentTitle(vfilename);
					
					/*
					 *  Copy to extSdCard enabled
					 */
					if (copy == true) {
						File src = new File(ShareActivity.dir_Downloads, vfilename);
						final File dst = new File(ShareActivity.path, vfilename);
						copyDst = dst;
						
						if (settings.getBoolean("enable_own_notification", true) == true) {
							try {
								removeIdUpdateNotification(id);
							} catch (NullPointerException e) {
								Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
							}
						}
							
						intent2 = new Intent(Intent.ACTION_VIEW);

						try {
							// Toast + Notification + Log ::: Copy in progress...
							Toast.makeText(context,"YTD: " + context.getString(R.string.copy_progress), Toast.LENGTH_LONG).show();
					        cBuilder.setContentText(context.getString(R.string.copy_progress));
							cNotificationManager.notify(ID, cBuilder.build());
							Utils.logger("i", "_ID " + ID + " Copy in progress...", DEBUG_TAG);
							
							Utils.copyFile(src, dst);
							
							// Toast + Notification + Log ::: Copy OK
							Toast.makeText(context,  vfilename + ": " + context.getString(R.string.copy_ok), Toast.LENGTH_LONG).show();
					        cBuilder.setContentText(context.getString(R.string.copy_ok));
					        intent2.setDataAndType(Uri.fromFile(dst), "video/*");
							Utils.logger("i", "_ID " + ID + " Copy OK", DEBUG_TAG);
							
							if (!audio.equals("conv")) Utils.scanMedia(getApplicationContext(), new File[] {dst}, new String[] {"video/*"});
						                  
							if (ShareActivity.dm.remove(id) == 0) {
								Toast.makeText(context, "YTD: " + getString(R.string.download_remove_failed), Toast.LENGTH_LONG).show();
								Log.e(DEBUG_TAG, "temp download file NOT removed");
								
				        	} else { 
				        		Utils.logger("v", "temp download file removed", DEBUG_TAG);
				        		
				        		// TODO dm.addCompletedDownload to add the completed file on extSdCard into the dm list; NOT working
				        		//Uri dstUri = Uri.fromFile(dst); // <-- tried also this; see (1)

				        		/*Utils.logger("i", "dst: " + dst.getAbsolutePath(), DEBUG_TAG);
				        		ShareActivity.dm.addCompletedDownload(vfilename, 
				        				getString(R.string.ytd_video), 
				        				true, 
				        				"video/*", 
				        				dst.getAbsolutePath(), // <-- dstUri.getEncodedPath(), // (1) 
				        				size,
				        				false);*/
				        	}	        	
						} catch (IOException e) {
							// Toast + Notification + Log ::: Copy FAILED
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
					
					/*
					 *  Audio extraction enabled
					 */
					if (!audio.equals("none")) {
						File in = new File(ShareActivity.path, vfilename);
						
						acodec = settings.getString(vfilename + "FFext", ".audio");
						aBaseName = settings.getString(vfilename + "FFbase", ".audio");
						aFileName = aBaseName + acodec;
						out = new File(ShareActivity.path, aFileName);
						
						FfmpegController ffmpeg = null;

					    try {
					    	ffmpeg = new FfmpegController(context);
					    	
					    	// Toast + Notification + Log ::: Audio job in progress...
					    	String text = null;
					    	if (audio.equals("extr")) {
								text = getString(R.string.audio_extr_progress);
							} else {
								text = getString(R.string.audio_conv_progress);
							}
					    	Toast.makeText(context,"YTD: " + text, Toast.LENGTH_LONG).show();
					    	cBuilder.setContentTitle(aFileName);
					        cBuilder.setContentText(text);
							cNotificationManager.notify(ID*ID, cBuilder.build());
							Utils.logger("i", "_ID " + ID + " " + text, DEBUG_TAG);
					    } catch (IOException ioe) {
					    	Log.e(DEBUG_TAG, "Error loading ffmpeg. " + ioe.getMessage());
					    }
					    
					    ShellDummy shell = new ShellDummy();
					    
					    String mp3BitRate = settings.getString("mp3_bitrate", getString(R.string.mp3_bitrate_default));
					    
					    try {
							ffmpeg.extractAudio(in, out, audio, mp3BitRate, shell);
					    } catch (IOException e) {
							Log.e(DEBUG_TAG, "IOException running ffmpeg" + e.getMessage());
						} catch (InterruptedException e) {
							Log.e(DEBUG_TAG, "InterruptedException running ffmpeg" + e.getMessage());
						}
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
			Utils.logger("d", "No downloads in progress; stopping FileObserver and DownloadsService", DEBUG_TAG);
			ShareActivity.videoFileObserver.stopWatching();
			nContext.stopService(new Intent(DownloadsService.getContext(), DownloadsService.class));
		}
	}
    
    private class ShellDummy implements ShellCallback {

		@Override
		public void shellOut(String shellLine) {
			audioQualitySuffixEnabled = settings.getBoolean("enable_audio_q_suffix", true);
			findAudioSuffix(shellLine, audioQualitySuffixEnabled);
			Utils.logger("d", shellLine, DEBUG_TAG);
		}

		@Override
		public void processComplete(int exitValue) {
			Utils.logger("i", "FFmpeg process exit value: " + exitValue, DEBUG_TAG);
			String text = null;
			Intent audioIntent =  new Intent(Intent.ACTION_VIEW);
			if (exitValue == 0) {
				// Toast + Notification + Log ::: Audio job OK
				if (audio.equals("extr")) {
					text = getString(R.string.audio_extr_completed);
				} else {
					text = getString(R.string.audio_conv_completed);
				}
				Utils.logger("i", "_ID " + ID + " " + text, DEBUG_TAG);
				
				final File renamedAudioFilePath = renameAudioFile(aBaseName, out);
				Toast.makeText(nContext,  renamedAudioFilePath.getName() + ": " + text, Toast.LENGTH_LONG).show();
				cBuilder.setContentTitle(renamedAudioFilePath.getName());
				cBuilder.setContentText(text);			
				audioIntent.setDataAndType(Uri.fromFile(renamedAudioFilePath), "audio/*");
				PendingIntent contentIntent = PendingIntent.getActivity(nContext, 0, audioIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        		cBuilder.setContentIntent(contentIntent);
        		
        		if (audio.equals("conv")) {
					try {
						Utils.logger("d", "writing ID3 tags...", DEBUG_TAG);
						addId3Tags(renamedAudioFilePath);
					} catch (ID3WriteException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					} catch (IOException e) {
						Log.e(DEBUG_TAG, "Unable to write id3 tags", e);
					}
					if (copy == true) {
						Utils.scanMedia(getApplicationContext(), new File[] {copyDst, renamedAudioFilePath}, new String[] {"video/*", "audio/*"});
					} else {
						Utils.scanMedia(getApplicationContext(), new File[] {renamedAudioFilePath}, new String[] {"audio/*"});
					}
        		}
			} else {
				// Toast + Notification + Log ::: Audio job error
				if (audio.equals("extr")) {
					text = getString(R.string.audio_extr_error);
				} else {
					text = getString(R.string.audio_conv_error);
				}
				Log.e(DEBUG_TAG, "_ID " + ID + " " + text);
				Toast.makeText(nContext,  "YTD: " + text, Toast.LENGTH_LONG).show();
				cBuilder.setContentText(text);
			}
			cNotificationManager.notify(ID*ID, cBuilder.build());
		}
    }
    
	public File renameAudioFile(String aBaseName, File extractedAudioFile) {
		// Rename audio file to add a more detailed suffix, 
		// but only if it has been matched from the ffmpeg console output
		if (audio.equals("extr") && 
				audioQualitySuffixEnabled && 
				extractedAudioFile.exists() && 
				!aSuffix.equals(".audio")) {
			String newFileName = aBaseName + aSuffix;
			File newFileNamePath = new File(ShareActivity.path, newFileName);
			if (extractedAudioFile.renameTo(newFileNamePath)) {
				Utils.logger("i", extractedAudioFile.getName() + " renamed to: " + newFileName, DEBUG_TAG);
				return newFileNamePath;
			} else {
				Log.e(DEBUG_TAG, "Unable to rename " + extractedAudioFile.getName() + " to: " + aSuffix);
			}
		}
		return extractedAudioFile;
	}

	/* method addId3Tags adapted from Stack Overflow:
	 * 
	 * http://stackoverflow.com/questions/9707572/android-how-to-get-and-setchange-id3-tagmetadata-of-audio-files/9770646#9770646
	 * 
	 * Q: http://stackoverflow.com/users/849664/chirag-shah
	 * A: http://stackoverflow.com/users/903469/mkjparekh
	 */
	
	public void addId3Tags(File src) throws IOException, ID3WriteException {
        MusicMetadataSet src_set = new MyID3().read(src);
        if (src_set == null) {
            Log.w(DEBUG_TAG, "no metadata");
        } else {
	        MusicMetadata meta = new MusicMetadata("ytd");
	        meta.setAlbum("YTD Extracted Audio");
	        meta.setArtist("YTD");
	        meta.setSongTitle(aBaseName);
	        //String year = String.valueOf(Time.YEAR);
	        Calendar cal = Calendar.getInstance();
	        int year = cal.get(Calendar.YEAR);
	        meta.setYear(String.valueOf(year));
	        Log.i("TEST", String.valueOf(year));
	        new MyID3().update(src, src_set, meta);
        }
	}

	private void findAudioSuffix(String shellLine, boolean audioQualitySuffixEnabled) {
		if (audioQualitySuffixEnabled) {
			Pattern audioPattern = Pattern.compile("#0:0.*: Audio: (.+), .+?(mono|stereo .default.|stereo)(, .+ kb|)"); 
			Matcher audioMatcher = audioPattern.matcher(shellLine);
			if (audioMatcher.find() && audio.equals("extr")) {
				String oggBr = "a";
				String groupTwo = "n";
				if (audioMatcher.group(2).equals("stereo (default)")) {
					if (vfilename.contains("hd")) {
						oggBr = "192k";
					} else {
						oggBr = "128k";
					}
					groupTwo = "stereo";
				} else {
					oggBr = "";
					groupTwo = audioMatcher.group(2);
				}
				
				aSuffix = "_" +
						groupTwo + 
						"_" + 
						audioMatcher.group(3).replace(", ", "").replace(" kb", "k") + 
						oggBr + 
						"." +
						audioMatcher.group(1).replaceFirst(" (.*/.*)", "").replace("vorbis", "ogg");
				
				Utils.logger("i", "AudioSuffix: " + aSuffix, DEBUG_TAG);
			}
		}
	}
}
