package ca.discotek.sinktank;

import java.util.List;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class Configuration {

    List<Method> sourceMethodList = new ArrayList<Method>();
    List<String> sourceMethodAnnotationList = new ArrayList<String>();
    List<String> sourceParameterAnnotationList = new ArrayList<String>();
    
    List<Method> sinkMethodList = new ArrayList<Method>();
    List<String> sinkMethodAnnotationList = new ArrayList<String>();
    List<String> sinkParameterAnnotationList = new ArrayList<String>();

    public Method[] getSourceMethods() {
        return sourceMethodList.toArray(new Method[sourceMethodList.size()]);
    }
    
    public void setSourceMethods(Method methods[]) {
        sourceMethodList.clear();
        sourceMethodList.addAll(Arrays.asList(methods));
    }
    
    public String[] getSourceMethodAnnotations() {
        return sourceMethodAnnotationList.toArray(new String[sourceMethodAnnotationList.size()]);
    }
    
    public void setSourceMethodAnnotations(String methodAnnotations[]) {
        sourceMethodAnnotationList.clear();
        sourceMethodAnnotationList.addAll(Arrays.asList(methodAnnotations));
    }
    
    public String[] getSourceParameterAnnotations() {
        return sourceParameterAnnotationList.toArray(new String[sourceParameterAnnotationList.size()]);
    }
    
    public void setSourceParameterAnnotations(String parameterAnnotations[]) {
        sourceParameterAnnotationList.clear();
        sourceParameterAnnotationList.addAll(Arrays.asList(parameterAnnotations));
    }
    
    
    public Method[] getSinkMethods() {
        return sinkMethodList.toArray(new Method[sinkMethodList.size()]);
    }
    
    public void setSinkMethods(Method methods[]) {
        sinkMethodList.clear();
        sinkMethodList.addAll(Arrays.asList(methods));
    }
    
    public String[] getSinkMethodAnnotations() {
        return sinkMethodAnnotationList.toArray(new String[sinkMethodAnnotationList.size()]);
    }
    
    public void setSinkMethodAnnotations(String methodAnnotations[]) {
        sinkMethodAnnotationList.clear();
        sinkMethodAnnotationList.addAll(Arrays.asList(methodAnnotations));
    }
    
    public String[] getSinkParameterAnnotations() {
        return sinkParameterAnnotationList.toArray(new String[sinkParameterAnnotationList.size()]);
    }
    
    public void setSinkParameterAnnotations(String parameterAnnotations[]) {
        sinkParameterAnnotationList.clear();
        sinkParameterAnnotationList.addAll(Arrays.asList(parameterAnnotations));
    }
}
