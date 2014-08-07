package com.overmc.overpermissions.exceptions;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.overmc.overpermissions.internal.dependencies.Dependency;

public class MissingDependencyException extends StartException {
    private static final long serialVersionUID = 8378319692981341521L;

    public MissingDependencyException(String dependencyName) {
        super("A required dependency wasn't loaded: " + dependencyName + " It has been downloaded, but the server must restart.");
    }

    public MissingDependencyException(Iterable<Dependency> missingDependencies) {
        super("Required dependencies weren't loaded: ("
                + Joiner.on(", ").join(Iterables.transform(missingDependencies, new Function<Dependency, String>() {
                    @Override
                    public String apply(Dependency dep) {
                        return dep.getFileName();
                    }
                }))
                + ") They have been downloaded, but the server must restart.");
    }
}
