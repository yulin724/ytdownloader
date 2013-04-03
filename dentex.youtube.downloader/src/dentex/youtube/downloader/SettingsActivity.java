package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import dentex.youtube.downloader.service.AutoUpgradeApkService;
import dentex.youtube.downloader.service.FfmpegDownloadService;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class SettingsActivity extends Activity {
	
	public static final String DEBUG_TAG = "SettingsActivity";
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_settings);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
        	case R.id.menu_donate:
        		startActivity(new Intent(this, DonateActivity.class));
        		return true;
        	case R.id.menu_about:
        		startActivity(new Intent(this, AboutActivity.class));
        		return true;
        	case R.id.menu_dm:
        		startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
		//private Preference dm;
		private Preference filechooser;
		private Preference quickStart;
		private Preference up;
		private CheckBoxPreference ownNot;
		private Preference th;
		private Preference lang;
		private CheckBoxPreference audio;
		
		protected int cpuVers;
		public static String link;
		
		public static final int YTD_SIG_HASH = -1892118308; // final string
		//public static final int YTD_SIG_HASH = -118685648; // dev test: desktop
		//public static final int YTD_SIG_HASH = 1922021506; // dev test: laptop
		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            String cf = settings.getString("CHOOSER_FOLDER", "");
            if (cf.isEmpty()) {
            	chooserSummary = getString(R.string.chooser_location_summary);
            } else {
            	chooserSummary = settings.getString("CHOOSER_FOLDER", "");
            }
            initSwapPreference();
            initSizePreference();
            initBitratePreference();
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }

            /*dm = (Preference) findPreference("dm");
            dm.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
                    return true;
                }
            });*/
            
            filechooser = (Preference) getPreferenceScreen().findPreference("open_chooser");
            filechooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  FileChooserActivity.class);
            		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile(Environment.getExternalStorageDirectory()));
            		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
            		startActivityForResult(intent, _ReqChooseFile);
                    return true;
                }
            });
            
            quickStart = (Preference) getPreferenceScreen().findPreference("quick_start");
            quickStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					PopUps.showPopUp(getString(R.string.quick_start_title), getString(R.string.quick_start_text), "info", getActivity());
					return true;
				}
			});
            
            up = (Preference) findPreference("update");
            up.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
		            Intent intent = new Intent(getActivity(),  UpgradeApkActivity.class);
		            startActivity(intent);
		            return true;
                }
            });
            
            initUpdate();
            
            ownNot = (CheckBoxPreference) findPreference("enable_own_notification");
            ownNot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	if (!ownNot.isChecked() && ShareActivity.mId == 1) {
                		ShareActivity.mNotificationManager.cancelAll();
                		ShareActivity.mId = 0;
                	}
					return true;
                }
            });
            
            th = (Preference) findPreference("choose_theme");
			th.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String theme = settings.getString("choose_theme", "D");
			    	if (theme.equals("D")) {
			    		getActivity().setTheme(R.style.AppThemeDark);
			    	} else {
			    		getActivity().setTheme(R.style.AppThemeLight);
			    	}
			    	
			    	if (!theme.equals(newValue)) reload();
					return true;
				}
			});
			
			lang = (Preference) findPreference("lang");
			lang.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String language = settings.getString("lang", "default");
					if (!language.equals(newValue)) reload();
					return true;
				}
			});
 
			audio = (CheckBoxPreference) findPreference("enable_audio_extraction");
			audio.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean audioExtrEnabled = settings.getBoolean("enable_audio_extraction", false);
					if (!audioExtrEnabled) {
						cpuVers = armCpuVersion();
						boolean isCpuSupported = (cpuVers > 0) ? true : false;
						Utils.logger("d", "isCpuSupported: " + isCpuSupported, DEBUG_TAG);
						if (!isCpuSupported) {
							audio.setChecked(false);
							audio.setEnabled(false);
							PopUps.showPopUp("alert", "CPU not yet supported", "alert", getActivity()); //TODO strings 
							// TODO ...or make dialog to send mail to developer?
						}
						
						File binDir = getActivity().getDir("bin", 0);
						boolean ffmpegInstalled = new File(binDir, "ffmpeg").exists();
						Utils.logger("d", "ffmpegInstalled: " + ffmpegInstalled, DEBUG_TAG);
					
						if (!ffmpegInstalled && isCpuSupported) {
							AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
	                        adb.setIcon(android.R.drawable.ic_dialog_info);
	                        adb.setTitle(getString(R.string.ffmpeg_download_dialog_title));
	                        
	                        link = getString(R.string.ffmpeg_download_dialog_msg_link, cpuVers);
	                        String msg = getString(R.string.ffmpeg_download_dialog_msg);
	                        String size = getString(R.string.size) + " 6.6 MB";
	                        
	                        adb.setMessage(msg + " " + link + "\n" + size);
	                        
	                        adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	Intent intent = new Intent(getActivity(), FfmpegDownloadService.class);
	                            	intent.putExtra("CPU", cpuVers);
	                            	getActivity().startService(intent);
	                            }
	                        });
	
	                        adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	// cancel and disable audio pref
	                            	audio.setChecked(false);
	                            }
	                        });
	
	                        AlertDialog helpDialog = adb.create();
	                        if (! (getActivity()).isFinishing()) { //TODO check
	                        	helpDialog.show();
	                        }
						}
					}
					return true;
				}
			});

		}
        
        private int armCpuVersion() {
			// TODO Auto-generated method stub
        	String cpuAbi = Build.CPU_ABI;
			Utils.logger("d", "CPU_ABI: " + cpuAbi, DEBUG_TAG);
			if (cpuAbi.equals("armeabi-v7a")) {
				return 7;
			} else if (cpuAbi.equals("armeabi")) {
				return 5;
			} else {
				return 0;
			}
		}

		public void reload() {
        	Intent intent = getActivity().getIntent();
        	intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    		getActivity().finish();
    		getActivity().overridePendingTransition(0, 0);
    		startActivity(intent);
    		getActivity().overridePendingTransition(0, 0);
        }

		public void initUpdate() {
			int prefSig = settings.getInt("APP_SIGNATURE", 0);
			Utils.logger("d", "prefSig: " + prefSig, DEBUG_TAG);
			
			if (prefSig == 0 ) {
				if (Utils.getSigHash(SettingsFragment.this) == YTD_SIG_HASH) {
					Utils.logger("d", "Found YTD signature: update check possile", DEBUG_TAG);
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
						autoUpdate(getActivity());
					}
		    	} else {
		    		Utils.logger("d", "Found different signature: " + Utils.currentHashCode + " (F-Droid?). Update check cancelled.", DEBUG_TAG);
		    		up.setEnabled(false);
		    		up.setSummary(R.string.update_disabled_summary);
		    	}
				SharedPreferences.Editor editor = settings.edit();
		    	editor.putInt("APP_SIGNATURE", Utils.currentHashCode);
		    	if (editor.commit()) Utils.logger("d", "saving sig pref...", DEBUG_TAG);
			} else {
				if (prefSig == YTD_SIG_HASH) {
					Utils.logger("d", "YTD signature in PREFS: update check possile", DEBUG_TAG);
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
						autoUpdate(getActivity());
					}
				} else {
					Utils.logger("d", "diffrent YTD signature in prefs (F-Droid?). Update check cancelled.", DEBUG_TAG);
					up.setEnabled(false);
				}
			}
		}

		private void initSwapPreference() {
			boolean swap = settings.getBoolean("swap_location", false);
			PreferenceScreen p = (PreferenceScreen) findPreference("open_chooser");
            if (swap == true) {
            	p.setEnabled(true);
            } else {
            	p.setEnabled(false);
            }
		}
		
		private void initSizePreference() {
			CheckBoxPreference s = (CheckBoxPreference) findPreference("show_size");
			CheckBoxPreference l = (CheckBoxPreference) findPreference("show_size_list");
            if (l.isChecked()) {
            	s.setEnabled(false);
            	s.setChecked(true);
            } else {
            	s.setEnabled(true);
            }
		}
		
		private void initBitratePreference() {
			String encode = settings.getString("audio_extraction_type", "strip");
			Preference p = (Preference) findPreference("mp3_bitrate");
				if (encode.equals("encode") == true) {
					p.setEnabled(true);
				} else {
					p.setEnabled(false);
				}
		}
        
		/*@Override
	    public void onStart() {
	        super.onStart();
	        Utils.logger("v", "_onStart");
	    }*/
	    
        @Override
        public void onResume(){
        	super.onResume();
        	// Set up a listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        	Utils.logger("v", "_onResume", DEBUG_TAG);
        }
       
        @Override
        public void onPause() {
        	super.onPause();
        	// Unregister the listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        	Utils.logger("v", "_onPause", DEBUG_TAG);
        }
        
        /*@Override
        public void onStop() {
            super.onStop();
        	Utils.logger("v", "_onStop");
        }*/
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	updatePrefSummary(findPreference(key));
        	initSwapPreference();
        	initSizePreference();
        	initBitratePreference();
        }

		private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory)p;
        		for(int i=0;i<pCat.getPreferenceCount();i++){
        			initSummary(pCat.getPreference(i));
        	    }
        	}else{
        		updatePrefSummary(p);
        	}
        }
        
        private void updatePrefSummary(Preference p){
        	if (p instanceof ListPreference) {
        		ListPreference listPref = (ListPreference) p;
        	    p.setSummary(listPref.getEntry());
        	}
        	if (p instanceof PreferenceScreen && p.getKey().equals("open_chooser")) {
        		p.setSummary(chooserSummary);
        	}
        }

        @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
            case _ReqChooseFile:
                if (resultCode == RESULT_OK) {
                    @SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
                    	
                	File chooserFolder = files.get(0);
					chooserSummary = chooserFolder.toString();
                	Utils.logger("d", "file-chooser selection: " + chooserSummary, DEBUG_TAG);
                	
                	switch (pathCheck(chooserFolder)) {
                		case 0:
                			// Path on standard sdcard
                			setChooserPrefAndSummary();
	                		break;
                		case 1:
                			// Path not writable
                			chooserSummary = ShareActivity.dir_Downloads.getAbsolutePath();
                			setChooserPrefAndSummary();
                			PopUps.showPopUp(getString(R.string.system_warning_title), getString(R.string.system_warning_msg), "alert", getActivity());
                			//Toast.makeText(fragContext, getString(R.string.system_warning), Toast.LENGTH_SHORT).show();
                			break;
                		case 2:
                			// Path not mounted
                			Toast.makeText(getActivity(), getString(R.string.sdcard_unmounted_warning), Toast.LENGTH_SHORT).show();
                	}
                }
                break;
            }
        }

		public void setChooserPrefAndSummary() {
			for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
				initSummary(getPreferenceScreen().getPreference(i));
			}
			settings.edit().putString("CHOOSER_FOLDER", chooserSummary).apply();
		}
        
        public int pathCheck(File path) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
            	if (path.canWrite()) {
					return 0;
				} else {
					Utils.logger("w", "Path not writable", DEBUG_TAG);
					return 1;
				}
            } else {
            	Utils.logger("w", "Path not mounted", DEBUG_TAG);
            	return 2;
            }
        }
        
        public static void autoUpdate(Context context) {
	        long storedTime = settings.getLong("time", 0); // final string
	        //long storedTime = 10000; // dev test: forces auto update
	        
	        boolean shouldCheckForUpdate = !DateUtils.isToday(storedTime);
	        Utils.logger("i", "shouldCheckForUpdate: " + shouldCheckForUpdate, DEBUG_TAG);
	        if (shouldCheckForUpdate) {
	        	Intent intent = new Intent(context, AutoUpgradeApkService.class);
	        	context.startService(intent);
	        }
	        
	        long time = System.currentTimeMillis();
	        if (settings.edit().putLong("time", time).commit()) Utils.logger("i", "time written in prefs", DEBUG_TAG);
		}
	}
}
