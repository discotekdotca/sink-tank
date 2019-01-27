package ca.discotek.sinktank.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.discotek.rebundled.org.objectweb.asm.ClassReader;
import ca.discotek.rebundled.org.objectweb.asm.tree.ClassNode;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class Classpath {
    
    public static final String CLASS_NAME_SEPARATOR = ".";
    public static final String FILE_NAME_SEPARATOR = "/";

    List<AbstractPath> classpathList = new ArrayList<AbstractPath>();
    
    public List<AbstractPath> getPaths() {
        List<AbstractPath> list = new ArrayList<AbstractPath>();
        list.addAll(classpathList);
        return list;
    }
    
    public void addPath(String path) throws IOException {
        addPath(path, true);
    }
    
    public void addPath(String path, boolean isApp) throws IOException {
        File file = new File(path);
        if (file.isDirectory())
            classpathList.add(new DirectoryPath(file, isApp));
        else
            classpathList.add(new ArchivePath(file, isApp));
    }

    /**
     * @param name Format: Class names use '.' as path separator. File names use '/' as path separator. 
     * @throws IOException 
     */
    public byte[] get(String name) throws IOException {
        Iterator<AbstractPath> it = classpathList.listIterator();
        AbstractPath path;
        byte bytes[] = null;
        while (it.hasNext()) {
            path = it.next();
            bytes = path.get(name);
            if (bytes != null)
                break;
        }
        
        return bytes;
    }
    
    static Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
    
    public ClassNode getClassNode(String name) throws IOException {
        ClassNode node = classNodeCache.get(name);
        if (node == null) {
            byte bytes[] = get(name);
            if (bytes == null)
                return null;
            node = new ClassNode();
            ClassReader cr = new ClassReader(bytes);
            cr.accept(node, ClassReader.EXPAND_FRAMES);
            classNodeCache.put(name, node);
        }
        return node;
    }
}
