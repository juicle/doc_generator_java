package com.juicle.tools.docgen.classloader;

public interface ClassLoaderInterface {
    public Class<?> loadClass(String name) throws ClassNotFoundException;
}
