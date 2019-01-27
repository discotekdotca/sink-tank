package ca.discotek.sinktank.jsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class JspUtil {

    List<JspListener> listenerList = new ArrayList<JspListener>();
    
    public JspUtil() {
        
    }
    
    public void addListener(JspListener l) {
        listenerList.add(l);
    }
    
    public void fireFoundJsp(String jsp, byte bytes[]) {
        Iterator<JspListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundJsp(jsp, bytes);
    }
    
    public void fireFoundWarClass(String clazz, byte bytes[]) {
        Iterator<JspListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundWarClass(clazz, bytes);
    }
    
    public void fireFoundJar(String jar) {
        Iterator<JspListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundJar(jar);
    }
    
    public void fireEnd() {
        Iterator<JspListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().end();
    }
    
    public void process(String file) throws IOException {
        process(new File(file));
    }
    
    public void process(File file) throws IOException {
        if (file.isDirectory())
            processDirectory(file);
        else {
            String name = file.getName();
            if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear")) {
                try ( FileInputStream fis = new FileInputStream(file);
                      ZipInputStream zis = new ZipInputStream(fis); ) {
                    processArchive(file.getAbsolutePath(), zis);
                }
            }
        }
        
        fireEnd();
    }
    
    void processArchive(String path, ZipInputStream zis) throws IOException {
        ZipEntry entry;
        String name;
        String newPath;
        ZipInputStream innerZis;
        boolean isWar = path.endsWith(".war");
        while ( (entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                name = entry.getName();
                newPath = path + "!/" + name;

                if (name.endsWith(".jar"))
                    fireFoundJar(newPath);
                
                if (name.endsWith(".jar") || name.endsWith(".war")) {
                    innerZis = new ZipInputStream(zis);
                    processArchive(newPath, innerZis);
                }
                else if (name.endsWith(".jsp"))
                    fireFoundJsp(newPath, Util.read(zis));
                else if (isWar && name.endsWith(".class"))
                    fireFoundWarClass(newPath, Util.read(zis));
            }
        }
    }
    
    void processDirectory(File directory) throws IOException {
        File files[] = Util.listFiles(directory);
        String name;
        for (int i=0; i<files.length; i++) {
            if (files[i].isFile()) {
                name = files[i].getName();
                if (name.endsWith(".jsp"))
                    fireFoundJsp(files[i].getAbsolutePath(), Util.read(files[i]));
                else if (name.endsWith(".class")) // ugh. assume war class
                    fireFoundWarClass(files[i].getAbsolutePath(), Util.read(files[i]));
                else
                    process(files[i]);
            }
            else if (files[i].isDirectory())
                processDirectory(files[i]);
        }
    }
    
    public static interface JspListener {
        public void foundJar(String jar);
        public void foundWarClass(String clazz, byte bytes[]);
        public void foundJsp(String jsp, byte bytes[]);
        public void end();
    }
}
