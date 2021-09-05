package com.github.eighty88.kurar.launcher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;

public class Downloader {
    java.net.URL URL;

    public Downloader(String URL) throws MalformedURLException {
        this.URL = new URL(URL);
    }

    public void DownloadTo(String Path) throws IOException {
        System.out.println("Download from " + URL.toString());
        System.out.print("Connection opening...");
        HttpURLConnection Connection =
                (HttpURLConnection) URL.openConnection();
        Connection.setAllowUserInteraction(false);
        Connection.setInstanceFollowRedirects(true);
        Connection.setRequestMethod("GET");
        Connection.connect();
        System.out.println(" Success!");

        DataInputStream InputStream = new DataInputStream(
                Connection.getInputStream());

        String FileName = Connection.getHeaderField("Content-Disposition");

        if(FileName == null || !FileName.contains("filename=\"")) {
            FileName = Paths.get(URL.getPath()).getFileName().toString();
        } else {
            FileName = FileName.substring(FileName.indexOf("filename=\"") + 10, FileName.length() - 1);
        }

        System.out.print("Downloading " + FileName + "...");

        File Download = new File(Path, FileName);

        if(Download.exists()) {
            System.out.println("\n" + FileName + " already exists");
            InputStream.close();
        } else {
            ReadableByteChannel ByteChannel = Channels.newChannel(InputStream);

            FileOutputStream OutputStream = new FileOutputStream(Download);

            try {
                OutputStream.getChannel().transferFrom(ByteChannel, 0, Long.MAX_VALUE);
            } finally {
                System.out.println("Success!");
                InputStream.close();
                OutputStream.close();
                ByteChannel.close();
            }
        }
    }
}
