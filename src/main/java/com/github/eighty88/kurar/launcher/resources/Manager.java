package com.github.eighty88.kurar.launcher.resources;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.eighty88.kurar.launcher.Utils;
import com.github.eighty88.kurar.launcher.resources.type.Asset;
import com.github.eighty88.kurar.launcher.resources.type.Library;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Manager {
    ArrayList<Library> libraries = new ArrayList<>();

    ArrayList<Asset> assets;

    JsonObject versionJson;

    JsonObject assetsJson;

    public Manager(String jsonUrl) {
        System.out.println("Loading Libraries and Assets...");
        this.versionJson = Utils.queryJson(jsonUrl);
        JsonObject assetsIndexJson = Objects.requireNonNull(this.versionJson).get("assetIndex").asObject();
        String assetsUrl = assetsIndexJson.getString("url", null);
        this.assetsJson = Utils.queryAssetsJson(assetsUrl);
        this.assets = new ArrayList<>();
        JsonObject assetsJson2 = Objects.requireNonNull(this.assetsJson).get("objects").asObject();
        for (String name : assetsJson2.names()) {
            JsonObject assetJson = assetsJson2.get(name).asObject();
            this.assets.add(new Asset(name, assetJson.getString("hash", null), assetJson.getInt("size", 0)));
        }
        parseLibraries(this.versionJson);
    }

    public void parseLibraries(JsonObject versionJson) {
        String osName = Utils.getPlatformName();
        String arch = System.getProperty("sun.arch.data.model");

        JsonArray librariesArrayJson = versionJson.get("libraries").asArray();
        for (JsonValue val : librariesArrayJson.values()) {
            JsonObject libraryJson = val.asObject();
            if (libraryJson.get("rules") != null) {
                boolean allowed = false;
                for (JsonValue ruleVal : libraryJson.get("rules").asArray().values()) {
                    JsonObject rule = ruleVal.asObject();
                    if (rule.get("os") != null &&
                            !osName.equals(rule.get("os").asObject().getString("name", null)))
                        continue;
                    allowed = "allow".equals(rule.getString("action", null));
                }
                if (!allowed)
                    continue;
            }
            JsonObject downloadsJson = libraryJson.get("downloads").asObject().get("artifact").asObject();
            if (downloadsJson.getString("path", null).contains("realms"))
                continue;
            this.libraries.add(new Library(downloadsJson.getString("path", null), downloadsJson.getString("sha1", null), downloadsJson.getInt("size", 0), downloadsJson.getString("url", null)));
            if (libraryJson.get("natives") != null) {
                String nativeName = libraryJson.get("natives").asObject().getString(osName, null);
                if (nativeName == null)
                    nativeName = libraryJson.get("natives").asObject().getString(osName + arch, null);
                if (nativeName != null) {
                    JsonObject classifierJson = libraryJson.get("downloads").asObject().get("classifiers").asObject();
                    JsonObject nativeJson = classifierJson.get(nativeName).asObject();
                    String path = nativeJson.getString("path", null);
                    if (nativeJson.getString("url", null).endsWith(".zip"))
                        path = path + "natives.zip";
                    this.libraries.add(new Library(path, nativeJson.getString("sha1", null), nativeJson.getInt("size", 0), nativeJson.getString("url", null)));
                }
            }
        }
    }

    public void download() throws IOException {
        for (Asset asset : this.assets) {
            File destination = new File(Utils.getWorkingDirectory(), "assets" + File.separator + "objects" + File.separator + asset.getHash().substring(0, 2) + File.separator + asset.getHash());
            if (destination.exists()) {
                System.out.println(destination);
                continue;
            }
            Utils.downloadFileFromUrl("https://resources.download.minecraft.net/" + asset.getHash().substring(0, 2) + "/" + asset.getHash(), destination);
        }
        for (Library library : this.libraries) {
            File destination = new File(Utils.getWorkingDirectory(), "libraries" + File.separator + library.getPath().replaceAll("//", File.separator));
            if (destination.exists() && Utils.getFileSha1Sum(destination).equals(library.getSha1())) {
                continue;
            }
            Utils.downloadFileFromUrl(library.getUrl(), destination);
            if (library.getUrl().endsWith(".zip")) {
                byte[] buff = new byte[4096];
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(destination))) {
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.isDirectory()) {
                            File file = new File(destination.getParent(), ze.getName() + File.separator);
                            file.mkdirs();
                            continue;
                        }
                        File dest = new File(destination.getParent(), ze.getName());
                        try (FileOutputStream fos = new FileOutputStream(dest)) {
                            int readed;
                            while ((readed = zis.read(buff)) > 0)
                                fos.write(buff, 0, readed);
                        }
                        zis.closeEntry();
                    }
                }
            }
        }
        if (!getAssetsJsonPath().getParentFile().exists())
            getAssetsJsonPath().getParentFile().mkdirs();
        Files.write(getAssetsJsonPath().toPath(), this.assetsJson.toString().getBytes());
    }

    public File getAssetsJsonPath() {
        return new File(Utils.getWorkingDirectory(), "assets" + File.separator + "indexes" + File.separator + Utils.getAssetsVersion() + ".json");
    }

    public ArrayList<Asset> getAssets() {
        return this.assets;
    }

    public ArrayList<Library> getLibraries() {
        return this.libraries;
    }
}
