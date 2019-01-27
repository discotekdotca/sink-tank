package ca.discotek.sinktank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class UserConfiguration {

    List<Method> disabledSourceMethodList = new ArrayList<Method>();
    List<String> disabledSourceMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> disabledSinkMethodList = new ArrayList<Method>();
    List<String> disabledSinkMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSinkMethodParameterAnnotationList = new ArrayList<String>();
    
    List<Method> addedSourceMethodList = new ArrayList<Method>();
    List<String> addedSourceMethodAnnotationList = new ArrayList<String>();
    List<String> addedSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> addedSinkMethodList = new ArrayList<Method>();
    List<String> addedSinkMethodAnnotationList = new ArrayList<String>();
    List<String> addedSinkMethodParameterAnnotationList = new ArrayList<String>();
    
    public void reset() {
        disabledSourceMethodList.clear();
        disabledSourceMethodAnnotationList.clear();
        disabledSourceMethodParameterAnnotationList.clear();
        disabledSinkMethodList.clear();
        disabledSinkMethodAnnotationList.clear();
        disabledSinkMethodParameterAnnotationList.clear();
        
        addedSourceMethodList.clear();
        addedSourceMethodAnnotationList.clear();
        addedSourceMethodParameterAnnotationList.clear();
        addedSinkMethodList.clear();
        addedSinkMethodAnnotationList.clear();
        addedSinkMethodParameterAnnotationList.clear();
    }
    
    public void parse(String file) throws IOException {
        parse(new File(file));
    }
    
    public void parse(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            parse(fis);
        } 
        finally {
            if (fis != null) fis.close();
        }
    }
    
    public void parse(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ( (line = br.readLine()) != null ) {
            parseLine(line);
        }
    }
    
    void parseLine(String line) throws IOException {
        String trimmed = line.trim();
        if (trimmed.length() == 0)
            return;
        else {
            String lowercase = trimmed.toLowerCase();
            if (lowercase.startsWith("disabled-source-method=")) {
                disabledSourceMethodList.add( parseMethod(getValue(line)) );
            }
            else if (lowercase.startsWith("disabled-source-method-annotation=")) {
                disabledSourceMethodAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("disabled-source-method-parameter-annotation=")) {
                disabledSourceMethodParameterAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("disabled-sink-method=")) {
                disabledSinkMethodList.add( parseSinkMethod(getValue(line)) );
            }
            else if (lowercase.startsWith("disabled-sink-method-annotation=")) {
                disabledSinkMethodAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("disabled-sink-method-parameter-annotation=")) {
                disabledSinkMethodParameterAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("added-source-method=")) {
                addedSourceMethodList.add( parseMethod(getValue(line)) );
            }
            else if (lowercase.startsWith("added-source-method-annotation=")) {
                addedSourceMethodAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("added-source-method-parameter-annotation=")) {
                addedSourceMethodParameterAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("added-sink-method=")) {
                addedSinkMethodList.add( parseSinkMethod(getValue(line)) );
            }
            else if (lowercase.startsWith("added-sink-method-annotation=")) {
                addedSinkMethodAnnotationList.add( getValue(line) );
            }
            else if (lowercase.startsWith("added-sink-method-parameter-annotation=")) {
                addedSinkMethodParameterAnnotationList.add( getValue(line) );
            }
            else
                throw new IOException("Found unexpected line: " + line);
        }
    }
    
    String getValue(String line) throws IOException {
        String chunks[] = line.split("=");
        if (chunks.length == 2)
            return chunks[1];
        else
            throw new IOException("Line has illegal format. Must be <name>=<value>. Line: " + line);
    }
    
    Method parseMethod(String value) {
        int colonIndex = value.lastIndexOf(':');
        String returnType = value.substring(0, colonIndex);
        
        int startParam = value.indexOf('(');
        int endParam = value.indexOf(')');

        String noParams = value.substring(colonIndex+1, startParam);
        
        int startMethod = noParams.lastIndexOf('.') + returnType.length() + 1 + 1; // one for colon, one for skipping the '.'
        String className = value.substring(colonIndex+1, startMethod - 1); // remove one for the '.'
        
        String methodName = value.substring(startMethod, startParam);

        String params = value.substring(startParam+1, endParam);
        String internalParams = AsmUtil.toInternalParams(params.split(","));
        String desc = '(' + internalParams + ')' + (returnType.equals("void") ? "V" : AsmUtil.toInternalType(returnType));
        
        return NodeManager.getMethod(className, methodName, desc);
    }
    
    Method parseSinkMethod(String value) {
        int index = value.indexOf('{');
        String rawMethod = value.substring(0, index < 0 ? value.length() : index);
        Method m = parseMethod(rawMethod);

        if (index > 0) {
            String noBrackets = value.substring(index+1, value.length()-1);
            String chunks[] = noBrackets.split(", ");
            for (int i=0; i<chunks.length; i++)
                m.addVulnerableParameterIndex(Integer.parseInt(chunks[i]));
        }
        
        return m;
    }
    
    /*
    List<Method> disabledSourceMethodList = new ArrayList<Method>();
    List<String> disabledSourceMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> disabledSinkMethodList = new ArrayList<Method>();
    List<String> disabledSinkMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSinkMethodParameterAnnotationList = new ArrayList<String>();
    
    List<Method> addedSourceMethodList = new ArrayList<Method>();
    List<String> addedSourceMethodAnnotationList = new ArrayList<String>();
    List<String> addedSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> addedSinkMethodList = new ArrayList<Method>();
    List<String> addedSinkMethodAnnotationList = new ArrayList<String>();
    List<String> addedSinkMethodParameterAnnotationList = new ArrayList<String>();
     */
    
    void dump() {
        System.out.println("disabledSourceMethodList: " + disabledSourceMethodList);
        System.out.println("disabledSourceMethodAnnotationList: " + disabledSourceMethodAnnotationList);
        System.out.println("disabledSourceMethodAnnotationParameterList: " + disabledSourceMethodParameterAnnotationList);
        System.out.println("disabledSinkMethodList: " + disabledSourceMethodList);
        System.out.println("disabledSinkMethodAnnotationList: " + disabledSourceMethodAnnotationList);
        System.out.println("disabledSinkMethodParameterAnnotationxList: " + disabledSourceMethodParameterAnnotationList);
        
        System.out.println("addedSourceMethodList: " + addedSourceMethodList);
        System.out.println("addedSourceMethodAnnotationList: " + addedSourceMethodAnnotationList);
        System.out.println("addedSourceMethodAnnotationParameterList: " + addedSourceMethodParameterAnnotationList);
        System.out.println("addedSinkMethodList: " + addedSourceMethodList);
        System.out.println("addedSinkMethodAnnotationList: " + addedSourceMethodAnnotationList);
        System.out.println("addedSinkMethodParameterAnnotationList: " + addedSourceMethodParameterAnnotationList);
    }
    
    public void save(File file) throws FileNotFoundException {
        PrintStream ps = new PrintStream(file);
        
        Iterator<Integer> it;
        
        for (Method m : disabledSourceMethodList)
            ps.println("disabled-source-method=" + AsmUtil.toReturnTypeString(ProjectData.getMethodDesc(m.descId)) + ":" + m.toString());
        for (String a : disabledSourceMethodAnnotationList)
            ps.println("disabled-source-method-annotation=" + a);
        for (String a : disabledSourceMethodParameterAnnotationList)
            ps.println("disabled-source-method-parameter-annotation=" + a);
        
        for (Method m : disabledSinkMethodList) {
            ps.print("disabled-sink-method=" + AsmUtil.toReturnTypeString(ProjectData.getMethodDesc(m.descId)) + ":" + m.toString());
            ps.println(getVulnerableParametersText(m));
        }
        for (String a : disabledSinkMethodAnnotationList)
            ps.println("disabled-sink-method-annotation=" + a);
        for (String a : disabledSinkMethodParameterAnnotationList)
            ps.println("disabled-sink-method-parameter-annotation=" + a);
        
        for (Method m : addedSourceMethodList) {
            ps.print("added-source-method=" + AsmUtil.toReturnTypeString(ProjectData.getMethodDesc(m.descId)) + ":" + m.toString());
            ps.println(getVulnerableParametersText(m));
        }
        for (String a : addedSourceMethodAnnotationList)
            ps.println("added-source-method-annotation=" + a);
        for (String a : addedSourceMethodParameterAnnotationList)
            ps.println("added-source-method-parameter-annotation=" + a);
        
        
        for (Method m : addedSinkMethodList) {
            ps.print("added-sink-method=" + AsmUtil.toReturnTypeString(ProjectData.getMethodDesc(m.descId)) + ":" + m.toString());
            ps.println(getVulnerableParametersText(m));
        }
        for (String a : addedSinkMethodAnnotationList)
            ps.println("added-sink-method-annotation=" + a);
        for (String a : addedSinkMethodParameterAnnotationList)
            ps.println("added-sink-method-parameter-annotation=" + a);
    }
    
    public static String getVulnerableParametersText(Method sinkMethod) {
        return getVulnerableParametersText(sinkMethod, true);
    }
    
    public static String getVulnerableParametersText(Method sinkMethod, boolean includeIfNoneDefined) {
        Integer params[] = sinkMethod.getVulnerableParameters();
        if (params.length == 0 && !includeIfNoneDefined)
            return "";
        StringBuilder buffer = new StringBuilder();
        for (int i=0; i<params.length; i++) {
            if (i>0)
                buffer.append(", ");
            buffer.append(params[i]);
        }
        
        return "[" + buffer.toString() + "]";
    }
    
    /*
     *     List<Method> disabledSourceMethodList = new ArrayList<Method>();
    List<String> disabledSourceMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> disabledSinkMethodList = new ArrayList<Method>();
    List<String> disabledSinkMethodAnnotationList = new ArrayList<String>();
    List<String> disabledSinkMethodParameterAnnotationList = new ArrayList<String>();
    
    List<Method> addedSourceMethodList = new ArrayList<Method>();
    List<String> addedSourceMethodAnnotationList = new ArrayList<String>();
    List<String> addedSourceMethodParameterAnnotationList = new ArrayList<String>();
    List<Method> addedSinkMethodList = new ArrayList<Method>();
    List<String> addedSinkMethodAnnotationList = new ArrayList<String>();
    List<String> addedSinkMethodParameterAnnotationList = new ArrayList<String>();
     */
    
    public Method[] getDisabledSourceMethods() {
        return disabledSourceMethodList.toArray(new Method[disabledSourceMethodList.size()]);
    }
    
    public void toggleDisabledSourceMethod(Method method) {
        if (!disabledSourceMethodList.remove(method))
            disabledSourceMethodList.add(method);
    }
    
    public String[] getDisabledSourceMethodAnnotations() {
        return disabledSourceMethodAnnotationList.toArray(new String[disabledSourceMethodAnnotationList.size()]);
    }
    
    public void toggleDisabledSourceMethodAnnotation(String annotation) {
        if (!disabledSourceMethodAnnotationList.remove(annotation))
            disabledSourceMethodAnnotationList.add(annotation);
    }
    
    public String[] getDisabledSourceMethodParameterAnnotations() {
        return disabledSourceMethodParameterAnnotationList.toArray(new String[disabledSourceMethodParameterAnnotationList.size()]);
    }
    
    public void toggleDisabledSourceMethodParameterAnnotation(String annotation) {
        if (!disabledSourceMethodParameterAnnotationList.remove(annotation))
            disabledSourceMethodParameterAnnotationList.add(annotation);
    }
    
    
    public Method[] getDisabledSinkMethods() {
        return disabledSinkMethodList.toArray(new Method[disabledSinkMethodList.size()]);
    }
    
    public void toggleDisabledSinkMethod(Method method) {
        if (!disabledSinkMethodList.remove(method))
            disabledSinkMethodList.add(method);
    }
    
    public String[] getDisabledSinkMethodAnnotations() {
        return disabledSinkMethodAnnotationList.toArray(new String[disabledSinkMethodAnnotationList.size()]);
    }
    
    public void toggleDisabledSinkMethodAnnotation(String annotation) {
        if (!disabledSinkMethodAnnotationList.remove(annotation))
            disabledSinkMethodAnnotationList.add(annotation);
    }
    
    public String[] getDisabledSinkMethodParameterAnnotations() {
        return disabledSinkMethodParameterAnnotationList.toArray(new String[disabledSinkMethodParameterAnnotationList.size()]);
    }
    
    public void toggleDisabledSinkMethodParameterAnnotation(String annotation) {
        if (!disabledSinkMethodParameterAnnotationList.remove(annotation))
            disabledSinkMethodParameterAnnotationList.add(annotation);
    }
    
    
    
    
    
    
    public Method[] getAddedSourceMethods() {
        return addedSourceMethodList.toArray(new Method[addedSourceMethodList.size()]);
    }
    
    public void addSourceMethod(Method method) {
        addedSourceMethodList.add(method);
    }
    
    public void removeSourceMethod(Method method) {
        addedSourceMethodList.remove(method);
    }
    
    public String[] getAddedSourceMethodAnnotations() {
        return addedSourceMethodAnnotationList.toArray(new String[addedSourceMethodAnnotationList.size()]);
    }
    
    public void addSourceMethodAnnotation(String annotation) {
        addedSourceMethodAnnotationList.add(annotation);
    }
    
    public void removeSourceMethodAnnotation(String annotation) {
        addedSourceMethodAnnotationList.remove(annotation);
    }
    
    public String[] getAddedSourceMethodParameterAnnotations() {
        return addedSourceMethodParameterAnnotationList.toArray(new String[addedSourceMethodParameterAnnotationList.size()]);
    }
    
    public void addSourceMethodParameterAnnotation(String annotation) {
        addedSourceMethodParameterAnnotationList.add(annotation);
    }
    
    public void removeSourceMethodParameterAnnotation(String annotation) {
        addedSourceMethodParameterAnnotationList.remove(annotation);
    }
    
    
    public Method[] getAddedSinkMethods() {
        return addedSinkMethodList.toArray(new Method[addedSinkMethodList.size()]);
    }
    
    public void addSinkMethod(Method method) {
        addedSinkMethodList.add(method);
    }
    
    public void removeSinkMethod(Method method) {
        addedSinkMethodList.remove(method);
    }
    
    public String[] getAddedSinkMethodAnnotations() {
        return addedSinkMethodAnnotationList.toArray(new String[addedSinkMethodAnnotationList.size()]);
    }
    
    public void addSinkMethodAnnotation(String annotation) {
        addedSinkMethodAnnotationList.add(annotation);
    }
    
    public void removeSinkMethodAnnotation(String annotation) {
        addedSinkMethodAnnotationList.remove(annotation);
    }
    
    public String[] getAddedSinkMethodParameterAnnotations() {
        return addedSinkMethodParameterAnnotationList.toArray(new String[addedSinkMethodParameterAnnotationList.size()]);
    }
    
    public void addSinkMethodParameterAnnotation(String annotation) {
        addedSinkMethodParameterAnnotationList.add(annotation);
    }
    
    public void removeSinkMethodParameterAnnotation(String annotation) {
        addedSinkMethodParameterAnnotationList.remove(annotation);
    }
    
    
    
    
    public static void main(String[] args) throws IOException {
        UserConfiguration config = new UserConfiguration();
        config.parse("C:/temp/config/config.properties");
        config.dump();
    }
}
