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

public class ParameterAnnotationDescriptor {
    public final Method method;
    public final int parameterIndex;
    public final AnnotationNode node;
    
    public ParameterAnnotationDescriptor(int classId, int methodId, int descId, int parameterIndex, AnnotationNode node) {
        this(NodeManager.getMethod(classId, methodId, descId), parameterIndex, node);
    }
    
    public ParameterAnnotationDescriptor(Method method, int parameterIndex, AnnotationNode node) {
        this.method = method;
        this.parameterIndex = parameterIndex;
        this.node = node;
    }
    
    public int hashCode() {
        return method.hashCode() + node.desc.hashCode() + parameterIndex;
    }
    
    public boolean equals(Object o) {
        if (o instanceof ParameterAnnotationDescriptor) {
            ParameterAnnotationDescriptor pad = (ParameterAnnotationDescriptor) o;
            return pad.method.equals(method) && pad.node.desc.equals(node.desc) && pad.parameterIndex == parameterIndex;
        }
        else
            return false;
    }
}