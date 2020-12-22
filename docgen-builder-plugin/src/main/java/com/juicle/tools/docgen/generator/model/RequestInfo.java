package com.juicle.tools.docgen.generator.model;

import lombok.Data;

@Data
public class RequestInfo {
    private String name;
    private String required;
    private String type;
    private String intro;
    private String position;
}
