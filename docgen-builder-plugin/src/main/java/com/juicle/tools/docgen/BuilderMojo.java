package com.juicle.tools.docgen;

import com.alibaba.fastjson.JSONObject;
import com.juicle.tools.docgen.anno.*;
import com.juicle.tools.docgen.classloader.ClassLoaderInterface;
import com.juicle.tools.docgen.classloader.MavenPluginContextClassLoader;
import com.juicle.tools.docgen.finder.ClassFinder;
import com.juicle.tools.docgen.finder.FileFinder;
import com.juicle.tools.docgen.generator.GeneratorMarkdown;
import com.juicle.tools.docgen.generator.model.RequestInfo;
import com.juicle.tools.docgen.generator.model.ResponseInfo;
import com.juicle.tools.docgen.util.HttpsUtil;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.RequestMapping;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class BuilderMojo extends AbstractMojo {


    private File basedir;
    private String packages;
    private String requestUrl;
    private String key;
    private String token;
    private MavenProject project;
    private ClassLoaderInterface classLoaderInterface;
    private String scanSource = File.separator + "src"+ File.separator +"main"+ File.separator +"java" + File.separator;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String error = "";
        try{
            GeneratorMarkdown apiDoc = new GeneratorMarkdown();
            //初始化classesLoader
            initClassesLoader();
            //递归找出需要扫描的file
            List<File> files = FileFinder.findAllFileNeedToParse(this);
            List<Class<?>> clazzs = ClassFinder.findAllClass(this, files);

            if (clazzs != null) {
                int count = 0;
                for (Class<?> clazz : clazzs) {
                    String catName = "";
                    String apiUrl = "";

                    //get the cat name
                    if(clazz.isAnnotationPresent(ModuleTitle.class)){
                        ModuleTitle moduleTitle = clazz.getAnnotation(ModuleTitle.class);
                        catName = moduleTitle.value();

                        //get the requestMapping value
                        if(clazz.isAnnotationPresent(RequestMapping.class)){
                            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                            apiUrl = requestMapping.value()[0];
                        }
                        //get method properties

                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            List<RequestInfo> listRequestInfo = new ArrayList<RequestInfo>();
                            List<ResponseInfo> listResponseInfo = new ArrayList<ResponseInfo>();
                            int index = 0;

                            if(method.isAnnotationPresent(ApiDescription.class)){
                                ApiDescription apiDescription = method.getAnnotation(ApiDescription.class);
                                apiDoc.setTitle(apiDescription.value());
                                apiDoc.setNote(apiDescription.note());
                                index = apiDescription.index();
                                error = apiDescription.value()+apiDescription.note();
                            }else{
                                break;
                            }

                            if(method.isAnnotationPresent(RequestMapping.class)){
                                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                                apiDoc.setUrl(apiUrl + requestMapping.value()[0]);
                                String type = "";
                                switch (requestMapping.method()[0]){
                                    case GET:
                                        type = "Get";
                                        break;
                                    case POST:
                                        type = "Post";
                                        break;
                                    case DELETE:
                                        type = "Delete";
                                        break;
                                    case PATCH:
                                        type = "Patch";
                                        break;
                                    case PUT:
                                        type = "Put";
                                        break;
                                    case HEAD:
                                        type = "Head";
                                        break;
                                    case OPTIONS:
                                        type = "Options";
                                        break;
                                    case TRACE:
                                        type = "Trace";
                                        break;
                                }
                                apiDoc.setType(type);
                            }

                            if(method.isAnnotationPresent(SingleRequestData.class)){
                                SingleRequestData singleRequestData = method.getAnnotation(SingleRequestData.class);
                                RequestInfo requestInfo = new RequestInfo();
                                requestInfo.setIntro(singleRequestData.description());
                                requestInfo.setName(singleRequestData.name());
                                requestInfo.setPosition(singleRequestData.position());
                                requestInfo.setRequired(singleRequestData.required()?"是":"否");
                                requestInfo.setType(singleRequestData.dataType());
                                listRequestInfo.add(requestInfo);
                            }

                            RequestData[] listRequestData = method.getAnnotationsByType(RequestData.class);
                            for(RequestData requestData : listRequestData){
                                RequestInfo requestInfo = new RequestInfo();
                                requestInfo.setIntro(requestData.description());
                                requestInfo.setName(requestData.name());
                                requestInfo.setPosition(requestData.position());
                                requestInfo.setRequired(requestData.required()?"是":"否");
                                requestInfo.setType(requestData.dataType());
                                listRequestInfo.add(requestInfo);
                            }

                            apiDoc.setListRequestInfo(listRequestInfo);

                            if(method.isAnnotationPresent(ResponseData.class)){
                                ResponseData responseData = method.getAnnotation(ResponseData.class);
                                Class<?> resClass = responseData.value();
                                if(!resClass.isAssignableFrom(Boolean.class)){
                                    Object o = resClass.newInstance();
                                    Field[] fieldList = o.getClass().getDeclaredFields();
                                    for (Field field : fieldList) {
                                        //集合类型
                                        if(isCollectionType(field.getType())){
                                            ResponseInfo responseHeader = new ResponseInfo();
                                            if(field.isAnnotationPresent(Column.class)){
                                                Column column = field.getAnnotation(Column.class);
                                                responseHeader.setNote(column.name());
                                            }
                                            responseHeader.setType("list");
                                            responseHeader.setName(field.getName());
                                            listResponseInfo.add(responseHeader);

                                            Type t = field.getGenericType();
                                            ParameterizedType pt = (ParameterizedType) t;
                                            Class clz = (Class) pt.getActualTypeArguments()[0];
                                            if(!isBaseType(clz.toString())){
                                                responseCustomize(listResponseInfo, clz,true);
                                            }else{
                                                ResponseInfo responseInfo = new ResponseInfo();
                                                if(field.isAnnotationPresent(Column.class)){
                                                    Column column = field.getAnnotation(Column.class);
                                                    responseInfo.setNote(column.name());
                                                }
                                                responseInfo.setName(field.getName());
                                                String type = clz.toString();
                                                switch (clz.toString()){
                                                    case "class java.lang.String":
                                                        type = "String";
                                                        break;
                                                    case "class java.lang.Byte":
                                                        type = "Byte";
                                                        break;
                                                    case "class java.lang.Long":
                                                        type = "Long";
                                                        break;
                                                    case "class java.lang.Integer":
                                                        type = "Integer";
                                                        break;
                                                    case "class java.math.BigDecimal":
                                                        type = "BigDecimal";
                                                        break;
                                                    case "class java.lang.Boolean":
                                                        type = "bool";
                                                        break;
                                                    case "class java.util.Date":
                                                        type = "Date";
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                responseInfo.setType(type);
                                                listResponseInfo.add(responseInfo);
                                            }

                                        }else if(!isCollectionType(field.getType()) && !isBaseType(field.getType().toString())){
                                            //自定义类型
                                            responseCustomize(listResponseInfo, field.getType(),false);
                                        }else{
                                            if(field.getName() == "serialVersionUID"){
                                                continue;
                                            }
                                            ResponseInfo responseInfo = new ResponseInfo();
                                            if(field.isAnnotationPresent(Column.class)){
                                                Column column = field.getAnnotation(Column.class);
                                                responseInfo.setNote(column.name());
                                            }
                                            responseInfo.setName(field.getName());
                                            String type = field.getType().toString();
                                            switch (field.getType().toString()){
                                                case "class java.lang.String":
                                                    type = "String";
                                                    break;
                                                case "class java.lang.Byte":
                                                    type = "Byte";
                                                    break;
                                                case "class java.lang.Long":
                                                    type = "Long";
                                                    break;
                                                case "class java.lang.Integer":
                                                    type = "Integer";
                                                    break;
                                                case "class java.math.BigDecimal":
                                                    type = "BigDecimal";
                                                    break;
                                                case "class java.lang.Boolean":
                                                    type = "bool";
                                                    break;
                                                case "class java.util.Date":
                                                    type = "Date";
                                                    break;
                                                default:
                                                    break;
                                            }
                                            responseInfo.setType(type);
                                            listResponseInfo.add(responseInfo);
                                        }
                                    }
                                    apiDoc.setListResponseInfo(listResponseInfo);
                                }else{
                                    apiDoc.setListResponseInfo(null);
                                }
                            }

                            //set page content
                            String content = apiDoc.getDocStr();

                            //upload doc
                            Map<String, String> params = new HashMap<>();
                            params.put("api_key",key);
                            params.put("api_token",token);
                            params.put("cat_name",catName);
                            params.put("page_title",apiDoc.getTitle());
                            params.put("page_content",content);
                            params.put("s_number", String.valueOf(index));

                            String result = HttpsUtil.post(requestUrl, null,params, null);
                            JSONObject resultObject = JSONObject.parseObject(result);
                            if(resultObject.getString("error_code").equals("0")){
                                System.out.println("upload and generator success");
                                count ++;
                            }else{
                                System.out.println(resultObject.getString("error_message"));
                            }
                        }
                    }

                }
                System.out.println("generator doc " + count);
            }else{
                System.out.println("not found");
            }
        }catch (Exception e){
            System.out.println("upload doc res error is " + e.getMessage()+error);
        }

    }

    private boolean isCollectionType(Class resClass){
        return resClass.toString().equals("interface java.util.List") ||
                resClass.toString().equals("interface java.util.Set");
    }

    private boolean isBaseType(String type) {

        return type.equals("class java.lang.String") ||
                type.equals("class java.lang.Byte") ||
                type.equals("class java.lang.Long") ||
                type.equals("class java.lang.Integer") ||
                type.equals("class java.math.BigDecimal") ||
                type.equals("class java.lang.Boolean") ||
                type.equals("class java.util.Date") ||
                type.equals("int") ||
                type.equals("long") ||
                type.equals("byte") ||
                type.equals("boolean");
    }

    private void responseCustomize(List<ResponseInfo> res , Class<?> resClass, boolean isChilld){
        try{
            Object o = resClass.newInstance();
            Field[] fieldList = o.getClass().getDeclaredFields();
            for (Field field : fieldList) {
                if(field.getName() == "serialVersionUID"){
                    continue;
                }
                ResponseInfo responseInfo = new ResponseInfo();
                if(field.isAnnotationPresent(Column.class)){
                    Column column = field.getAnnotation(Column.class);
                    responseInfo.setNote(column.name());
                }
                responseInfo.setName(field.getName());
                if(isChilld){
                    responseInfo.setName("&ensp;&ensp;|-- &ensp;"+field.getName());
                }else{
                    responseInfo.setName(field.getName());
                }


                String type = field.getType().toString();
                switch (field.getType().toString()){
                    case "class java.lang.String":
                        type = "String";
                        break;
                    case "class java.lang.Byte":
                        type = "Byte";
                        break;
                    case "class java.lang.Long":
                        type = "Long";
                        break;
                    case "class java.lang.Integer":
                        type = "Integer";
                        break;
                    case "class java.math.BigDecimal":
                        type = "BigDecimal";
                        break;
                    case "class java.lang.Boolean":
                        type = "bool";
                        break;
                    case "class java.util.Date":
                        type = "Date";
                        break;
                    default:
                        break;
                }
                responseInfo.setType(type);
                res.add(responseInfo);
            }
        }catch(Exception e){
            System.out.println("upload doc gen error is " + e.getMessage());
        }
    }



    private void initClassesLoader() {
        if (classLoaderInterface == null) {
            try {
                classLoaderInterface = new MavenPluginContextClassLoader(project);
            } catch (DependencyResolutionRequiredException e) {
                e.printStackTrace();
            }
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public ClassLoaderInterface getClassLoaderInterface() {
        return classLoaderInterface;
    }

    public void setClassLoaderInterface(ClassLoaderInterface classLoaderInterface) {
        this.classLoaderInterface = classLoaderInterface;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public String getScanSource() {
        return scanSource;
    }

    public void setScanSource(String scanSource) {
        this.scanSource = scanSource;
    }

    public String getPackages() {
        return packages;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
