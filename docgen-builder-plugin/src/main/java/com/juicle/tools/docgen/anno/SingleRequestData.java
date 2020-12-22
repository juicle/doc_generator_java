package com.juicle.tools.docgen.anno;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface SingleRequestData {
    String name();
    String description();
    boolean required();
    String dataType();
    String position();
}
