package ca.discotek.sinktank.classpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ArchivePath extends AbstractPath {
    
    JarFile jar;
    
    public ArchivePath(File file) throws IOException {
        this(file, true);
    }
    
    public ArchivePath(File file, boolean isApp) throws IOException {
        super(file, isApp);
        jar = new JarFile(file);
    }
    
    public int getType() {
        return TYPE_ARCHIVE;
    }
    
    public JarEntry[] getEntries() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        JarInputStream jis = new JarInputStream(fis);
        JarEntry entry;
        
        List<JarEntry> list = new ArrayList<JarEntry>();
        while ( (entry = jis.getNextJarEntry()) != null) {
            list.add(entry);
        }
            
        return list.toArray(new JarEntry[list.size()]);
    }
    
    public byte[] get(JarEntry entry) throws IOException {
        return Util.read(jar.getInputStream(entry));
    }
    
    public byte[] get(String name) throws IOException {
        ZipEntry entry = jar.getEntry(name);
        if (entry == null)
            entry = jar.getEntry(name + ".class");
        return entry == null ? null : Util.read(entry, jar);
    }
}