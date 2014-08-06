package com.overmc.overpermissions.internal.dependencies;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

public enum DefaultDependencies implements Dependency {
    SLF4J_API("http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.5/", "slf4j-api-1.7.5.jar"),
    SLF4J_JDK14("http://central.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.5/", "slf4j-jdk14-1.7.5.jar"),
    JAVASSIST("http://central.maven.org/maven2/org/javassist/javassist/3.18.1-GA/", "javassist-3.18.1-GA.jar"),
    JEDIS("http://search.maven.org/remotecontent?filepath=redis/clients/jedis/2.4.2/", "jedis-2.4.2.jar"),
    HIKARI_CP("http://central.maven.org/maven2/com/zaxxer/HikariCP/2.0.1/", "HikariCP-2.0.1.jar") {
        @Override
        public Iterable<? extends Dependency> getSubDependencies( ) {
            return Arrays.asList(SLF4J_JDK14, SLF4J_API, JAVASSIST);
        }
    };

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

    @Override
    public Iterable<? extends Dependency> getSubDependencies( ) {
        return Collections.emptyList();
    }
}