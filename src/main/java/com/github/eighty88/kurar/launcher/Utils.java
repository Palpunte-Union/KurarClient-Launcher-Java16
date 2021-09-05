package com.github.eighty88.kurar.launcher;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class Utils {
    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String queryUrl(String url) {
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(url));
            try (BufferedReader br = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonObject queryJson(String url) {
        String query = queryUrl(url);
        return (query == null) ? null : Json.parse(query.replace("\"size\": ,", "")).asObject();
    }

    public static JsonObject queryAssetsJson(String url) {
        String query = queryAssetsUrl(url);
        return (query == null) ? null : Json.parse(query.replace("\"size\": ,", "")).asObject();
    }

    public static String queryAssetsUrl(String url) {
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection)(new URL(url)).openConnection();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileSha1Sum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream fis = new FileInputStream(file)) {
                int n = 0;
                byte[] buffer = new byte[4096];
                while (n != -1) {
                    n = fis.read(buffer);
                    if (n > 0)
                        digest.update(buffer, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0xF];
        }
        return new String(hexChars);
    }

    public static String getPlatformName() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac") || osName.startsWith("Darwin"))
            return "osx";
        if (osName.toLowerCase().contains("windows"))
            return "windows";
        return "linux";
    }

    public static void downloadFileFromUrl(String url, File file) {
        if (file.getParentFile() != null && !file.getParentFile().exists())
            file.getParentFile().mkdirs();
        try {
            HttpURLConnection con = (HttpURLConnection)(new URL(url)).openConnection();
            try(InputStream is = con.getInputStream(); FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buff = new byte[8192];
                int readedLen;
                while ((readedLen = is.read(buff)) > -1) {
                    fos.write(buff, 0, readedLen);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getWorkingDirectory() {
        String applicationData, folder, userHome = System.getProperty("user.home", ".");
        switch (getPlatformName()) {
            case "linux":
                return new File(userHome, ".minecraft/");
            case "windows":
                applicationData = System.getenv("APPDATA");
                folder = (applicationData != null) ? applicationData : userHome;
                return new File(folder, ".minecraft/");
            case "osx":
                return new File(userHome, "Library/Application Support/minecraft");
        }
        return new File(userHome, "minecraft/");
    }

    public static File getKurarDirectory() {
        return new File(getWorkingDirectory(), "Kurar16");
    }

    public static String getAssetsVersion() {
        return "1.17";
    }

    public static boolean checkUpdate() {
        try {
            int version = readURL();
            File file = new File(getKurarDirectory(), "latest.json");
            if (!file.exists())
                return true;
            return Json.parse(new InputStreamReader(new FileInputStream(file))).asObject().getInt("version", 1) != version;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int readURL() throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL("https://pa-union.f5.si/KurarClient-Binary-Java16/latest.json");
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder builder = new StringBuilder();
            char[] chars = new char[1024];
            int read;
            while ((read = reader.read(chars)) != -1)
                builder.append(chars, 0, read);
            return Json.parse(builder.toString()).asObject().getInt("version", 1);
        } catch (Exception e) {
            URL url = new URL("http://pa-union.f5.si/KurarClient-Binary-Java16/latest.json");
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder builder = new StringBuilder();
            char[] chars = new char[1024];
            int read;
            while ((read = reader.read(chars)) != -1)
                builder.append(chars, 0, read);
            return Json.parse(builder.toString()).asObject().getInt("version", 1);
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public void downloadJson() throws Exception {
        FileOutputStream outputStream = null;
        ReadableByteChannel byteChannel = null;
        try {
            if ((new File(getKurarDirectory(), "latest.json")).exists())
                (new File(getKurarDirectory(), "latest.json")).delete();
            URL url = new URL("https://pa-union.f5.si/KurarClient-Binary-Java16/latest.json");
            byteChannel = Channels.newChannel(url.openStream());
            outputStream = new FileOutputStream(new File(getKurarDirectory(), "latest.json"));
            outputStream.getChannel().transferFrom(byteChannel, 0L, Long.MAX_VALUE);
        } catch (Exception e) {
            if ((new File(getKurarDirectory(), "latest.json")).exists())
                (new File(getKurarDirectory(), "latest.json")).delete();
            URL url = new URL("http://pa-union.f5.si/KurarClient-Binary-Java16/latest.json");
            byteChannel = Channels.newChannel(url.openStream());
            outputStream = new FileOutputStream(new File(getKurarDirectory(), "latest.json"));
            outputStream.getChannel().transferFrom(byteChannel, 0L, Long.MAX_VALUE);
        } finally {
            System.out.println("Success!");
            Objects.requireNonNull(outputStream).close();
            byteChannel.close();
        }
    }

    public void downloadVersionJson() throws Exception {
        FileOutputStream outputStream = null;
        ReadableByteChannel byteChannel = null;
        try {
            if ((new File(getKurarDirectory(), "KurarClient.json")).exists())
                (new File(getKurarDirectory(), "KurarClient.json")).delete();
            URL url = new URL("https://github.com/Palpunte-Union/KurarClient-Binary-java16/releases/download/" + readURL() + "/KurarClient.json");
            byteChannel = Channels.newChannel(url.openStream());
            outputStream = new FileOutputStream(new File(getKurarDirectory(), "KurarClient.json"));
            outputStream.getChannel().transferFrom(byteChannel, 0L, Long.MAX_VALUE);
        } finally {
            System.out.println("Success!");
            Objects.requireNonNull(outputStream).close();
            byteChannel.close();
        }
    }
}
