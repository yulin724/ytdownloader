package dentex.youtube.downloader.ffmpeg;

/* 
 * code adapted from BinaryInstaller.java:
 * Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian
 * See LICENSE for licensing information
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dentex.youtube.downloader.R;

import android.content.Context;
import android.util.Log;

public class BinaryInstaller  {

	
	private File mInstallFolder;
	private Context mContext;

    private static String CHMOD_EXEC = "700";
    
    private final static int FILE_WRITE_BUFFER_SIZE = 32256;
    
	public BinaryInstaller (Context context, File installFolder) {
		mContext = context;
		mInstallFolder = installFolder;
	}

	public boolean installFromRaw () throws IOException, FileNotFoundException {
		InputStream is;
        File outFile;
        
		is = mContext.getResources().openRawResource(R.raw.ffmpeg);
		outFile = new File(mInstallFolder, "ffmpeg");
		streamToFile(is, outFile, false, false, CHMOD_EXEC);
		return true;
	}
	
	/*
	 * Write the inputstream contents to the file
	 */
    @SuppressWarnings({ "resource", "unused" })
	private static boolean streamToFile(InputStream stm, File outFile, boolean append, 
            boolean zip, String mode) throws IOException {
        
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;
    	OutputStream stmOut = new FileOutputStream(outFile, append);
    	
    	if (zip) {
    		ZipInputStream zis = new ZipInputStream(stm);    		
    		ZipEntry ze = zis.getNextEntry();
    		stm = zis;
    		//zis.close();
    	}
    	
        while ((bytecount = stm.read(buffer)) > 0) {
            stmOut.write(buffer, 0, bytecount);
        }
        stmOut.close();
        stm.close();
        
		doChmod(outFile, mode);

        return true;
    }
	
    //copy the file from inputstream to File output - alternative impl
	public void copyFile (InputStream is, File outputFile) {
		try {
			outputFile.createNewFile();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
			DataInputStream in = new DataInputStream(is);
			
			int b = -1;
			byte[] data = new byte[1024];
			
			while ((b = in.read(data)) != -1) {
				out.write(data);
			}
			
			if (b == -1); //rejoice
			
			//
			out.flush();
			out.close();
			in.close();
			// chmod?
			
		} catch (IOException ex) {
			Log.e("ffmpeg", "Error copying binary", ex);
		}
	}
	
	public static Process doChmod(File file, String mode) throws IOException {
	    String filepath = file.getAbsolutePath();
	    return Runtime.getRuntime().exec("chmod " + mode + " " + filepath);
	}
}
