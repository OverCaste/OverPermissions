package com.overmc.overpermissions.internal.dependencies;

import java.net.MalformedURLException;
import java.net.URL;

public enum DefaultDependencies implements Dependency {
    JEDIS("jedis-2.4.2.jar", "http://search.maven.org/remotecontent?filepath=redis/clients/jedis/2.4.2/");

    private final URL url;
    private final String filename;

    DefaultDependencies(String url, String filename) {
        try {
            this.url = new URL(url + filename);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid url for dependency: " + url, e);
        }
        this.filename = filename;
    }

    @Override
    public URL getUrl( ) {
        return url;
    }

    @Override
    public String getFileName( ) {
        return filename;
    }
}