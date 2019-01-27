package ca.discotek.sinktank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class NodeManager {

    static Map<Integer, Map<Integer, Map<Integer, Method>>> vertexMap = new HashMap<Integer, Map<Integer, Map<Integer, Method>>>();
    static Set<Method> nodeSet = new HashSet<Method>();
    
    static Set<Method> sourceSet = new HashSet<Method>();
    static Set<Method> sinkSet = new HashSet<Method>();
    
    public static Method getMethod(int owner, int method, int desc) {
        Map<Integer, Map<Integer, Method>> methodMap = vertexMap.get(owner);
        if (methodMap == null)
            return null;
        
        Map<Integer, Method> descMap = methodMap.get(method);
        if (descMap == null)
            return null;
        
        return descMap.get(desc);
    }
    
    public synchronized static Method getMethod(String owner, String method, String desc) {
        int ownerId = ProjectData.addClassName(owner);
        int methodId = ProjectData.addMethodName(method);
        int descId = ProjectData.addMethodDesc(desc);

        Map<Integer, Map<Integer, Method>> methodDescMap = vertexMap.get(ownerId);
        if (methodDescMap == null) {
            methodDescMap = new HashMap<Integer, Map<Integer, Method>>();
            vertexMap.put(ownerId, methodDescMap);
        }
        
        Map<Integer, Method> descMethodMap = methodDescMap.get(methodId);
        if (descMethodMap == null) {
            descMethodMap = new HashMap<Integer, Method>();
            methodDescMap.put(methodId, descMethodMap);
        }
        
        Method v = descMethodMap.get(descId);
        if (v == null) {
            v = new Method(owner.replace('/', '.'), method, desc);
            NodeManager.addMethod(v); 
            descMethodMap.put(descId, v);
            nodeSet.add(v);
        }
        
        return v;
    }
    
    public synchronized static void addMethod(Method md) {
        Map<Integer, Map<Integer, Method>> methodDescMap = vertexMap.get(md.classId);
        if (methodDescMap == null) {
            methodDescMap = new HashMap<Integer, Map<Integer, Method>>();
            vertexMap.put(md.classId, methodDescMap);
        }
        
        Map<Integer, Method> descMethodMap = methodDescMap.get(md.memberId);
        if (descMethodMap == null) {
            descMethodMap = new HashMap<Integer, Method>();
            methodDescMap.put(md.memberId, descMethodMap);
        }
        
        descMethodMap.put(md.descId, md);
        nodeSet.add(md);
    }

    public static Method[] getAppNodes() {
        Set<Method> set = new HashSet<Method>();

        set.addAll(nodeSet);
        return set.toArray(new Method[set.size()]);
    }
    
    public static Method[] getNodes() {
        return nodeSet.toArray(new Method[nodeSet.size()]);
    }
}
