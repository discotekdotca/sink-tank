package ca.discotek.sinktank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.discotek.rebundled.org.objectweb.asm.AnnotationVisitor;
import ca.discotek.rebundled.org.objectweb.asm.ClassReader;
import ca.discotek.rebundled.org.objectweb.asm.ClassVisitor;
import ca.discotek.rebundled.org.objectweb.asm.Label;
import ca.discotek.rebundled.org.objectweb.asm.MethodVisitor;
import ca.discotek.rebundled.org.objectweb.asm.Opcodes;
import ca.discotek.rebundled.org.objectweb.asm.Type;
import ca.discotek.rebundled.org.objectweb.asm.tree.AnnotationNode;
import ca.discotek.sinktank.classpath.AbstractPath;
import ca.discotek.sinktank.dijkstra.Edge;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class PathUtil {
    
    public final File file;
    
    public PathUtil(File file) {
        this.file = file;
    }
    
    Set<Edge> edgeSet = Collections.synchronizedSet(new HashSet<Edge>());
    Set<Method> methodSet = Collections.synchronizedSet(new HashSet<Method>());
    Set<Method> definedMethodSet = Collections.synchronizedSet(new HashSet<Method>());
    
    static Set<AnnotationNode> allAnnotationSet = new LinkedHashSet<AnnotationNode>();
    static Map<Integer, List<AnnotationNode>> classAnnotationMap = new LinkedHashMap<Integer, List<AnnotationNode>>();
    List<MethodAnnotationDescriptor> methodAnnotationDescriptorList = new ArrayList<>();
    List<ParameterAnnotationDescriptor> parameterAnnotationDescriptorList = new ArrayList<>();
    
    public Edge[] getEdges() {
        return edgeSet.toArray(new Edge[edgeSet.size()]);
    }
    
    void addEdge(Edge edge) {
        edgeSet.add(edge);
    }
    
    public void addMethodAnnotation(int classId, int methodId, int descId, AnnotationNode node) {
        methodAnnotationDescriptorList.add(new MethodAnnotationDescriptor(classId, methodId, descId, node));
        registerAnnotation(node);
    }
    
    public void addParameterAnnotation(int classId, int methodId, int descId, int parameterIndex, AnnotationNode node) {
        parameterAnnotationDescriptorList.add(new ParameterAnnotationDescriptor(classId, methodId, descId, parameterIndex, node));
        registerAnnotation(node);
    }
    
    void registerAnnotation(AnnotationNode node) {
        allAnnotationSet.add(node);
        AnnotationNode innerNodes[] = AsmUtil.getInnerNodes(node);
        allAnnotationSet.addAll(Arrays.asList(innerNodes));
    }
    
    public MethodAnnotationDescriptor[] getMethodAnnotations() {
        return methodAnnotationDescriptorList.toArray(new MethodAnnotationDescriptor[methodAnnotationDescriptorList.size()]);
    }
    
    public ParameterAnnotationDescriptor[] getParameterAnnotations() {
        return parameterAnnotationDescriptorList.toArray(new ParameterAnnotationDescriptor[parameterAnnotationDescriptorList.size()]);
    }
    
    public Set<Method> getAllMethods() {
        return methodSet;
    }
    
    public Set<Method> getDefinedMethods() {
        return definedMethodSet;
    }
    
    public void process() throws IOException {
        Set<AbstractPath> processedRootPathSet = new HashSet<AbstractPath>();

        InputStream is;
        if (file.isDirectory()) {
            File files[] = Util.listFiles(file, Pattern.compile(".*\\.class"));
            for (int i=0; i<files.length; i++) {
                if (files[i].getName().endsWith(".class")) {
                    is = new FileInputStream(files[i]);
                    processClass(is);
                    is.close();
                }
            }
        }
        else if (file.isFile()) {
            String name = file.getName();
            if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear"))
                processArchive(file);
        }
        else
            System.out.println(file.getAbsolutePath() + " is neither a valid file or directory.");
            
        try { 
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS); 
        } 
        catch (InterruptedException e) {
            // should never happen.
            throw new RuntimeException("Bug.", e);
        }
        
        Method methods[] = methodSet.toArray(new Method[methodSet.size()]);
        for (int i=0; i<methods.length; i++) {
            if (ProjectData.isAbstract(methods[i])) {
                if (ProjectData.isInterface(methods[i].classId))
                    createInterfaceMethods(methods[i]);
                else
                    createAbstractMethods(methods[i]);
            }
        }
        
    }
    
    void createInterfaceMethods(Method method) {
        Integer implementers[] = ProjectData.getImplementors(method.classId);
        for (int i=0; i<implementers.length; i++) 
            createInterfaceMethods(method, implementers[i]);
    }
    
    void createInterfaceMethods(Method method, Integer implementer) {
        Method implMethod = NodeManager.getMethod(implementer, method.memberId, method.descId);
        
        if (implMethod == null) {
            createSubClassInterfaceMethods(method, implementer);
            createSuperClassInterfaceMethods(method, implementer);
        }
        else
            addEdge(new VirtualEdge(method, implMethod));
    }
    
    void createSubClassInterfaceMethods(Method method, Integer implementer) {
        Integer subclasses[] = ProjectData.getSubClasses(implementer);
        Method implMethod;
        for (int i=0; i<subclasses.length; i++) {
            implMethod = NodeManager.getMethod(subclasses[i], method.memberId, method.descId);
            if (implMethod == null)
                createSuperClassInterfaceMethods(method, subclasses[i]);
            else 
                addEdge(new VirtualEdge(method, implMethod));
        }
    }
    
    void createSuperClassInterfaceMethods(Method method, Integer implementer) {
        Integer cursor = implementer;
        
        Integer superclass;
        while (cursor != null) {
            superclass = ProjectData.getSuperClass(cursor);
            if (superclass == null)
                break;
            Method implMethod = NodeManager.getMethod(superclass, method.memberId, method.descId);
            if (implMethod == null) 
                cursor = superclass;
            else {
                addEdge(new VirtualEdge(method, implMethod));
                break;
            }            
        }
    }
    
    void createAbstractMethods(Method method) {
        Integer subclasses[] = ProjectData.getSubClasses(method.classId);
        for (int i=0; i<subclasses.length; i++)
            createAbstractMethods(method, subclasses[i]);
    }
    
    void createAbstractMethods(Method method, int subclass) {
        Method implMethod = NodeManager.getMethod(subclass, method.memberId, method.descId);
        if (implMethod == null) {
            Integer subclasses[] = ProjectData.getSubClasses(subclass);
            for (int i=0; i<subclasses.length; i++)
                createAbstractMethods(method, subclasses[i]);
        }
        else
            addEdge(new VirtualEdge(method, implMethod));
    }
    
    void processArchive(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file); ) {
            processArchive(fis);
        }
    }
    
    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    void processArchive(InputStream is) throws IOException {
        final ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        String name;
        Callable<Object> c;
        while ( (entry = zis.getNextEntry()) != null) {
            name = entry.getName();
            if (name.endsWith(".jar") || name.endsWith(".war")) {
                final byte bytes[] = Util.read(zis);
                c = new Callable<Object>() {
                    public String call() throws Exception {
                        processArchive(new ByteArrayInputStream(bytes));
                        return null;
                    }
                };
                service.submit(c);
            }
            else if (name.endsWith(".class"))
                processClass(zis);
        }
    }
    
    
    void processClass(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        MyClassVisitor cv = new MyClassVisitor();
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
    }
    
    class MyClassVisitor extends ClassVisitor {
        
        String className;
        int classId;
        String interfaces[];
        String superClass;

        boolean isAbstract;
        
        public MyClassVisitor() {
            super(Opcodes.ASM7);
        }
        
        public void visit(int version,
                int access,
                java.lang.String name,
                java.lang.String signature,
                java.lang.String superName,
                java.lang.String[] interfaces) {
            this.className = name.replace('/', '.');
            this.classId = className.hashCode();
            
            this.superClass = superName == null ? null : superName.replace('/', '.');
            this.interfaces = interfaces == null ? new String[0] : interfaces;
            for (int i=0; i<interfaces.length; i++)
                this.interfaces[i] = this.interfaces[i].replace('/', '.');
            
            if (this.superClass != null)
                ProjectData.addSuperClass(this.className.hashCode(), this.superClass.hashCode());
            
            if (interfaces != null && interfaces.length > 0 && (Opcodes.ACC_INTERFACE & access) == 0)
                ProjectData.addInterfaces(this.className, interfaces);
        
            isAbstract = (Opcodes.ACC_ABSTRACT & access) != 0;
        }
        
        public MethodVisitor visitMethod(int access,
                java.lang.String name,
                java.lang.String descriptor,
                java.lang.String signature,
                java.lang.String[] exceptions) {
            
            if (isAbstract && ((Opcodes.ACC_ABSTRACT & access) != 0) && !name.startsWith("["))
                ProjectData.addInterfaceMethod(className.hashCode(), name.hashCode(), descriptor.hashCode());

            Method method = NodeManager.getMethod(MyClassVisitor.this.className, name, descriptor);
            methodSet.add(method);
            definedMethodSet.add(method);
            
            Type methodType = Type.getType(descriptor);
            int parameterCount = methodType.getArgumentTypes().length;
            return new MyMethodVisitor(parameterCount, method, (Opcodes.ACC_STATIC & access) != 0);
        }
    }
    
    class MyMethodVisitor extends MethodVisitor {

        Method source;
        boolean isStatic;

        List<Integer> parameterNameList = null;
        int parameterCount;
        
        public MyMethodVisitor(int parameterCount, Method source, boolean isStatic) {
            super(Opcodes.ASM7);

            this.parameterCount = parameterCount;
            this.source = source;
            this.isStatic = isStatic;
        }
        
        public AnnotationVisitor visitAnnotation(java.lang.String descriptor, boolean visible) {
            AnnotationNode node = new AnnotationNode(Opcodes.ASM7, descriptor);
            addMethodAnnotation(source.classId, source.memberId, source.descId, node);
            return node;
        }
        
        public AnnotationVisitor visitParameterAnnotation(int parameter, java.lang.String descriptor, boolean visible) {
            AnnotationNode node = new AnnotationNode(Opcodes.ASM7, descriptor);
            addParameterAnnotation(source.classId, source.memberId, source.descId, parameter, node);
            return node;
        }
        
        public void visitLocalVariable(java.lang.String name,
                java.lang.String descriptor,
                java.lang.String signature,
                Label start,
                Label end,
                int index) {
            
            if (!isStatic && index == 0) { /* do nothing. don't care about "this" */ }
            else if ((isStatic ? index : index - 1) < parameterCount){
                if (parameterNameList == null)
                    parameterNameList = new ArrayList<Integer>();
                parameterNameList.add(ProjectData.addParameterName(name));
            }
        }
        
        public void visitMethodInsn(int opcode,
                java.lang.String owner,
                java.lang.String name,
                java.lang.String descriptor,
                boolean isInterface) {
            
            if (!owner.startsWith("[")) {
                Method destination = NodeManager.getMethod(owner.replace('/', '.'), name, descriptor);
                methodSet.add(destination);
                addEdge(new Edge(source, destination));
            }
        }
        
        public void visitEnd() {
            if (parameterNameList != null)
                ProjectData.addParameterNames(source.classId, source.memberId, source.descId, parameterNameList.toArray(new Integer[parameterNameList.size()]));
        }
    }

}
