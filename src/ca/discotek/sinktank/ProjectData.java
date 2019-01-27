package ca.discotek.sinktank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

public class ProjectData {
    
    static Map<Integer, String> classNameMap = new HashMap<Integer, String>();
    static Map<Integer, String> methodNameMap = new HashMap<Integer, String>();
    static Map<Integer, String> methodDescMap = new HashMap<Integer, String>();
    static Map<Integer, String> fieldNameMap = new HashMap<Integer, String>();
    static Map<Integer, String> fieldDescMap = new HashMap<Integer, String>();
    
    static Map<Integer, Set<Integer>> interfaceImplementedByMap = new HashMap<Integer, Set<Integer>>();
    static Map<Integer, Integer> superClassMap = new HashMap<Integer, Integer>();
    static Map<Integer, Set<Integer>> subClassMap = new HashMap<Integer, Set<Integer>>();
    
    static Map<Integer, Integer> superInterfaceMap = new HashMap<Integer, Integer>();
    static Map<Integer, Set<Integer>> subInterfaceMap = new HashMap<Integer, Set<Integer>>();

    static Map<Integer, Map<Integer, Set<Integer>>> abstractMethodMap = new HashMap<Integer, Map<Integer, Set<Integer>>>();
    
    static Map<Integer, String> parameterNameMap = new HashMap<Integer, String>();
    
    static Map<Integer, Map<Integer, Map<Integer, Integer[]>>> parameterNamesMap = new LinkedHashMap<Integer, Map<Integer, Map<Integer, Integer[]>>>();

    
    public static int addParameterName(String name) {
        int hashCode = name.hashCode();
        parameterNameMap.put(hashCode, name);
        return hashCode;
    }
    
    public static String getParameterName(int id) {
        return parameterNameMap.get(id);
    }

    public static void addParameterNames(int classId, int methodId, int descId, Integer names[]) {
        Map<Integer, Map<Integer, Integer[]>> methodMap = parameterNamesMap.get(classId);
        if (methodMap == null) {
            methodMap = new LinkedHashMap<Integer, Map<Integer, Integer[]>>();
            parameterNamesMap.put(classId, methodMap);
        }
        
        Map<Integer, Integer[]> descMap = methodMap.get(methodId);
        if (descMap == null) {
            descMap = new LinkedHashMap<Integer, Integer[]>();
            methodMap.put(methodId, descMap);
        }
        
        descMap.put(descId, names);

    }
    
    public static String[] getParameterNames(Method method) {
        return getParameterNames(method.classId, method.memberId, method.descId);
    }
    
    public static String[] getParameterNames(int classId, int methodId, int descId) {
        Integer ids[] = getParameterNameIds(classId, methodId, descId);
        if (ids == null)
            return null;
        else {
            String names[] = new String[ids.length];
            for (int i=0; i<ids.length; i++)
                names[i] = getParameterName(ids[i]);
            return names;
        }
    }
    
    public static Integer[] getParameterNameIds(int classId, int methodId, int descId) {
        Map<Integer, Map<Integer, Integer[]>> methodMap = parameterNamesMap.get(classId);
        if (methodMap == null) 
            return null;
        else {
            Map<Integer, Integer[]> descMap = methodMap.get(methodId);
            if (descMap == null) 
                return null;
            else
                return descMap.get(descId);
        }
    }
    
    public static void addInterfaceMethod(Integer classId, Integer methodId, Integer descId) {
        Map<Integer, Set<Integer>> methodMap = abstractMethodMap.get(classId);
        if (methodMap == null) {
            methodMap = new HashMap<Integer, Set<Integer>>();
            abstractMethodMap.put(classId, methodMap);
        }
        
        Set<Integer> set = methodMap.get(methodId);
        if (set == null) {
            set = new HashSet<Integer>();
            methodMap.put(methodId, set);
        }
        
        set.add(descId);
    }
    
    public static boolean isAbstract(Method method) {
        Map<Integer, Set<Integer>> methodMap = abstractMethodMap.get(method.classId);
        if (methodMap == null)
            return false;
        
        Set<Integer> set = methodMap.get(method.memberId);
        if (set == null) 
            return false;
        
        return set.contains(method.descId);
    }
    
    public static boolean isInterface(int classId) {
        return interfaceImplementedByMap.keySet().contains(classId);
    }

    public static int addClassName(String className) {
        int hashCode = className.hashCode();
        classNameMap.put(hashCode, className);
        return hashCode;
    }
    
    public static String getClassName(int id) {
        return classNameMap.get(id);
    }
    
    public static int addMethodName(String methodName) {
        int hashCode;
        methodNameMap.put(hashCode = methodName.hashCode(), methodName);
        return hashCode;
    }
    
    public static String getMethodName(int id) {
        return methodNameMap.get(id);
    }
    
    public static int addMethodDesc(String methodDesc) {
        int hashCode = methodDesc.hashCode();
        methodDescMap.put(hashCode, methodDesc);
        return hashCode;
    }
    
    public static String getMethodDesc(int id) {
        return methodDescMap.get(id);
    }
    
    
    public static int addFieldName(String fieldName) {
        int hashCode = fieldName.hashCode();
        fieldNameMap.put(hashCode, fieldName);
        return hashCode;
    }
    
    public static String getFieldName(int id) {
        return fieldNameMap.get(id);
    }
    
    public static int addFieldDesc(String fieldDesc) {
        int hashCode = fieldDesc.hashCode();
        fieldDescMap.put(hashCode, fieldDesc);
        return hashCode;
    }
    
    public static String getFieldDesc(int id) {
        return fieldDescMap.get(id);
    }
    
    public static void addInterfaces(String implementer, String interfaces[]) {
        int interInts[] = new int[interfaces.length];
        for (int i=0; i<interInts.length; i++) {
            interInts[i] = interfaces[i].hashCode();
            classNameMap.put(interInts[i], interfaces[i]);
        }
        addInterfaces(implementer.hashCode(), interInts);
    }
    
    public static void addInterfaces(int implementer, int interfaces[]) {
        Set<Integer> implSet;
        for (int i=0; i<interfaces.length; i++) {
            implSet = interfaceImplementedByMap.get(interfaces[i]);
            if (implSet == null) {
                implSet = new HashSet<Integer>();
                interfaceImplementedByMap.put(interfaces[i],  implSet);
            }
            
            implSet.add(implementer);
        }
    }
    
    public static Integer[] getInterfaces() {
        return interfaceImplementedByMap.keySet().toArray(new Integer[interfaceImplementedByMap.size()]);
    }
    
    public static Map.Entry<Integer, Set<Integer>>[] getInterfaceImplementers() {
        return interfaceImplementedByMap.entrySet().toArray(new Map.Entry[interfaceImplementedByMap.size()]);
    }
    
    
    public static Integer[] getImplementors(Integer interfaceClass) {
        Set<Integer> set = interfaceImplementedByMap.get(interfaceClass);
        return set == null ? new Integer[0] : set.toArray(new Integer[set.size()]);
    }
    
    public static void addSuperClass(int subclass, int superClass) {
        superClassMap.put(subclass, superClass);
        
        Set<Integer> set = subClassMap.get(superClass);
        if (set == null) {
            set = new HashSet<Integer>();
            subClassMap.put(superClass, set);
        }
        
        set.add(subclass);
    }
    
    public static Integer[] getSubClasses(int classId) {
        Set<Integer> set = subClassMap.get(classId);
        return set == null ? new Integer[0] : set.toArray(new Integer[set.size()]);
    }
    
    public static Integer getSuperClass(int subclass) {
        return superClassMap.get(subclass);
    }
    
    public static void addSuperInterface(int subInterface, int superInterface) {
        superInterfaceMap.put(subInterface, superInterface);
        
        Set<Integer> set = subInterfaceMap.get(superInterface);
        if (set == null) {
            set = new HashSet<Integer>();
            subInterfaceMap.put(superInterface, set);
        }
        
        set.add(subInterface);
    }
    
    public static Integer[] getSubInterfaces(int interfaceId) {
        Set<Integer> set = subInterfaceMap.get(interfaceId);
        return set == null ? new Integer[0] : set.toArray(new Integer[set.size()]);
    }
    
    public static Integer getInterfaceClass(int subInterface) {
        return superInterfaceMap.get(subInterface);
    }
}
