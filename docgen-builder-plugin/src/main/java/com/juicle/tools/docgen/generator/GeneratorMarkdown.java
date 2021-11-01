package com.juicle.tools.docgen.generator;

import com.juicle.tools.docgen.generator.model.RequestInfo;
import com.juicle.tools.docgen.generator.model.ResponseInfo;
import lombok.Data;

import java.util.List;

@Data
public class GeneratorMarkdown {
    private String title;
    private String url;
    private String type;
    private String note;
    private List<RequestInfo> listRequestInfo;
    private List<ResponseInfo> listResponseInfo;

    public String getDocStr(){
        StringBuffer resut = new StringBuffer();

        //url
        resut.append("**请求URL：**");
        resut.append("\n");
        resut.append("- ` "+url+" `");
        resut.append("\n\n");

        //type
        resut.append("**请求方式：**");
        resut.append("\n");
        resut.append("- "+type);
        resut.append("\n\n");

        //note
        resut.append("**接口说明：**");
        resut.append("\n");
        resut.append("- "+note);
        resut.append("\n\n");

        //request data
        resut.append("**请求参数：**\n\n");
        resut.append("| 字段     | 是否必填   |   类型     | 说明        |位置       |\n");
        resut.append("|:---------|:----|:-----------|:-------------|:-----------|\n");
        for(RequestInfo requestInfo : listRequestInfo){
            resut.append("|"+requestInfo.getName());
            resut.append("|"+requestInfo.getRequired());
            resut.append("|"+requestInfo.getType());
            resut.append("|"+requestInfo.getIntro());
            resut.append("|"+requestInfo.getPosition()+"|\n");
        }
        resut.append("\n");

        //response data
        if(listResponseInfo != null){
            resut.append("**返回参数**\n\n");
            resut.append("| 字段               | 类型  | 说明                  |\n");
            resut.append("|:----------|:-------|:-----------------------|\n");
            for(ResponseInfo responseInfo : listResponseInfo){
                resut.append("|"+responseInfo.getName());
                resut.append("|"+responseInfo.getType());
                resut.append("|"+responseInfo.getNote()+"|\n");
            }
            resut.append("\n");
        }


        return resut.toString();
    }
}
