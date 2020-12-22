package com.juicle.tools.docgen.anno;

import java.lang.annotation.*;

@Repeatable(value = RequestDataSet.class)
public @interface RequestData {
    String name();
    String description();
    boolean required();
    String dataType();
    String position();

}
