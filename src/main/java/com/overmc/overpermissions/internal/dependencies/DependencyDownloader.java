package com.overmc.overpermissions.internal.dependencies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.internal.StartException;

/**
 * A utility class to download dependencies for
 * 
 * @author <a href="http://www.reddit.com/user/TheOverCaste/">OverCaste</a>
 *
 */
public class DependencyDownloader {
    private final File libFolder;

    public DependencyDownloader(File baseFolder) {
        Preconditions.checkNotNull(baseFolder, "baseFolder");
        Preconditions.checkArgument(baseFolder.isDirectory(), "base folder must be directory!");
        this.libFolder = new File(baseFolder, "lib" + File.pathSeparator);
    }

    public void ensureDependencyExists(Dependency dependency) {
        if (!libFolder.exists()) {
            libFolder.mkdirs();
        }
        File requiredFile = new File(libFolder, dependency.getFileName());
        if (!requiredFile.exists()) {
            downloadFile(requiredFile, dependency.getUrl());
        }
    }

    private void downloadFile(File file, URL url) {
        try {
            file.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try (FileOutputStream fout = new FileOutputStream(file)) {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setRequestMethod("GET");
            fout.getChannel().transferFrom(Channels.newChannel(con.getInputStream()), 0, Long.MAX_VALUE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new StartException("Failed to download required file: " + url);
        }
    }
}
