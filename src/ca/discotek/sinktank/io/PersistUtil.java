package ca.discotek.sinktank.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.Path;
import ca.discotek.sinktank.ResultsConsolidator;
import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class PersistUtil {

    static final String REPORT_NAME = "SinkTank.html";
    
    static final String CSS_DIR = "css";
    static final String JS_DIR = "js";  
    
    static final String CSS_FILES[] = {
            "bootstrap.min.css"
    };
    
    static final String JS_FILES[] = {
            "bootstrap.min.js",
            "jquery.min.js"
    };
    
    PrintStream ps = null;
    
    Map<Method, HtmlList> sinkRootMap = new HashMap<Method, HtmlList>();
    
    public PersistUtil() {}
    
    public void saveToHtml(String directory, ResultsConsolidator rc, String title, Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap, Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap, Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) throws IOException {
        saveToHtml(new File(directory), rc, title, sourceOriginMap, sinkOriginMap, sourceMethodAnnotationMap, sinkMethodAnnotationMap, sourceMethodParameterAnnotationMap, sinkMethodParameterAnnotationMap) ;
    }
    
    void copyFiles(String files[], String subdir, File destinationDir) throws IOException {
        InputStream is;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        FileOutputStream fos;
        int length;
        byte bytes[] = new byte[1024];
        File file;
        for (int i=0; i<files.length; i++) {
            is = cl.getResourceAsStream(subdir + "/" + files[i]);
            file = new File(destinationDir, subdir + '/' + files[i]);
            file.getParentFile().mkdirs();
            fos = new FileOutputStream(file);
            while ( (length = is.read(bytes)) > 0)
                fos.write(bytes, 0, length);
            is.close();
            fos.close();
        }
    }
    
    public void saveToHtml(File directory, ResultsConsolidator rc, String title, Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap, Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap, Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) throws IOException {

        copyFiles(CSS_FILES, CSS_DIR, directory);
        copyFiles(JS_FILES, JS_DIR, directory);
        
        
        sinkRootMap.clear();
        
        File file = new File(directory, REPORT_NAME);
        
        ps = new PrintStream(file);
        printStart(title);
        

        HtmlList sourceSinkTopList = new HtmlList("Common Method&rarr;Source/Sinks");
        Path paths[][] = rc.getSourceSinkPaths(true, true);
        for (int i=0; i<paths.length; i++)
            addSourceSink(paths[i][0], paths[i][1], sourceSinkTopList, sourceOriginMap, sinkOriginMap, sourceMethodAnnotationMap, sinkMethodAnnotationMap, sourceMethodParameterAnnotationMap, sinkMethodParameterAnnotationMap);

        HtmlList sourceTopList = new HtmlList("Sources");
        Path path[] = rc.getSourcePaths(true);
        for (int i=0; i<path.length; i++)
            addPath(path[i], sourceTopList);
        
        HtmlList sinkTopList = new HtmlList("Sinks");
        path = rc.getSinkPaths(true);
        for (int i=0; i<path.length; i++)
            addPath(path[i], sinkTopList);
        
        ps.println("<ul class=\"nav nav-tabs\">");
        ps.println("    <li class=\"active\"><a data-toggle=\"tab\" href=\"#source-sink\">Source&rarr;Sinks</a></li>");
        ps.println("    <li><a data-toggle=\"tab\" href=\"#sources\">Sources</a></li>");
        ps.println("    <li><a data-toggle=\"tab\" href=\"#sinks\">Sinks</a></li>");
        ps.println("</ul>");
        
        
        ps.println("<div class=\"tab-content\">");
        ps.println("    <div id=\"source-sink\" class=\"tab-pane fade in active\">");
        ps.println("      <h3>Source&rarr;Sinks</h3>");
        ps.println(sourceSinkTopList.toString());
        ps.println("    </div>");
        ps.println("    <div id=\"sources\" class=\"tab-pane fade\">");
        ps.println("      <h3>Sources</h3>");
        ps.println(sourceTopList.toString());
        ps.println("    </div>");
        ps.println("    <div id=\"sinks\" class=\"tab-pane fade\">");
        ps.println("      <h3>Sinks</h3>");
        ps.println(sinkTopList.toString());
        ps.println("    </div>");
        ps.println(" </div>");
        
        
        printEnd();
        
        ps.close();
    }
    
    public void addPath(Path path, HtmlList topList) {
        HtmlList parentNode = topList;
        HtmlList methodNode = null;
        for (int i=path.methods.length-1; i>=0; i--) {
            methodNode = getMethodChild(path.methods[i], parentNode);
            if (methodNode == null) {
                methodNode = new HtmlList(Util.escapeHtml(path.methods[i].toString())); 
                parentNode.addItem(methodNode);
            }
            parentNode = methodNode;
        }
    }
    
    public void addSourceSink(Path sourcePath, Path sinkPath, HtmlList topList, Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap, Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap, Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) {
        Method sinkMethod = sinkPath.methods[sinkPath.methods.length-1];
        HtmlList sinkRootNode = sinkRootMap.get(sinkMethod);
        boolean sinkRootNodeIsNull = sinkRootNode == null;
        if (sinkRootNodeIsNull) {
            String tooltip = getMethodToolTip(sinkMethod, sourceOriginMap, sinkOriginMap, sourceMethodAnnotationMap, sinkMethodAnnotationMap, sourceMethodParameterAnnotationMap, sinkMethodParameterAnnotationMap);
            if (sinkMethod.toString().contains("BlindSendFileAssignment"))
                System.out.println();

            if (tooltip == null)
                sinkRootNode = new HtmlList("<b>" + Util.escapeHtml(sinkMethod.toString()) + "</b>");
            else
                sinkRootNode = new HtmlList("<span title=\"" + tooltip + "\">" + "<b>" + Util.escapeHtml(sinkMethod.toString()) + "</b></span>");
            sinkRootMap.put(sinkMethod, sinkRootNode);
            topList.addItem(sinkRootNode);
        }
        
        if (sourcePath.methods.length == 1 && sinkPath.methods.length == 1 && sourcePath.methods[0] == sinkPath.methods[0])
            return;
        
        HtmlList parentNode = sinkRootNode;
        HtmlList methodNode = null;
        String text;
        String tooltip;
        
        HtmlList commonNode = null;
        if (!sinkRootNodeIsNull) {
            int count = sinkRootNode.list.size();
            for (int i=0; i<count; i++) {
                commonNode = (HtmlList) sinkRootNode.list.get(i);
                if (commonNode.name.equals(sourcePath.methods[0].toString()))
                    break;
                else
                    commonNode = null;
            }
        }
        
        boolean commonNodeExists = commonNode != null;
        if (commonNode == null) {
            commonNode = new HtmlList(sourcePath.methods[0].toString());
            parentNode.addItem(commonNode);
            parentNode = commonNode;
        }
        
        if (sourcePath.methods.length > 1) {
            HtmlList sourceNode;
            if (commonNodeExists)
                sourceNode = (HtmlList) commonNode.list.get(0);
            else {
                sourceNode = new HtmlList("[Sources]");
                commonNode.addItem(sourceNode);
            }
            parentNode = sourceNode;
        }
        
        for (int i=0; i<sourcePath.methods.length; i++) {
            if (i == 0)
                continue;
            
            methodNode = getMethodChild(sourcePath.methods[i], parentNode);
            if (methodNode == null) {
                text =  Util.escapeHtml(sourcePath.methods[i].toString());
                tooltip = getMethodToolTip(sourcePath.methods[i], sourceOriginMap, sinkOriginMap, sourceMethodAnnotationMap, sinkMethodAnnotationMap, sourceMethodParameterAnnotationMap, sinkMethodParameterAnnotationMap);
                if (tooltip == null)
                    methodNode = new HtmlList(i<sourcePath.methods.length-1 ? text : "<html><font color=\"red\">" + text + "</font></html>");
                else
                    methodNode = new HtmlList(i<sourcePath.methods.length-1 ? text : "<html><span title=\"" + tooltip + "\"><font color=\"red\">" + text + "</font></span></html>");
                parentNode.addItem(methodNode);
            }
            
            parentNode = methodNode;
        }

        if (sinkPath.methods.length > 1) {
            HtmlList sinkNode;
            if (commonNodeExists)
                sinkNode = ((HtmlList) commonNode.list.get(1));
            else {
                sinkNode = new HtmlList("[Sinks]");
                commonNode.addItem(sinkNode);
            }
            parentNode = sinkNode;
        }
        
        for (int i=0; i<sinkPath.methods.length; i++) {
            if (i == 0)
                continue;

            methodNode = getMethodChild(sinkPath.methods[i], parentNode);
            if (methodNode == null) {
                text = Util.escapeHtml(sinkPath.methods[i].toString());
                methodNode = new HtmlList(i<sinkPath.methods.length-1 ? text : "<html><font color=\"red\">" + text + "</font></html>");
                parentNode.addItem(methodNode);
            }
            
            parentNode = methodNode;
        }
    }
    
    
    
    
    HtmlList getMethodChild(Method method, HtmlList parentNode) {
        String methodText = method.toString();
        int count = parentNode.list.size();
        for (int i=0; i<count; i++) {
            HtmlList node = (HtmlList) parentNode.list.get(i);
            if (node.name.equals(methodText))
                return node;
        }
        
        return null;
    }
    
    
    void printPaths(String header, Path paths[]) {
        printHeader(header, 1);
        HtmlList topLevelList;
        HtmlList parentList;
        HtmlList childList;
        
        for (Path path : paths) {
            topLevelList = null;
            parentList = null;
            
            for (int i=path.methods.length-1; i>=0; i--) {
                if (i > 0) {
                    childList = new HtmlList(path.methods[i].toString());
                    if (parentList == null)
                        topLevelList = parentList = childList;
                    else {
                        parentList.addItem(childList);
                        parentList = childList;
                    }
                }
                else 
                    parentList.addItem(path.methods[i].toString());
            }
            
            println(topLevelList.toString());
        }
    }
    
    void printSourceSinkPaths(String header, Path sourcePaths[], Path sinkPaths[]) {
        printHeader(header, 1);
        String sink;
        HtmlList topList;
        HtmlList commonList;
        HtmlList sourceList;
        HtmlList sinkList;
        HtmlList parentList;
        HtmlList childList;
        for (int i=0; i<sourcePaths.length; i++) {

            sourceList = new HtmlList("[Source]");
            sinkList = new HtmlList("[Sink]");
            
            sink = sinkPaths[i].methods[sinkPaths[i].methods.length-1].toString();
            topList = new HtmlList("<b>" + sink + "</b>");
            
            if (sourcePaths[i].methods.length + sinkPaths[i].methods.length != 2) {
                commonList = new HtmlList(sinkPaths[i].methods[0].toString());
                topList.addItem(commonList);
                commonList.addItem(sourceList);

                parentList = sourceList;
                for (int j=1; j<sourcePaths[i].methods.length; j++) {
                    
                    if (j<sourcePaths[i].methods.length - 1) {
                        childList = new HtmlList(sourcePaths[i].methods[j].toString());
                        parentList.addItem(childList);
                        parentList = childList;
                    }
                    else
                        parentList.addItem(sourcePaths[i].methods[j].toString());
                }
                
                commonList.addItem(sinkList);
                parentList = sinkList;
                for (int j=1; j<sinkPaths[i].methods.length; j++) {

                    
                    if (j<sinkPaths[i].methods.length - 1) {
                        childList = new HtmlList(sinkPaths[i].methods[j].toString());
                        parentList.addItem(childList);
                        parentList = childList;
                    }
                    else
                        parentList.addItem(sinkPaths[i].methods[j].toString());
                }
            }
            
            println(topList.toString());
        }
    }
    
    
    void printStart(String title) {
        ps.println("<html>");
        ps.println("<head>");

        ps.print("<title>");
        ps.print(title);
        ps.println("</title>");
        
        ps.println("<link rel=\"stylesheet\" href=\"css/bootstrap.min.css\">");
        ps.println("<script src=\"js/jquery.min.js\"></script>");
        ps.println("<script src=\"js/bootstrap.min.js\"></script>");
        
        ps.println("</head>");
        ps.println("<body>");
    }
    
    void printHeader(String header, int size) {
        print("<h" + size + ">");
        print(header);
        println("</h" + size + ">");
    }
    
    void printEnd() {
        ps.println("<body>");
        ps.println("</html>");
    }
    
    void println(Path path) {
        print(path);
        ps.println();
    }
    
    void print(Path path) {
        for (int i=0; i<path.methods.length; i++) {
            print(path.methods[i].toString());
            if (i<path.methods.length-1)
                print("|");
        }
    }
    
    void print(String s) {
        ps.print(s);
    }
    
    void println(String s) {
        ps.println(s);
    }
    
    static final String CRLF = "&#013;&#010;";
    static final String BULLET = "&#8226;";
    
    public String getMethodToolTip(Method method, Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap, Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap, Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) {
        String text = null;

        StringBuilder buffer = new StringBuilder();

        List<Method> sourceOriginList = sourceOriginMap.get(method);
        List<Method> sinkOriginList = sinkOriginMap.get(method);

        boolean found = sourceOriginList != null || sinkOriginList != null;
        
        if (found) {
            buffer.append("");
            buffer.append("This method is included because it is a descendent of the following sink/source method(s):" + CRLF);
        }
        if (sourceOriginList != null) {
            Iterator<Method> it = sourceOriginList.listIterator();
            while (it.hasNext()) {
                buffer.append(BULLET + it.next() + CRLF);
            }
        }
        
        if (sinkOriginList != null) {
            found = true;
            Iterator<Method> it = sinkOriginList.listIterator();
            while (it.hasNext()) {
                buffer.append(BULLET + it.next() + " [sink]" + CRLF);
            }
        }


        List<String> list = sourceMethodAnnotationMap.get(method);
        Set<String> set = new HashSet<>();

        if (list != null) {
            found = true;
            buffer.append("This method has the following source method annotation(s):" + CRLF);
            Iterator<String> annotationIt = list.listIterator();
            String annotation;
            set.clear();
            while (annotationIt.hasNext()) {
                annotation = annotationIt.next();
                if (!set.contains(annotation)) {
                    buffer.append(BULLET + annotation + CRLF);
                    set.add(annotation);
                }
            }
        }
        
        list = sourceMethodParameterAnnotationMap.get(method);
        if (list != null) {
            found = true;
            buffer.append("This method has the following source method parameter annotation(s):" + CRLF);
            Iterator<String> annotationIt = list.listIterator();
            String annotation;
            set.clear();
            while (annotationIt.hasNext()) {
                annotation = annotationIt.next();
                if (!set.contains(annotation)) {
                    buffer.append(BULLET + annotation + CRLF);
                    set.add(annotation);
                }
            }
        }
        
        list = sinkMethodAnnotationMap.get(method);
        if (list != null) {
            found = true;
            buffer.append("This method has the following sink method annotation(s):" + CRLF);
            Iterator<String> annotationIt = list.listIterator();
            String annotation;
            set.clear();
            while (annotationIt.hasNext()) {
                annotation = annotationIt.next();
                if (!set.contains(annotation)) {
                    buffer.append(BULLET + annotation + CRLF);
                    set.add(annotation);
                }
            }
        }
        
        list = sinkMethodParameterAnnotationMap.get(method);
        if (list != null) {
            found = true;
            buffer.append("This method has the following sink method parameter annotation(s):" + CRLF);
            Iterator<String> annotationIt = list.listIterator();
            String annotation;
            set.clear();
            while (annotationIt.hasNext()) {
                annotation = annotationIt.next();
                if (!set.contains(annotation)) {
                    buffer.append(BULLET + annotation + CRLF);
                    set.add(annotation);
                }
            }
        }
        
        if (found)
            text = buffer.toString();
        
        return text;
    }

}
