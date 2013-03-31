package dentex.youtube.downloader.ffmpeg;

/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import android.util.Log;

import dentex.youtube.downloader.utils.Utils;

@SuppressWarnings("unused")
public class ShellUtils {

	private final static String DEBUG_TAG = "ShellUtils";
	
	//various console cmds
	public final static String SHELL_CMD_CHMOD = "chmod";
	public final static String SHELL_CMD_KILL = "kill -9";
	public final static String SHELL_CMD_RM = "rm";
	public final static String SHELL_CMD_PS = "ps";
	public final static String SHELL_CMD_PIDOF = "pidof";

	public final static String CHMOD_EXE_VALUE = "700";
	
	/*public static boolean isRootPossible() {
		try {
			// Check if Superuser.apk exists
			File fileSU = new File("/system/app/Superuser.apk");
			if (fileSU.exists())
				return true;
			
			fileSU = new File("/system/bin/su");
			if (fileSU.exists())
				return true;
			
			//Check for 'su' binary 
			String[] cmd = {"which su"};
			int exitCode = ShellUtils.doShellCommand(null,cmd, new ShellCallback () {

				@Override
				public void shellOut(String msg) {
					
					//System.out.print(msg);
					
				}

				@Override
				public void processComplete(int exitValue) {
					// TODO Auto-generated method stub
				}
				
			}, false, true).exitValue();
			
			if (exitCode == 0) {
				Utils.logger("d", "Can acquire root permissions", DEBUG_TAG);
		    	 return true;
		     
		    }
		      
		} catch (IOException e) {
			//this means that there is no root to be had (normally) so we won't log anything
			Log.e(DEBUG_TAG, "Error checking for root access",e);
			
		}
		catch (Exception e) {
			Log.e(DEBUG_TAG, "Error checking for root access",e);
			//this means that there is no root to be had (normally)
		}
		
		Utils.logger("d", "Could not acquire root permissions", DEBUG_TAG);
		
		
		return false;
	}
	
	
	public static int findProcessId(String command) {
		int procId = -1;
		
		try {
			procId = findProcessIdWithPidOf(command);
			
			if (procId == -1)
				procId = findProcessIdWithPS(command);
		} catch (Exception e) {
			try {
				procId = findProcessIdWithPS(command);
			} catch (Exception e2) {
				Log.e(DEBUG_TAG, "Unable to get proc id for: " + command,e2);
			}
		}
		
		return procId;
	}
	
	//use 'pidof' command
	public static int findProcessIdWithPidOf(String command) throws Exception {
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
		String baseName = new File(command).getName();
		//fix contributed my mikos on 2010.12.10
		procPs = r.exec(new String[] {SHELL_CMD_PIDOF, baseName});
        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;

        while ((line = reader.readLine()) != null) {
            try {
                //this line should just be the process id
            	procId = Integer.parseInt(line.trim());
            	break;
            } catch (NumberFormatException e) {
            	Log.e(DEBUG_TAG, "unable to parse process pid: " + line,e);
            }
        }
        
        return procId;
	}
	
	//use 'ps' command
	public static int findProcessIdWithPS(String command) throws Exception {
		
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
        procPs = r.exec(SHELL_CMD_PS);
            
        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;
        
        while ((line = reader.readLine())!=null) {
        	if (line.indexOf(' ' + command)!=-1) {
        		StringTokenizer st = new StringTokenizer(line," ");
        		st.nextToken(); //proc owner
        		
        		procId = Integer.parseInt(st.nextToken().trim());
        		
        		break;
        	}
        }
        return procId;
	}*/
	
	public static int doShellCommand(String[] cmds, ShellCallback sc, 
			boolean runAsRoot, boolean waitFor) throws Exception {
		return doShellCommand (null, cmds, sc, runAsRoot, waitFor).exitValue();
	}
	
	public static Process doShellCommand(Process proc, String[] cmds, 
	        ShellCallback sc, boolean runAsRoot, boolean waitFor) 
	        throws Exception {
		
		if (proc == null) {
	    	if (runAsRoot) {
	    		proc = Runtime.getRuntime().exec("su");
	    	} else {
	    		proc = Runtime.getRuntime().exec("sh");
	    	}
		}	
    	
    	OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
        
        for (int i = 0; i < cmds.length; i++) {
        	Utils.logger("d", "executing shell cmd: " + cmds[i] + "; runAsRoot=" + 
        	        runAsRoot + ";waitFor=" + waitFor, DEBUG_TAG);
    		
        	out.write(cmds[i]);
        	out.write("\n");
        }
        
        out.flush();
		out.write("exit\n");
		out.flush();
	
		if (waitFor) {			
			final char buf[] = new char[20];
			
			// Consume the "stdout"
			InputStreamReader reader = new InputStreamReader(proc.getInputStream());
			while ((reader.read(buf)) != -1) {
				if (sc != null) {
				    sc.shellOut(new String(buf));
				}
			}
			
			// Consume the "stderr"
			reader = new InputStreamReader(proc.getErrorStream());
			while ((reader.read(buf)) != -1) {
				if (sc != null) {
				    sc.shellOut(new String(buf));
				}
			}
			proc.waitFor();
		}
		sc.processComplete(proc.exitValue());
        
        return proc;

	}
	
	public interface ShellCallback {
		public void shellOut (String shellLine);
		public void processComplete (int exitValue);
	}
}
