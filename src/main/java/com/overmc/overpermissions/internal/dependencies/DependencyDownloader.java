package com.overmc.overpermissions.internal.dependencies;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.exceptions.StartException;

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
        this.libFolder = new File(baseFolder, "lib" + File.separator);
    }

    public boolean ensureDependencyExists(Dependency dependency) {
        boolean dependencyExists = true;
        for (Dependency subDependency : dependency.getSubDependencies()) {
            if (!ensureDependencyExists(subDependency)) {
                dependencyExists = false;
            }
        }
        if (!libFolder.exists()) {
            libFolder.mkdirs();
        }
        File requiredFile = new File(libFolder, dependency.getFileName());
        if (!requiredFile.exists()) {
            downloadFile(requiredFile, dependency.getUrl());
            dependencyExists = false;
        }
        return dependencyExists;
    }
    
    public void ensureDependencyExists(Dependency dependency, ArrayList<Dependency> missingDependencies) {
        for (Dependency subDependency : dependency.getSubDependencies()) {
            if (!ensureDependencyExists(subDependency)) {
                missingDependencies.add(subDependency);
            }
        }
        if (!libFolder.exists()) {
            libFolder.mkdirs();
        }
        File requiredFile = new File(libFolder, dependency.getFileName());
        if (!requiredFile.exists()) {
            downloadFile(requiredFile, dependency.getUrl());
            missingDependencies.add(dependency);
        }
    }

    private void downloadFile(File file, URL url) {
        try {
            file.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try (FileOutputStream fout = new FileOutputStream(file)) {
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoInput(true);
                con.setRequestMethod("GET");
                fout.getChannel().transferFrom(Channels.newChannel(con.getInputStream()), 0, Long.MAX_VALUE);
            } catch (Exception e) {
                file.delete(); //Delete the file if something goes wrong.
                throw e;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new StartException("Failed to download required dependency file \"" + url + "\" - " + e.getMessage());
        }
    }
}
