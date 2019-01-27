package ca.discotek.sinktank;

import ca.discotek.rebundled.org.objectweb.asm.tree.AnnotationNode;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class MethodAnnotationDescriptor {
    public final Method method;
    public final AnnotationNode node;
    
    public MethodAnnotationDescriptor(int classId, int methodId, int descId, AnnotationNode node) {
        this(NodeManager.getMethod(classId, methodId, descId), node);
    }
    
    public MethodAnnotationDescriptor(Method method, AnnotationNode node) {
        this.method = method;
        this.node = node;
    }
    
    public int hashCode() {
        return method.hashCode() + node.desc.hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof MethodAnnotationDescriptor) {
            MethodAnnotationDescriptor mad = (MethodAnnotationDescriptor) o;
            return mad.method.equals(method) && mad.node.desc.equals(node.desc);
        }
        else
            return false;
    }
}