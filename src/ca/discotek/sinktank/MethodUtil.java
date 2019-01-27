package ca.discotek.sinktank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ca.discotek.rebundled.org.objectweb.asm.ClassVisitor;
import ca.discotek.rebundled.org.objectweb.asm.Opcodes;
import ca.discotek.rebundled.org.objectweb.asm.tree.MethodNode;

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

public class MethodUtil extends ClassVisitor {

    String className;
    
    ClassNodeCache cache = new ClassNodeCache(ClassReader.SKIP_CODE);
    
    public MethodUtil() {
        super(Opcodes.ASM7);
    }
    
    public void visit(int version, int access, String name, String signature, String superName, String interfaces[]) {
        this.className = name;
    }
    
    public List<ClassNode> getSuperTypes(ClassNode node) throws IOException {
        List<ClassNode> list = new ArrayList<ClassNode>();
        getSuperTypes(node, list);
        return list;
    }
    
    public void getSuperTypes(ClassNode node, List<ClassNode> list) throws IOException {
        if (node != null && node.superName != null) {
            ClassNode superNode = cache.getNode(node.superName);
            if (superNode != null) {
                list.add(superNode);
                getSuperTypes(superNode, list);
            }
        }
    }
    
    
    public Method[] findMatchingSubtypeMethods(Set<Method> set, String classNodeName) throws IOException {
        return findMatchingSubtypeMethods(set, cache.getNode(classNodeName));
    }
    
    public Method[] findMatchingSubtypeMethods(Set<Method> set, ClassNode node) throws IOException {
        List<Method> list = new ArrayList<Method>();
        findMatchingSubtypeMethods(set, node, list);
        return list.toArray(new Method[list.size()]);
    }
    
    public void findMatchingSubtypeMethods(Set<Method> set, String classNodeName, List<Method> methodL) throws IOException {
        findMatchingSubtypeMethods(set, cache.getNode(classNodeName), methodL);
    }
    
    public void findMatchingSubtypeMethods(Set<Method> set, ClassNode node, List<Method> methodList) throws IOException {
        // first get all super types
        List<ClassNode> list = getSuperTypes(node);
        
        // determine if there is an exact match in the hierarchy
        
        Iterator<ClassNode> superIt;
        boolean foundExactMatch;
        ClassNode superNode;
        Method m;
        
        boolean foundClassName = false;
        
        Iterator<Method> setIterator = set.iterator();
        MethodNode methodNode;
        String dotName;
        while (setIterator.hasNext()) {
            foundExactMatch = false;
            m = setIterator.next();
            
            superIt = list.listIterator();
            while (superIt.hasNext()) {
                superNode = superIt.next();
                dotName = superNode.name.replace('/', '.');

                
                if (!foundExactMatch)
                    foundClassName = dotName.hashCode() == m.classId;
                if (foundClassName || foundExactMatch) {
                    Iterator<MethodNode> methodNodeIt = superNode.methods.iterator();
                    while (methodNodeIt.hasNext()) {
                        methodNode = methodNodeIt.next();
                        if (methodNode.name.hashCode() == m.memberId && methodNode.desc.hashCode() == m.descId) {
                            if (foundClassName)
                                foundExactMatch = true;
                            else
                                methodList.add(NodeManager.getMethod(dotName, methodNode.name, methodNode.desc));
                        }
                    }
                }
            }
        }
            
        if (node.superName != null)
            findMatchingSubtypeMethods(set, node.superName);
    }
    
    public static void main(String[] args) throws IOException {
        MethodUtil test = new MethodUtil();
        Set<Method> sourceSet = new HashSet<Method>();
        sourceSet.add(NodeManager.getMethod("java/io/Writer", "write", "([C)V"));
        sourceSet.add(NodeManager.getMethod("java/io/Writer", "write", "([CII)V"));
        sourceSet.add(NodeManager.getMethod("java/io/Writer", "write", "(I)V"));
        sourceSet.add(NodeManager.getMethod("java/io/Writer", "write", "(Ljava/lang/String;)V"));
        sourceSet.add(NodeManager.getMethod("java/io/Writer", "write", "(Ljava/lang/String;II)V"));
        
        
        Method m[] = test.findMatchingSubtypeMethods(sourceSet, "java/io/BufferedWriter");
        System.out.println(m);
    }
}
