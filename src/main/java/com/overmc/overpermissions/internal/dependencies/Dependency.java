package com.overmc.overpermissions.internal.dependencies;

import java.net.URL;

public interface Dependency {
    public URL getUrl( );

    public String getFileName( );
    
    public Iterable<? extends Dependency> getSubDependencies();
}