package de.maikmerten.bbbdownload.ui;

import de.maikmerten.bbbdownload.downloader.Downloader;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.SwingUtilities;

/**
 *
 * @author maik
 */
public class DownloadThread extends Thread {
    
    private DownloaderFrame frame;
    private String url;
    private String filename;
    private boolean skipChat;
    
    public DownloadThread(DownloaderFrame frame, String url, String filename, boolean skipChat) {
        this.frame = frame;
        this.url = url;
        this.filename = filename;
        this.skipChat = skipChat;
    }

    @Override
    public void run() {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(filename)));

            Downloader d = new Downloader(url, skipChat);
            d.downloadPresentation(zos);
            
            zos.close();

        } catch (Exception e) {

        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setIdle(true);
            }
        });
        
    }
}
