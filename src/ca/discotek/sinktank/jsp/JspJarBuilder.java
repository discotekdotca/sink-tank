package ca.discotek.sinktank.jsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.jasper.JspC;

import ca.discotek.sinktank.Util;
import ca.discotek.sinktank.io.IOUtil;
import ca.discotek.sinktank.jsp.JspUtil.JspListener;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class JspJarBuilder implements JspListener{

    static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd--hh-mm-ss--SSS");
    
    static final String FILE_SEPARATOR = System.getProperty("file.separator");
    
    static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    static final String WORKING_DIR_NAME = "discotek_sinktank";
    static final File WORKING_DIR = new File(TMP_DIR, WORKING_DIR_NAME);
    static final String NESTED_JAR_DIR_NAME = "nested_jars";
    static final File NESTED_JAR_DIR = new File(WORKING_DIR, NESTED_JAR_DIR_NAME);
    
    File file;
    
    Set<File> jspJarSet = new HashSet<File>();
    Set<String> rawJarSet = new HashSet<String>();
    Set<File> jarSet = new HashSet<File>();
    
    File workingDir;
    File nestedJarDir;
    
    public void buildJspJars(File deployments[]) throws IOException {
        String timestampText = FORMAT.format( System.currentTimeMillis() );
        
        workingDir = new File(WORKING_DIR, timestampText);
        workingDir.deleteOnExit();
        nestedJarDir = new File(NESTED_JAR_DIR, timestampText);
        nestedJarDir.deleteOnExit();
        JspUtil util = new JspUtil();

        util.addListener(this);
        for (int i=0; i<deployments.length; i++) {
            file = deployments[i];
            util.process(deployments[i]);
        }
    }
    
    public File[] getJspJars() {
        return jspJarSet.toArray(new File[jspJarSet.size()]);
    }

    @Override
    public void foundJar(String jar) {
        rawJarSet.add(jar);
    }

    @Override
    public void foundWarClass(String clazz, byte[] bytes) {
        
    }

    @Override
    public void foundJsp(String jsp, byte[] bytes) {
        String paths[] = jsp.split("!/");
        StringBuilder buffer = new StringBuilder();
        for (int i=0; i<paths.length-1; i++) {
            if (i>0)
                buffer.append("_");
            buffer.append(paths[i].replace(":", "_"));
        }
        buffer.append(".jsp.war");
        
        File directory = new File(workingDir, buffer.toString());
        directory.deleteOnExit();
        jspJarSet.add(directory);
        File file = new File(directory, paths[paths.length-1]);
        file.deleteOnExit();
        file.getParentFile().mkdirs();
        try {
            Util.write(bytes, file);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void end() {
        if (jspJarSet.size() == 0)
            return;
        
        Iterator<String> rawJarIt = rawJarSet.iterator();
        String rawJar;
        while (rawJarIt.hasNext()) {
            rawJar = rawJarIt.next();
            String paths[] = rawJar.split("!/");
            if (paths.length == 1)
                jarSet.add(new File(rawJar));
            else {
                InputStream is = null;
                try {
                    FileInputStream fos = new FileInputStream(paths[0]);
                    ZipInputStream zis = new ZipInputStream(fos);
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(paths[0].replace(":", "_"));
                    for (int i=1; i<paths.length; i++) {
                        buffer.append("_");
                        buffer.append(paths[i]);
                        is = Util.getInputStream(zis, paths[i]);
                        zis = new ZipInputStream(is);
                    }
                    
                    if (is == null)
                        throw new RuntimeException("Bug. Couldn't find path: " + rawJar);
                    
                    File file = new File(nestedJarDir, Util.escapeHtml(buffer.toString()));
                    file.deleteOnExit();
                    file.getParentFile().mkdirs();
                    Util.write(is, file);
                    
                    jarSet.add(file);
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        

        
        
        File f;
        Iterator<File> jarIt = jarSet.iterator();
        StringBuilder buffer = new StringBuilder();
        while (jarIt.hasNext()) {
            f = jarIt.next();
            buffer.append(f.getAbsolutePath());
            if (jarIt.hasNext())
                buffer.append(System.getProperty("path.separator"));
        }
        
        String classpath = buffer.toString();
        String packagePathSuffix;
        String path;
        File files[];
        File directory;
        Iterator<File> it = jspJarSet.iterator();
        while (it.hasNext()) {
            directory = it.next();
            files = IOUtil.listFilesRecursively(directory, true);
            for (int i=0; i<files.length; i++) {
                if (files[i].getName().endsWith(".jsp")) {
                    path = files[i].getAbsolutePath();
                    int index = path.lastIndexOf(FILE_SEPARATOR);
                    packagePathSuffix = path.substring(directory.getAbsolutePath().length(), index < 0 ? path.length() : index).replace(FILE_SEPARATOR, "_").replace("-", "_");
                    
                    System.out.println("Compiling: " + files[i].getAbsolutePath());
                    try {
                        JspC.main(new String[] {
//                                "-v",
//                                "-d", "c:/temp/jsp-output",
//                                "-d", files[i].getParentFile().getAbsolutePath(),
                                "-d", directory.getAbsolutePath(),
                                "-s",
                                "-classpath", classpath,
                                "-l",
//                                "-p", "ca.discotek.org.apache.jsp",
                                "-p", "ca.discotek." + packagePathSuffix,
                                "-compile",
//                                "-uriroot", "C:/temp/struts/web-app/",
                                "-uriroot", files[i].getParentFile().getAbsolutePath(),
                                files[i].getAbsolutePath()
                        });            
                    }
                    catch (Exception e) {
                        System.out.println("Compiled failed for " + files[i]);
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }
}
