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
    
    private final DownloaderFrame frame;
    private final String url;
    private final String filename;
    private final boolean skipChat;
    private final boolean anonymizeChat;
    
    public DownloadThread(DownloaderFrame frame, String url, String filename, boolean skipChat, boolean anonymizeChat) {
        this.frame = frame;
        this.url = url;
        this.filename = filename;
        this.skipChat = skipChat;
        this.anonymizeChat = anonymizeChat;
    }

    @Override
    public void run() {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(filename)));

            Downloader d = new Downloader(url, skipChat, anonymizeChat);
            d.downloadPresentation(zos);
            
            zos.close();
        } catch (Exception e) {
            // sing and dance, ignore error
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setIdle(true);
            }
        });
        
    }
}
