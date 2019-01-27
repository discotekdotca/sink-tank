package ca.discotek.sinktank.classpath;

import java.io.File;
import java.io.IOException;

import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class DirectoryPath extends AbstractPath {
    
    public DirectoryPath(File file) {
        this(file, true);
    }
    
    public DirectoryPath(File file, boolean isApp) {
        super(file, isApp);
    }
    
    public int getType() {
        return TYPE_DIRECTORY;
    }
    
    public byte[] get(String name) throws IOException {
        String className = name.replace('.', '/') + ".class";
        File classFile = new File(file, className);
        return classFile.exists() ? Util.read(classFile) : null;
    }
    
}