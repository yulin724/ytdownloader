package dentex.youtube.downloader.ffmpeg;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import dentex.youtube.downloader.utils.Utils;

import android.content.Context;
import android.util.Log;

public class FfmpegController {

	private final static String DEBUG_TAG = "FfmpegController";
	public static final String[] LIB_ASSETS = {"ffmpeg", "liblame.so"};
	
	private File mBinFileDir;
	private File mLibFileDir;
	private Context mContext;
	public String mFfmpegBinPath;
	public String mLiblamePath;
	
	private final static String TAG = "FFMPEG";

	public FfmpegController(Context cxt) throws FileNotFoundException, IOException {
		mContext = cxt;
		mBinFileDir = mContext.getDir("bin", 0);
		mLibFileDir = mContext.getDir("lib", 0);

		if (!new File(mBinFileDir, LIB_ASSETS[0]).exists())  {
			BinaryInstaller bi = new BinaryInstaller(mContext, mBinFileDir);
			if (bi.installFromRaw()) {
				Log.v(TAG, "ffmpeg binary installed");
			} else {
				Log.e(TAG, "ffmpeg binary NOT installed");
			}
		}
		
		/*if (!new File(mBinFileDir, LIB_ASSETS[1]).exists())  {
			LibInstaller li = new LibInstaller(mContext, mLibFileDir);
			if (li.installFromRaw()) { 
				Log.v(TAG, "liblame library installed");
			} else {
				Log.e(TAG, "liblame library NOT installed");
			}
		}*/
		
		mFfmpegBinPath = new File(mBinFileDir, LIB_ASSETS[0]).getAbsolutePath();
		mLiblamePath = new File(mLibFileDir, LIB_ASSETS[1]).getAbsolutePath();
	}
	
	public  void execFFMPEG (List<String> cmd, ShellUtils.ShellCallback sc) {
		execChmod(mFfmpegBinPath, "755");
		//execChmod(mLiblamePath, "755");
		execProcess(cmd, sc);
	}
	
	public  void execChmod(String filepath, String code) {
		Log.d(TAG, "Trying to chmod '" + filepath + "' to: " + code);
		try {
			Runtime.getRuntime().exec("chmod " + code + " " + filepath);
		} catch (IOException e) {
			Log.e(TAG, "Error changing file permissions!", e);
		}
	}
	
	public  int execProcess(List<String> cmds, ShellUtils.ShellCallback sc) {		
		StringBuilder cmdlog = new StringBuilder();
		for (String cmd : cmds) {
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}
		Log.v(TAG, cmdlog.toString());
		
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(mBinFileDir);
		pb.command(cmds);
		
    	Process process = null;
    	int exitVal = 1; // Default error
    	try {
    		process = pb.start();    
    	
    		// any error message?
    		StreamGobbler errorGobbler = new 
    				StreamGobbler(process.getErrorStream(), "ERROR", sc);            
        
    		// any output?
    		StreamGobbler outputGobbler = new 
    				StreamGobbler(process.getInputStream(), "OUTPUT", sc);
            
    		// kick them off
    		errorGobbler.start();
    		outputGobbler.start();
     
    		exitVal = process.waitFor();
        
    		sc.processComplete(exitVal);
        
    	} catch (Exception e) {
    		Log.e(TAG, "Error executing ffmpeg command!", e);
    	} finally {
    		if (process != null) {
    			Utils.logger("w", "destroyng process", DEBUG_TAG);
    			process.destroy();
    		}
    	}
        return exitVal;
	}
	
	public void testCommands(String command, ShellUtils.ShellCallback sc) {
		String[] commands = command.split(" ");
		List<String> cmds = new ArrayList<String>(commands.length);
		for (String com : commands) {
			cmds.add(com);
		}
		/*try {
			System.load(mLiblamePath);
		} catch (Exception e) {
			Log.e(TAG, "Error loading library.", e);
		}*/
		try {
			execProcess(cmds, sc);
		} catch (Exception e) {
			Log.e(TAG, "Error running command.", e);
		}
	}
	
	public void extractAudio (File videoIn, File audioOut, 
			ShellUtils.ShellCallback sc) throws IOException, InterruptedException {
		
		List<String> cmd = new ArrayList<String>();

		cmd.add(mFfmpegBinPath);
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(videoIn.getAbsolutePath());

		/*if (mdesc.startTime != null) {
			cmd.add("-ss");
			cmd.add(mdesc.startTime);
		}
		
		if (mdesc.duration != null) {
			cmd.add("-t");
			cmd.add(mdesc.duration);
		}*/
		
		cmd.add("-vn");
		cmd.add("-acodec");
		cmd.add("copy");
		cmd.add(audioOut.getAbsolutePath());

		execFFMPEG(cmd, sc);
	}
	
	class FileMover {

		InputStream inputStream;
		File destination;
		
		public FileMover(InputStream _inputStream, File _destination) {
			inputStream = _inputStream;
			destination = _destination;
		}
		
		public void moveIt() throws IOException {
		
			OutputStream destinationOut = new BufferedOutputStream(new FileOutputStream(destination));
				
			int numRead;
			byte[] buf = new byte[1024];
			while ((numRead = inputStream.read(buf) ) >= 0) {
				destinationOut.write(buf, 0, numRead);
			}
			    
			destinationOut.flush();
			destinationOut.close();
		}
	}
	
	class StreamGobbler extends Thread {
	    InputStream is;
	    String type;
	    ShellUtils.ShellCallback sc;
	    
	    StreamGobbler(InputStream is, String type, ShellUtils.ShellCallback sc) {
	        this.is = is;
	        this.type = type;
	        this.sc = sc;
		}
	    
	    public void run() {
	    	try {
	    		InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line = null;
	            while ((line = br.readLine()) != null) {
	            	if (sc != null) {
	            		sc.shellOut(line);
	            	}
	            }
	        } catch (IOException ioe) {
	                Log.e(TAG,"error reading shell log", ioe);
	        }
	    }
	}
}