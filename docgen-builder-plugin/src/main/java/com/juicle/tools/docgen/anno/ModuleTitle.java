package com.juicle.tools.docgen.anno;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ModuleTitle {
    String value();
}
