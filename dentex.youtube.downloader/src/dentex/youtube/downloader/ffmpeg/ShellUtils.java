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
