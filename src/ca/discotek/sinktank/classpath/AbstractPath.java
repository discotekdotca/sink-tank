package ca.discotek.sinktank.classpath;

import java.io.File;
import java.io.IOException;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public abstract class AbstractPath {
    public static final int TYPE_DIRECTORY = 0;
    public static final int TYPE_ARCHIVE = 1;
    
    public final File file;
    
    public final boolean isApp;
    
    public AbstractPath(File file) {
        this(file, true);
    }
    
    public AbstractPath(File file, boolean isApp) {
        this.file = file;
        this.isApp = isApp;
    }
    
    public abstract int getType();
    public abstract byte[] get(String name) throws IOException;
    
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof AbstractPath))
            return false;
        else {
            AbstractPath path = (AbstractPath) o;
            return path.file.equals(file);
        }
    }
    
    public int hashCode() {
        return file.hashCode();
    }
}