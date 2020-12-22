package com.juicle.tools.docgen.finder;

import com.juicle.tools.docgen.BuilderMojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassFinder {
    public static List<Class<?>> findAllClass(BuilderMojo builderMojo, List<File> files){
        List<Class<?>> result = new ArrayList<>();
        for (File file : files) {
            String className = file.getAbsolutePath().replace(builderMojo.getBasedir().getAbsolutePath(),"")
                    .replace(builderMojo.getScanSource(), "").replace(".java", "")
                    .replaceAll(File.separator+File.separator, ".");
            try {
                Class<?> clazz = builderMojo.getClassLoaderInterface().loadClass(className);
                result.add(clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("没有找到类:" + className);
            }
        }
        return result;
    }
}
