package de.maikmerten.bbbdownload.downloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author maik
 */
public class Downloader {

    private byte[] shapes;
    private String baseURL;
    private String recId;
    private boolean skipChat;
    private boolean anonymizeChat;

    private String[] resources = {
        "captions.json",
        "cursor.xml",
        "deskshare.xml",
        "shapes.svg",
        "metadata.xml",
        "panzooms.xml",
        "presentation_text.json",
        "slides_new.xml",
        "audio/audio.ogg",
        "audio/audio.webm",
        "video/webcams.webm",
        "video/webcams.mp4",
        "presentation/deskshare.png",
        "deskshare/deskshare.webm",
        "deskshare/deskshare.mp4"
    };

    public Downloader(String playerURL, boolean skipChat, boolean anonymizeChat) {
        this.baseURL = getBaseURL(playerURL);
        this.recId = getRecordingId(playerURL);
        this.skipChat = skipChat;
        this.anonymizeChat = anonymizeChat;
    }

    private String getBaseURL(String playerURL) {
        int firstslash = playerURL.indexOf("/", 8);
        return playerURL.substring(0, firstslash);
    }

    private String getRecordingId(String playerURL) {
        Pattern recording_pattern = Pattern.compile("\\p{Alnum}{40}-\\d{13}");
        Matcher m = recording_pattern.matcher(playerURL);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    private void downloadSlides(ZipOutputStream zos) throws Exception {
        if (shapes == null) {
            return;
        }

        String shapestring = new String(shapes, "utf8");
        Set<String> slide_resources = new HashSet<>();

        // find all slide PNGs referenced in shapes.svg
        Pattern slides_pattern = Pattern.compile("presentation/\\p{Alnum}{40}-\\d{13}/slide-[0-9]+\\.png");
        Matcher m = slides_pattern.matcher(shapestring);
        while (m.find()) {
            String slide = m.group();
            slide_resources.add(slide);
        }

        // find all slide Texts referenced in shapes.svg
        Pattern texts_pattern = Pattern.compile("presentation/\\p{Alnum}{40}-\\d{13}/textfiles/slide-[0-9]+\\.txt");
        m = texts_pattern.matcher(shapestring);
        while (m.find()) {
            String text = m.group();
            slide_resources.add(text);
        }

        for (String resource : slide_resources) {
            downloadIntoZip(baseURL + "/presentation/" + recId + "/" + resource, resource, zos);
        }

    }

    private void downloadIntoZip(String urlstring, String zipfilename, ZipOutputStream zos) throws Exception {

        // we need shapes.svg to get location of slide PNGs, save contents
        // for later processing
        boolean isShapes = zipfilename.equals("shapes.svg");
        boolean isChat = zipfilename.equals("slides_new.xml");

        zipfilename = "presentation/" + recId + "/" + zipfilename;

        if (skipChat && isChat) {
            String popcornString = "<?xml version=\"1.0\"?>\n<popcorn>\n</popcorn>";
            zos.putNextEntry(new ZipEntry(zipfilename));
            zos.write(popcornString.getBytes("utf8"));
            return;
        }

        InputStream is = null;
        try {
            URL url = new URL(urlstring);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
        } catch (Exception e) {
            // could not download resource
            System.out.println("could not download " + zipfilename);
            return;
        }

        System.out.println("downloading " + zipfilename);

        if (isChat && anonymizeChat) {
            try {
                is = ChatAnonymizer.anonymizeChat(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ZipEntry ze = new ZipEntry(zipfilename);
        zos.putNextEntry(ze);

        ByteArrayOutputStream baos = null;
        if (isShapes) {
            baos = new ByteArrayOutputStream();
        }

        byte[] buf = new byte[2048];
        int read = is.read(buf);
        while (read > 0) {
            zos.write(buf, 0, read);
            if (baos != null) {
                baos.write(buf, 0, read);
            }
            read = is.read(buf);
        }

        if (baos != null) {
            shapes = baos.toByteArray();
            baos.close();
        }

    }

    public void addPlayer(String recId, ZipOutputStream zos) throws Exception {
        ZipInputStream zis = new ZipInputStream(Downloader.class.getResourceAsStream("/playback.zip"));

        byte[] buf = new byte[2048];

        ZipEntry zentry = zis.getNextEntry();
        while (zentry != null) {
            if (!zentry.isDirectory()) {
                ZipEntry newentry = new ZipEntry(zentry.getName());
                zos.putNextEntry(newentry);

                int read = zis.read(buf);
                while (read != -1) {
                    zos.write(buf, 0, read);
                    read = zis.read(buf);
                }
            }
            zentry = zis.getNextEntry();
        }

        String htmlcontent = "<html><meta http-equiv='refresh' content='0; URL=./playback/presentation/2.0/playback.html?meetingId=" + recId + "'></html>";
        zos.putNextEntry(new ZipEntry("index.html"));
        zos.write(htmlcontent.getBytes("utf8"));

    }

    public void downloadPresentation(ZipOutputStream zos) throws Exception {
        String presURL = baseURL + "/presentation/" + recId + "/";

        for (String res : resources) {
            try {
                downloadIntoZip(presURL + res, res, zos);
            } catch (Exception e) {
                System.out.println("could not download " + res);
            }
        }

        downloadSlides(zos);
        addPlayer(recId, zos);
    }

}
