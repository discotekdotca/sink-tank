package ca.discotek.sinktank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import ca.discotek.rebundled.org.objectweb.asm.AnnotationVisitor;
import ca.discotek.rebundled.org.objectweb.asm.ClassReader;
import ca.discotek.rebundled.org.objectweb.asm.ClassVisitor;
import ca.discotek.rebundled.org.objectweb.asm.MethodVisitor;
import ca.discotek.rebundled.org.objectweb.asm.Opcodes;
import ca.discotek.sinktank.classpath.AbstractPath;
import ca.discotek.sinktank.classpath.ArchivePath;
import ca.discotek.sinktank.classpath.Classpath;
import ca.discotek.sinktank.classpath.DirectoryPath;
import ca.discotek.sinktank.dijkstra.DijkstraAlgorithm;
import ca.discotek.sinktank.gui.ProgressSupport;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class PathAnalyzer2 {
    
    static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    static final File EXPLODE_DIR = new File(TMP_DIR, "explode");
    
    Classpath classpath = new Classpath();
    
    File dependencyClasspath[] = null;
    File applicationClasspath[] = null;
    
    List<File> pathList = new ArrayList<File>();
    
    List<String> classpathUrlList = new ArrayList<String>();
    Stack<String> pathStack = new Stack<String>();
    
    DescriptorParser parser = null;
    
    Map<Method, Set<Method>> sourceNeighborMap = new HashMap<Method, Set<Method>>();
    Map<Method, Set<Method>> sinkNeighborMap = new HashMap<Method, Set<Method>>();

    List<TaintListener> listenerList = new ArrayList<>();
    
    Configuration configuration;
    
    Set<Method> unvalidatedSourceMethodSet = new HashSet<Method>();
    Set<Method> unvalidatedSinkMethodSet = new HashSet<Method>();

    Set<String> unvalidatedSourceMethodAnnotationSet = new HashSet<String>();
    Set<String> unvalidatedSinkMethodAnnotationSet = new HashSet<String>();
    
    Set<String> unvalidatedSourceParameterAnnotationSet = new HashSet<String>();
    Set<String> unvalidatedSinkParameterAnnotationSet = new HashSet<String>();
    
    Map<Method, List<Method>> sourceOriginMap = new HashMap<Method, List<Method>>();
    Map<Method, List<Method>> sinkOriginMap = new HashMap<Method, List<Method>>();
    
    Map<Method, List<String>> sourceMethodAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sinkMethodAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sourceMethodParameterAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sinkMethodParameterAnnotationMap = new HashMap<Method, List<String>>();
        
    public void addListener(TaintListener l) {
        listenerList.add(l);
    }
    
    public void fireDefinedSource(Method m) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSourceMethod(m);
    }
    
    public void fireDefinedSink(Method m) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSinkMethod(m);
    }
    
    public synchronized void fireFoundSource(List<Method> list) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundSource(list);
    }
    
    public synchronized void fireFoundSink(List<Method> list) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundSink(list);
    }
    
    public synchronized void fireFoundSourceSink(List<Method> sourceList, List<Method> sinkList) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().foundSourceSink(sourceList, sinkList);
    }
    
    public void addEdge(Method source, Method destination) {
        addEdge(source, destination, false);
    }
    
    boolean isPrimitiveNumberType(int id) {
        String className = ProjectData.getClassName(id);
        return  className.equals("java.lang.Byte") ||
                className.equals("java.lang.Boolean") ||
                className.equals("java.lang.Character") ||
                className.equals("java.lang.Double") ||
                className.equals("java.lang.Float") ||
                className.equals("java.lang.Integer") ||
                className.equals("java.lang.Long") ||
                className.equals("java.lang.Short");
    }
    
    public void addEdge(Method source, Method destination, boolean isVirtual) {
        
        if (isPrimitiveNumberType(source.classId))
            return;
        else if (isPrimitiveNumberType(destination.classId))
            return;
        
        {
            Set<Method> set = sourceNeighborMap.get(source);
            if (set == null) {
                set = new HashSet<Method>();
                sourceNeighborMap.put(source, set);
            }
            
            set.add(destination);
        }
        {
        Set<Method> set = sinkNeighborMap.get(source);
        if (set == null) {
            set = new HashSet<Method>();
            sinkNeighborMap.put(source, set);
        }
        
        set.add(destination);
        }
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
            
            this.superClass = superName == null ? null : superName.replace('/', '.');
            this.interfaces = interfaces == null ? new String[0] : interfaces;
            for (int i=0; i<interfaces.length; i++)
                this.interfaces[i] = this.interfaces[i].replace('/', '.');
            
            if (this.superClass != null) {
                if ((access & Opcodes.ACC_INTERFACE) != 0) {
                    for (int j=0; j<this.interfaces.length; j++)
                        ProjectData.addSuperInterface(this.className.hashCode(), this.interfaces[j].hashCode());
                }
                else
                    ProjectData.addSuperClass(this.className.hashCode(), this.superClass.hashCode());
            }
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
            if (unvalidatedSourceMethodSet.contains(method))
                unvalidatedSourceMethodSet.remove(method);
            
            if (unvalidatedSinkMethodSet.contains(method))
                unvalidatedSinkMethodSet.remove(method);
            
            return new MyMethodVisitor(method);
        }
    }
    
    void addMethodAnnotation(Method method, String annotation, Map<Method, List<String>> map) {
        List list = map.get(method);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(method,  list);
        }
        
        list.add(annotation);
    }
    
    class MyMethodVisitor extends MethodVisitor {

        Method method;
        
        public MyMethodVisitor(Method method) {
            super(Opcodes.ASM7);

            this.method = method;
        }
        
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

            String s = AsmUtil.toTypeString(desc);
            if (configuration.sourceMethodAnnotationList.contains(s)) {
                sourceMethodSet.add(method);
                addMethodAnnotation(method, s, sourceMethodAnnotationMap);
            }
            else if (configuration.sinkMethodAnnotationList.contains(s)) {
                sinkMethodSet.add(method);
                addMethodAnnotation(method, s, sinkMethodAnnotationMap);
            }
            
            if (unvalidatedSourceMethodAnnotationSet.contains(s))
                unvalidatedSourceMethodAnnotationSet.remove(s);
            
            if (unvalidatedSinkMethodAnnotationSet.contains(s))
                unvalidatedSinkMethodAnnotationSet.remove(s);
            
            
            return null;
        }
        
        public AnnotationVisitor visitParameterAnnotation(int parameter, java.lang.String descriptor, boolean visible) {
            String s = AsmUtil.toTypeString(descriptor);
            if (configuration.sourceParameterAnnotationList.contains(s)) {
                sourceMethodSet.add(method);
                addMethodAnnotation(method, s, sourceMethodParameterAnnotationMap);
            }
            else if (configuration.sinkParameterAnnotationList.contains(s)) {
                sinkMethodSet.add(method);
                addMethodAnnotation(method, s, sinkMethodParameterAnnotationMap);
            }
            
            if (unvalidatedSourceParameterAnnotationSet.contains(s))
                unvalidatedSourceParameterAnnotationSet.remove(s);
            
            if (unvalidatedSinkParameterAnnotationSet.contains(s))
                unvalidatedSinkParameterAnnotationSet.remove(s);
            
            return null;
        }
        
        public void visitMethodInsn(int opcode,
                java.lang.String owner,
                java.lang.String name,
                java.lang.String descriptor,
                boolean isInterface) {
            
            if (!owner.startsWith("[")) {
                Method destination = NodeManager.getMethod(owner.replace('/', '.'), name, descriptor);
                addEdge(method, destination);
            }
        }
    }


    public PathAnalyzer2(Configuration configuration) {
        this.configuration = configuration;
    }
    
    
    public void addApplication(String app) throws IOException {
        addApplication(new File(app));
    }
    
    public void addApplication(File app) throws IOException {
        if (app.exists()) {
            
            if (app.isDirectory())
                addAppDirectory(app);
            else {
                String name = app.getName();
                
                if (name.endsWith(".jar"))
                    addAppJar(app);
                else if (name.endsWith(".war"))
                    addAppWar(app);
                else if (name.endsWith(".ear"))
                    addAppEar(app);
                else
                    throw new IllegalArgumentException("Unsupported file type: " + app.getPath());
            }
        }
        else
            throw new IllegalArgumentException("File does not exist: " + app.getPath());
    }
    
    public void addAppDirectory(File directory) throws IOException {
        classpath.addPath(directory.getAbsolutePath());
    }
    
    public void addAppJar(File file) throws IOException {
        classpath.addPath(file.getAbsolutePath());
    }
    
    public void addSupportJar(File file) throws IOException {
        classpath.addPath(file.getAbsolutePath(), false);
    }
    
    public void addAppWar(File file) throws IOException {
        addAppWar(file, true);
    }
    
    public void addAppWar(File file, boolean processOnlyJeeStructure) throws IOException {
        File explodeRoot = new File(EXPLODE_DIR, file.getName());
        ExplodedWar explodedWar = ExplodedWar.explodeWar(file, explodeRoot, false);
        classpath.addPath(processOnlyJeeStructure ? explodedWar.getWebInfClassesDir().getAbsolutePath() : explodedWar.getRootDir().getAbsolutePath());
        
        File warLibs[] = explodedWar.getWebInfLibJars();
        for (int j=0; j<warLibs.length; j++) {
            classpath.addPath(warLibs[j].getAbsolutePath());
        }
        
        if (!processOnlyJeeStructure) {
            File otherLibs[] = explodedWar.getOtherLibJars();
            for (int j=0; j<otherLibs.length; j++) {
              classpath.addPath(otherLibs[j].getAbsolutePath());
          }
        }
    }
    
    public void addAppEar(File file) {
        throw new UnsupportedOperationException();
    }
    
    Set<Method> sourceMethodSet = new HashSet<Method>();
    Set<Method> sinkMethodSet = new HashSet<Method>();
    
    void resetSourceSinkSets() {
        sourceMethodSet.clear();
        sinkMethodSet.clear();
        
        unvalidatedSourceMethodSet.clear();
        unvalidatedSourceMethodAnnotationSet.clear();
        unvalidatedSourceParameterAnnotationSet.clear();
        unvalidatedSinkMethodSet.clear();
        unvalidatedSinkMethodAnnotationSet.clear();
        unvalidatedSinkParameterAnnotationSet.clear();
        
        unvalidatedSourceMethodSet.addAll(Arrays.asList(configuration.getSourceMethods()));
        unvalidatedSourceMethodAnnotationSet.addAll(Arrays.asList(configuration.getSourceMethodAnnotations()));
        unvalidatedSourceParameterAnnotationSet.addAll(Arrays.asList(configuration.getSourceParameterAnnotations()));
        unvalidatedSinkMethodSet.addAll(Arrays.asList(configuration.getSinkMethods()));
        unvalidatedSinkMethodAnnotationSet.addAll(Arrays.asList(configuration.getSinkMethodAnnotations()));
        unvalidatedSinkParameterAnnotationSet.addAll(Arrays.asList(configuration.getSinkParameterAnnotations()));
    }
    
    public boolean hasUnvalidatedSourceSinks() {
        return 
            unvalidatedSourceMethodSet.size() > 0 ||
            unvalidatedSourceMethodAnnotationSet.size() > 0 ||
            unvalidatedSourceParameterAnnotationSet.size() > 0 ||
            unvalidatedSinkMethodSet.size() > 0 ||
            unvalidatedSinkMethodAnnotationSet.size() > 0 ||
            unvalidatedSinkParameterAnnotationSet.size() > 0;
    }
    
    public Classpath getClasspath() {
        return classpath;
    }

   
    public void processClasspath() throws IOException {
        processClasspath(false);
    }
    
    
    
    public void processClasspath(boolean reset) throws IOException {
        if (reset) {
            pathStack.clear();
            classpathUrlList.clear();
        }
        resetSourceSinkSets();
        Set<AbstractPath> processedRootPathSet = new HashSet<AbstractPath>();
        
        Iterator<AbstractPath> it = classpath.getPaths().listIterator();
        AbstractPath path;
        InputStream is;
        while (it.hasNext()) {
            path = it.next();
            if (processedRootPathSet.contains(path)) {
                System.out.println("Found duplicate of " + path + ". Skipping.");
                continue;
            }
            else
                processedRootPathSet.add(path);
            if (path instanceof DirectoryPath) {
                classpathUrlList.add(path.file.toURI().toURL().toExternalForm());
                File files[] = Util.listFiles(path.file);
                for (int i=0; i<files.length; i++) {
                    if (files[i].getName().endsWith(".class")) {
                        is = new FileInputStream(files[i]);
                        processClass(is);
                        is.close();
                    }
                }
            }
            else if (path instanceof ArchivePath) {
                ArchivePath archivePath = (ArchivePath) path;
                System.out.println("Processing : " + archivePath.file.getName());
                
                classpathUrlList.add(path.file.toURI().toURL().toExternalForm());

                JarEntry entries[] = archivePath.getEntries();
                byte bytes[];
                String name;
                for (int i=0; i<entries.length; i++) {
                    name = entries[i].getName();
                    if (entries[i].isDirectory())
                        continue;
                    else if (name.endsWith(".class")) {
                        bytes = archivePath.get(entries[i]);
                        is = new ByteArrayInputStream(bytes);
                        processClass(is);                        
                        is.close();
                    }
                    else if (name.endsWith(".jar")) {
                        URL url = new File(path.file.getAbsolutePath() + "!/" + name).toURI().toURL();
                        classpathUrlList.add(url.toExternalForm());
                        pathStack.push(url.getFile());
                        bytes = archivePath.get(entries[i]);
                        final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
                        final String file = path.file.getAbsolutePath();
                        final String finalName = name;
                        processArchive(zis, file + "!/" + finalName);
                        zis.close();
                    }
                }
            }
        }
        

        Method methods[] = NodeManager.getNodes();
        for (int i=0; i<methods.length; i++) {
            if (ProjectData.isAbstract(methods[i])) {
                if (ProjectData.isInterface(methods[i].classId))
                    createInterfaceMethods(methods[i]);
                else
                    createAbstractMethods(methods[i]);
            }
        }
        
        sourceMethodSet.addAll(configuration.sourceMethodList);
        sinkMethodSet.addAll(configuration.sinkMethodList);
        
        findVirtualSourceSinkMethods(sourceMethodSet);
        findVirtualSourceSinkMethods(sinkMethodSet);
    }
    
    /*
     *     Map<Method, List<String>> sourceMethodAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sinkMethodAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sourceMethodParameterAnnotationMap = new HashMap<Method, List<String>>();
    Map<Method, List<String>> sinkMethodParameterAnnotationMap = new HashMap<Method, List<String>>();
     */
    
    public Map<Method, List<String>> getSourceMethodAnnotationMap() {
        return sourceMethodAnnotationMap;
    }
    
    public Map<Method, List<String>> getSinkMethodAnnotationMap() {
        return sinkMethodAnnotationMap;
    }
    
    public Map<Method, List<String>> getSourceMethodParameterAnnotationMap() {
        return sourceMethodParameterAnnotationMap;
    }
    
    public Map<Method, List<String>> getSinkMethodParameterAnnotationMap() {
        return sinkMethodParameterAnnotationMap;
    }
    
    public Map<Method, List<Method>> getSourceOriginMap() {
        return sourceOriginMap;
    }
    
    public Map<Method, List<Method>> getSinkOriginMap() {
        return sinkOriginMap;
    }
    
    void addOrigin(Method method, Method originMethod, Set<Method> set) {
        if (method == originMethod)
            return;
        Map<Method, List<Method>> originMap = set == sourceMethodSet ? sourceOriginMap : sinkOriginMap;
        List<Method> list = originMap.get(method);
        if (list == null) {
            list = new ArrayList<Method>();
            originMap.put(method, list);
        }
        
        list.add(originMethod);
    }
    
    void findVirtualSourceSinkSubtypeMethods(Method method, Set<Method> sourceSinkMethodSet, Method originMethod) {
        Integer classIds[] = ProjectData.getSubClasses(method.classId);
        Method m;
        for (int i=0; i<classIds.length; i++) {
            m = NodeManager.getMethod(classIds[i], method.memberId, method.descId);
            if (m != null) {
                addOrigin(m, originMethod, sourceSinkMethodSet);
                sourceSinkMethodSet.add(m);
            }
        }
    }
    
    void findVirtualSourceSinkInterfaceImplementorsMethods(Method method, Set<Method> sourceSinkMethodSet, Method originMethod) {
        Set<Integer> classIdSet = new HashSet<Integer>();
        Integer classIds[] = ProjectData.getImplementors(method.classId);
        classIdSet.addAll(Arrays.asList(classIds));
        
        Integer subinterfaces[] = ProjectData.getSubInterfaces(method.classId);
        for (int i=0; i<subinterfaces.length; i++) {
            classIds = ProjectData.getImplementors(subinterfaces[i]);
            classIdSet.addAll(Arrays.asList(classIds));
            classIdSet.add(subinterfaces[i]);
        }
        
        Method m;
        Iterator<Integer> it = classIdSet.iterator();
        Integer id;
        while (it.hasNext()) {
            id = it.next();
            m = NodeManager.getMethod(id, method.memberId, method.descId);
            
            if (m != null) {
                addOrigin(m, originMethod, sourceSinkMethodSet);
                sourceSinkMethodSet.add(m);
                findVirtualSourceSinkSubtypeMethods(m, sourceSinkMethodSet, originMethod);
            }
        }
    }
    
    void findVirtualSourceSinkMethods(Set<Method> sourceSinkMethodSet) {
        Method methods[] = sourceSinkMethodSet.toArray(new Method[sourceSinkMethodSet.size()]);
        for (int i=0; i<methods.length; i++) {
            if (ProjectData.isInterface(methods[i].classId)) 
                findVirtualSourceSinkInterfaceImplementorsMethods(methods[i], sourceSinkMethodSet, methods[i]);
            else
                findVirtualSourceSinkSubtypeMethods(methods[i], sourceSinkMethodSet, methods[i]);
        }
    }
    
    public String[] getProcessedUrls() {
        return classpathUrlList.toArray(new String[classpathUrlList.size()]);
    }
    
    void processArchive(ZipInputStream zis, final String archiveName) throws IOException {
        System.out.println("Processing: " + archiveName);
        ZipEntry entry = null;
        String name;
        while ( (entry = zis.getNextEntry()) != null) {
            name = entry.getName();
            if (entry.isDirectory())
                continue;
            else if (name.endsWith(".class"))
                processClass(zis); 
            else if (name.endsWith(".jar")) {
//                File file = new File(pathStack.peek());
//                file = new File(file, "!/" + name);
                URL url = new File(pathStack.peek() + "!/" + name).toURI().toURL();
                classpathUrlList.add(url.toExternalForm());
                pathStack.push(url.getFile());
                processArchive(new ZipInputStream(zis), archiveName + "!/" + name);
                pathStack.pop();
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
            addEdge(method, implMethod, true);
        
    }
    
    void createSubClassInterfaceMethods(Method method, Integer implementer) {
        Integer subclasses[] = ProjectData.getSubClasses(implementer);
        Method implMethod;
        for (int i=0; i<subclasses.length; i++) {
            implMethod = NodeManager.getMethod(subclasses[i], method.memberId, method.descId);
            if (implMethod == null)
                createSuperClassInterfaceMethods(method, subclasses[i]);
            else
                addEdge(method, implMethod, true);
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
                addEdge(method, implMethod, true);
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
            addEdge(method, implMethod, true);
    }
    
    void processClass(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        MyClassVisitor cv = new MyClassVisitor();
        cr.accept(cv, ClassReader.SKIP_DEBUG);
    }
    
    public void findPaths(Map<Character, Set<String>> namespaceMap) {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            Method nodes[] = NodeManager.getAppNodes();
            
            String className;
            String packageName;
            int index;
            Set<Method> nodeSet = new HashSet<Method>();
            Method filteredNodes[];
            Set<String> set;
            Iterator<String> it;
            String packagePattern;
            if (namespaceMap != null) {
                for (int i=0; i<nodes.length; i++) {
                    className = ProjectData.getClassName(nodes[i].classId);
                    index = className.lastIndexOf('.');
                    packageName = index < 0 ? "<empty>" : className.substring(0, index);
                    set = namespaceMap.get(packageName.charAt(0));
                    if (set != null) {
                        it = set.iterator();
                        while (it.hasNext()) {
                            packagePattern = it.next();
                            if (packageName.startsWith(packagePattern))
                                nodeSet.add(nodes[i]);
                        }
                    }
                }
                filteredNodes = nodeSet.toArray(new Method[nodeSet.size()]);
            }
            else
                filteredNodes = nodes;

            
            
            
            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            
            int divided = filteredNodes.length / cores;
            int size = divided;
            
            final Set<List<Method>> sourceListSet = Collections.synchronizedSet(new HashSet<List<Method>>());
            
            List<Callable<Object>> sinkJobList = new ArrayList<Callable<Object>>();
            
            for (int i=0; i<=cores; i++) {
                if (i == cores)
                    size = filteredNodes.length % cores;
                final Method threadNodes[] = new Method[size];
                System.arraycopy(filteredNodes, i * divided, threadNodes, 0, size);

                Callable sourceCallable = new Callable() {
                    public Object call() {
                        findSources(threadNodes, sourceListSet);
                        return null;
                    }
                };
                
                Callable sinkCallable = new Callable() {
                    public Object call() {
                        findSinks(threadNodes, sourceListSet);
                        return null;
                    }
                };
                sinkJobList.add(sinkCallable);
                
                service.submit(sourceCallable);
            }
            
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
            
            service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            service.invokeAll(sinkJobList);
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void findSources(final Method nodes[], final Set<List<Method>> sourceListSet) {
        final DijkstraAlgorithm sourceDijkstra = new DijkstraAlgorithm(sourceNeighborMap);
        
        Callable c = new Callable() {
            public Object call() throws Exception {
                Iterator<Method> sourceIt;
                Method sourceMd;
                List<Method> sourceList; 
                
                for (int i=0; i<nodes.length; i++) {                    
                    sourceDijkstra.execute(nodes[i]);
                    sourceIt = sourceMethodSet.iterator();
                    while (sourceIt.hasNext()) {
                        sourceMd = sourceIt.next();
                        sourceList = sourceDijkstra.getPath(sourceMd);
                        if (sourceList != null) {
                            fireFoundSource(sourceList);
                            sourceListSet.add(sourceList);
                        }
                    }
                    
                    ProgressSupport.INSTANCE.fireUpdate(this, i);
                }
                ProgressSupport.INSTANCE.fireFinished(this);

                return null;
            }
        };

        ProgressSupport.INSTANCE.fireStart(c, 1, nodes.length-1, "Finding Sources");
        
        
        try {
            c.call();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    boolean isValidPath(List<Method> pathList, String token, boolean isSource) {
        ListIterator<Method> it = pathList.listIterator();
        Method m;
        String desc;
        while (it.hasNext()) {
            if (isSource && !it.hasPrevious()) {
                it.next();
                continue;
            }

            m = it.next();
            
            if (!isSource && !it.hasNext())
                continue;
            else {
                desc = ProjectData.getMethodDesc(m.descId);
                if (desc.contains(token))
                    return false;
            }
        }
        
        return true;
    }
    
    boolean isValidSourcePath(List<Method> pathList) {
        return isValidPath(pathList, ")V", true);
    }
    
    boolean isValidSinkPath(List<Method> pathList) {
        return isValidPath(pathList, "()", false);
    }
    
    public void findSinks(final Method nodes[], final Set<List<Method>> sourceListSet) {
        try {

            Callable c = new Callable() {
                public Object call() throws Exception {
                    List<Method> sourceList;
                    List<Method> sinkList;
                    Iterator<Method> sinkIt;
                    Method sinkMd;
                    
                    Set<Method> cloneSet = new HashSet<Method>();
                    Iterator<List<Method>> it;
                    DijkstraAlgorithm sinkDijkstra = new DijkstraAlgorithm(sinkNeighborMap);
                    for (int i=0; i<nodes.length; i++) {
                        
                        if (sourceMethodSet.contains(nodes[i]) && sinkMethodSet.contains(nodes[i]) ) {
                            List<Method> list = new ArrayList<>();
                            list.add(nodes[i]);
                            fireFoundSourceSink(list,  list);
                        }
                        else {
                            sinkDijkstra.execute(nodes[i]);
                            sinkIt = sinkMethodSet.iterator();
                            while (sinkIt.hasNext()) {
                                sinkMd = sinkIt.next();
                                sinkList = sinkDijkstra.getPath(sinkMd);
                                
                                if (sinkList != null) {
//                                    if (!isValidSinkPath(sinkList))
//                                        continue;
                                    
                                    fireFoundSink(sinkList);

                                    it = sourceListSet.iterator();
                                    while (it.hasNext()) {
                                        sourceList = it.next();
                                        cloneSet.clear();
                                        cloneSet.addAll(sourceList);
                                        
                                        cloneSet.retainAll(sinkList);
                                        if (cloneSet.size() > 0) {
                                            fireFoundSourceSink(sourceList, sinkList);
                                        }
                                    }
                                }
                            }
                        }
                        
                        ProgressSupport.INSTANCE.fireUpdate(this, i);
                    }
                    
                    ProgressSupport.INSTANCE.fireFinished(this);

                    return null;
                }
            };

            ProgressSupport.INSTANCE.fireStart(c, 1, nodes.length-1, "Finding Sinks");
            try {
                c.call();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } 
        catch (Exception e) {
            // TODO: handle exception
        }
    }
}
