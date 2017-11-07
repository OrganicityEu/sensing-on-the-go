package eu.organicity.set.app.operations;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {
    private static final String TAG = "Downloader";

    public Downloader() {
        //
    }

    public void DownloadFromUrl(final String DownloadUrl, final String fileName) throws Exception {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();

            File dir = new File(root.getAbsolutePath() + "/dynamix");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            Log.i(TAG, "DownloadUrl:" + DownloadUrl);
            Log.i(TAG, "fileName:" + fileName);
            URL url = new URL(DownloadUrl); //you can write here any link
            File file = new File(dir, fileName);

            long startTime = System.currentTimeMillis();

		    /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();

		    /*
             * Define InputStreams to read from the URLConnection.
		     */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

		    /*
             * Read bytes to the Buffer until there is nothing more to read(-1).
		     */
            ByteArrayOutputStream baf = new ByteArrayOutputStream(5000);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.write((byte) current);
            }

		    /* Convert the Bytes read to a String. */
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
