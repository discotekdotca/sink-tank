package ca.discotek.sinktank;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import ca.discotek.rebundled.org.objectweb.asm.tree.ClassNode;

import ca.discotek.rebundled.org.objectweb.asm.ClassReader;;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ClassNodeCache {

    Map<String, ClassNode> nodeCache = new WeakHashMap<String, ClassNode>();
    
    ClassLoader loader;
    
    int classReaderOption;
    
    public ClassNodeCache(int classReaderOption) {
        this(classReaderOption, Thread.currentThread().getContextClassLoader());
    }
    
    public ClassNodeCache(int classReaderOption, ClassLoader loader) {
        this.classReaderOption = classReaderOption;
        this.loader = loader;
    }
    
    public ClassNode getNode(String name) throws IOException {
        ClassNode node = nodeCache.get(name);
        if (node == null) {
            InputStream is = loader.getResourceAsStream(name + ".class");
            if (is != null) {
                ClassReader cr = new ClassReader(is);
                node = new ClassNode();
                cr.accept(node, classReaderOption);
            }
        }
        
        return node;
    }
}
