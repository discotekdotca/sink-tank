package org.benf.cfr.reader.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Functional;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.MapFactory;
import org.benf.cfr.reader.util.Predicate;
import org.benf.cfr.reader.util.SetFactory;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public class ClassFileSourceImpl
implements ClassFileSource {
    private final Set<String> explicitJars = SetFactory.newSet();
    private Map<String, String> classToPathMap;
    private Map<String, String> classCollisionRenamerLCToReal;
    private Map<String, String> classCollisionRenamerRealToLC;
    private final Options options;
    private boolean unexpectedDirectory = false;
    private String pathPrefix = "";
    private String classRemovePrefix = "";
    private static final boolean JrtPresent = ClassFileSourceImpl.CheckJrt();
    private static Map<String, String> packMap = JrtPresent ? ClassFileSourceImpl.getPackageToModuleMap() : new HashMap();

    private static boolean CheckJrt() {
        try {
            return Object.class.getResource("Object.class").getProtocol().equals("jrt");
        }
        catch (Exception e) {
            return false;
        }
    }

    public ClassFileSourceImpl(Options options) {
        this.options = options;
    }

    private byte[] getBytesFromFile(InputStream is, long length) throws IOException {
        int offset;
        if (length > Integer.MAX_VALUE) {
            // empty if block
        }
        byte[] bytes = new byte[(int)length];
        int numRead = 0;
        for (offset = 0; offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0; offset += numRead) {
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file");
        }
        is.close();
        return bytes;
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        if (this.classCollisionRenamerRealToLC == null) {
            return path;
        }
        String res = this.classCollisionRenamerRealToLC.get(path + ".class");
        if (res == null) {
            return path;
        }
        return res.substring(0, res.length() - 6);
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String inputPath) throws IOException {
        String actualName;
        Map<String, String> classPathFiles = this.getClassPathClasses();
        String jarName = classPathFiles.get(inputPath);
        String path = inputPath;
        if (this.classCollisionRenamerLCToReal != null && (actualName = this.classCollisionRenamerLCToReal.get(path)) != null) {
            path = actualName;
        }
        ZipFile zipFile = null;
        try {
            byte[] content;
            File file;
            ZipEntry zipEntry;
            boolean forceJar;
            InputStream is = null;
            long length = 0L;
            String usePath = path;
            if (this.unexpectedDirectory) {
                if (usePath.startsWith(this.classRemovePrefix)) {
                    usePath = usePath.substring(this.classRemovePrefix.length());
                }
                usePath = this.pathPrefix + usePath;
            }
            File file2 = file = (forceJar = this.explicitJars.contains(jarName)) ? null : new File(usePath);
            if (file != null && file.exists()) {
                is = new FileInputStream(file);
                length = file.length();
                content = this.getBytesFromFile(is, length);
            } else if (jarName != null) {
                zipFile = new ZipFile(new File(jarName), 1);
                zipEntry = zipFile.getEntry(path);
                length = zipEntry.getSize();
                is = zipFile.getInputStream((ZipEntry)zipEntry);
                content = this.getBytesFromFile(is, length);
            } else {
                content = this.getInternalContent(inputPath);
            }
//            zipEntry = Pair.make(content, inputPath);
//            return zipEntry;
            
            return Pair.make(content, inputPath);
        }
        finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private static Map<String, String> getPackageToModuleMap() {
        Map<String, String> mapRes = MapFactory.newMap();
        try {
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");
            Method bootMethod = moduleLayerClass.getMethod("boot", new Class[0]);
            Object boot = bootMethod.invoke(null, new Object[0]);
            Method modulesMeth = boot.getClass().getMethod("modules", new Class[0]);
            Object modules = modulesMeth.invoke(boot, new Object[0]);
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Method getPackagesMethod = moduleClass.getMethod("getPackages", new Class[0]);
            Method getNameMethod = moduleClass.getMethod("getName", new Class[0]);
            for (Object module : (Set)modules) {
                Set<String> packageNames = (Set)getPackagesMethod.invoke(module, new Object[0]);
                String moduleName = (String)getNameMethod.invoke(module, new Object[0]);
                for (String packageName : packageNames) {
                    if (mapRes.containsKey(packageName)) {
                        mapRes.put(packageName, null);
                        continue;
                    }
                    mapRes.put(packageName, moduleName);
                }
            }
        }
        catch (Exception e) {
            // empty catch block
        }
        return mapRes;
    }

    private byte[] getContentByFromReflectedClass(String inputPath) {
        try {
            byte[] res;
            Class<?> cls;
            String classPath = inputPath.replace("/", ".").substring(0, inputPath.length() - 6);
            Pair<String, String> packageAndClassNames = ClassNameUtils.getPackageAndClassNames(classPath);
            String packageName = packageAndClassNames.getFirst();
            String moduleName = packMap.get(packageName);
            if (moduleName != null && (res = this.getUrlContent(new URL("jrt:/" + moduleName + "/" + inputPath))) != null) {
                return res;
            }
            try {
                cls = Class.forName(classPath);
            }
            catch (IllegalStateException e) {
                return null;
            }
            int idx = inputPath.lastIndexOf("/");
            String name = idx < 0 ? inputPath : inputPath.substring(idx + 1);
            return this.getUrlContent(cls.getResource(name));
        }
        catch (Exception e) {
            return null;
        }
    }

    private byte[] getUrlContent(URL url) {
        InputStream is;
        String protocol = url.getProtocol();
        if (!protocol.equals("jrt")) {
            return null;
        }
        int len = -1;
        try {
            URLConnection uc = url.openConnection();
            uc.connect();
            is = uc.getInputStream();
            len = uc.getContentLength();
        }
        catch (IOException ioe) {
            return null;
        }
        try {
            if (len >= 0) {
                byte[] b = new byte[len];
                int i = len;
                while (i > 0) {
                    int n = i;
                    if (n >= (i -= is.read(b, len - i, i))) continue;
                    i = -1;
                }
                if (i == 0) {
                    return b;
                }
            }
        }
        catch (IOException e) {
            // empty catch block
        }
        return null;
    }

    private byte[] getInternalContent(String inputPath) throws IOException {
        byte[] res;
        if (JrtPresent && (res = this.getContentByFromReflectedClass(inputPath)) != null) {
            return res;
        }
        throw new IOException("No such file " + inputPath);
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        Map<String, String> thisJar;
        this.getClassPathClasses();
        File file = new File(jarPath);
        if (!file.exists()) {
            throw new ConfusedCFRException("No such jar file " + jarPath);
        }
        jarPath = file.getAbsolutePath();
        if (!this.processClassPathFile(file, jarPath, thisJar = MapFactory.newOrderedMap(), false)) {
            throw new ConfusedCFRException("Failed to load jar " + jarPath);
        }
        Set<String> dedup = null;
        if (this.classCollisionRenamerLCToReal != null) {
            final Map map = Functional.groupToMapBy(thisJar.keySet(), new UnaryFunction<String, String>(){

                @Override
                public String invoke(String arg) {
                    return arg.toLowerCase();
                }
            });
            dedup = SetFactory.newSet(Functional.filter(map.keySet(), new Predicate<String>(){

                @Override
                public boolean test(String in) {
                    return ((List)map.get(in)).size() > 1;
                }
            }));
        }
        List<String> output = ListFactory.newList();
        for (Map.Entry<String, String> entry : thisJar.entrySet()) {
            String classPath = entry.getKey();
            if (!classPath.toLowerCase().endsWith(".class")) continue;
            if (this.classCollisionRenamerLCToReal != null) {
                String renamed = ClassFileSourceImpl.addDedupName(classPath, dedup, this.classCollisionRenamerLCToReal);
                this.classCollisionRenamerRealToLC.put(classPath, renamed);
                classPath = renamed;
            }
            this.classToPathMap.put(classPath, entry.getValue());
            output.add(classPath);
        }
        this.explicitJars.add(jarPath);
        return output;
    }

    private static String addDedupName(String potDup, Set<String> collisions, Map<String, String> data) {
        String n = potDup.toLowerCase();
        String name = n.substring(0, n.length() - 6);
        int next = 0;
        if (!collisions.contains(n)) {
            return potDup;
        }
        String testName = name + "_" + next + ".class";
        while (data.containsKey(testName)) {
            testName = name + "_" + ++next + ".class";
        }
        data.put(testName, potDup);
        return testName;
    }

    private Map<String, String> getClassPathClasses() {
        if (this.classToPathMap == null) {
            String[] classPaths;
            String extraClassPath;
            boolean renameCase = (Boolean)this.options.getOption(OptionsImpl.CASE_INSENSITIVE_FS_RENAME);
            boolean dump = (Boolean)this.options.getOption(OptionsImpl.DUMP_CLASS_PATH);
            this.classToPathMap = MapFactory.newMap();
            if (true)
                return classToPathMap;
            String classPath = System.getProperty("java.class.path") + File.pathSeparatorChar + System.getProperty("sun.boot.class.path");
            if (dump) {
                System.out.println("/* ClassPath Diagnostic - searching :" + classPath);
            }
            if (null != (extraClassPath = (String)this.options.getOption(OptionsImpl.EXTRA_CLASS_PATH))) {
                classPath = classPath + File.pathSeparatorChar + extraClassPath;
            }
            if (renameCase) {
                this.classCollisionRenamerLCToReal = MapFactory.newMap();
                this.classCollisionRenamerRealToLC = MapFactory.newMap();
            }
            for (String path : classPaths = classPath.split("" + File.pathSeparatorChar)) {
                File f;
                if (dump) {
                    System.out.println(" " + path);
                }
                if ((f = new File(path)).exists()) {
                    if (f.isDirectory()) {
                        File[] files;
                        if (dump) {
                            System.out.println(" (Directory)");
                        }
                        if ((files = f.listFiles()) == null) continue;
                        for (File file : files) {
                            this.processClassPathFile(file, file.getAbsolutePath(), this.classToPathMap, dump);
                        }
                        continue;
                    }
                    this.processClassPathFile(f, path, this.classToPathMap, dump);
                    continue;
                }
                if (!dump) continue;
                System.out.println(" (Can't access)");
            }
            if (dump) {
                System.out.println(" */");
            }
        }
        return this.classToPathMap;
    }

    private boolean processClassPathFile(File file, String path, Map<String, String> classToPathMap, boolean dump) {
        try {
            ZipFile zipFile = new ZipFile(file, 1);
            try {
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        if (dump) {
                            System.out.println("  " + name);
                        }
                        classToPathMap.put(name, path);
                        continue;
                    }
                    if (!dump) continue;
                    System.out.println("  [ignoring] " + name);
                }
            }
            finally {
                zipFile.close();
            }
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String specPath) {
        new Configurator().configureWith(usePath, specPath);
    }

    public String adjustInputPath(String inputPath) {
        if (!this.unexpectedDirectory) {
            return inputPath;
        }
        if (inputPath.startsWith(this.pathPrefix)) {
            inputPath = inputPath.substring(this.pathPrefix.length());
        }
        return inputPath;
    }

    public void clearConfiguration() {
        this.unexpectedDirectory = false;
        this.pathPrefix = null;
    }

    private class Configurator {
        private Configurator() {
        }

        private void reverse(String[] in) {
            List<String> l = Arrays.asList(in);
            Collections.reverse(l);
            l.toArray(in);
        }

        private void getCommonRoot(String filePath, String classPath) {
            int diffpt;
            String npath = filePath.replace('\\', '/');
            String[] fileParts = npath.split("/");
            String[] classParts = classPath.split("/");
            this.reverse(fileParts);
            this.reverse(classParts);
            int min = Math.min(fileParts.length, classParts.length);
            for (diffpt = 0; diffpt < min && fileParts[diffpt].equals(classParts[diffpt]); ++diffpt) {
            }
            fileParts = Arrays.copyOfRange(fileParts, diffpt, fileParts.length);
            classParts = Arrays.copyOfRange(classParts, diffpt, classParts.length);
            this.reverse(fileParts);
            this.reverse(classParts);
            ClassFileSourceImpl.this.pathPrefix = fileParts.length == 0 ? "" : StringUtils.join(fileParts, "/") + "/";
            ClassFileSourceImpl.this.classRemovePrefix = classParts.length == 0 ? "" : StringUtils.join(classParts, "/") + "/";
        }

        public void configureWith(String usePath, String specPath) {
            String actualPath = specPath;
            String path = usePath;
            if (!actualPath.equals(path)) {
                ClassFileSourceImpl.this.unexpectedDirectory = true;
                if (path.endsWith(actualPath)) {
                    ClassFileSourceImpl.this.pathPrefix = path.substring(0, path.length() - actualPath.length());
                } else {
                    this.getCommonRoot(path, actualPath);
                }
            }
        }
    }

}
