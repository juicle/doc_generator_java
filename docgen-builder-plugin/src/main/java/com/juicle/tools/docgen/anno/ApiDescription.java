package com.juicle.tools.docgen.anno;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ApiDescription {
    String value();
    int index();
    String note();
}
