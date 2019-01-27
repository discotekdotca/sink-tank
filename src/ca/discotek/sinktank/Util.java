package ca.discotek.sinktank;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.discotek.rebundled.org.objectweb.asm.Opcodes;
import ca.discotek.rebundled.org.objectweb.asm.Type;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class Util {
    
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    
    public static File[] listFiles(File directory) {
        return listFiles(directory, null);
    }
    
    public static File[] listFiles(File directory, Pattern pattern) {
        List<File> list = new ArrayList<File>();
        listFiles(directory, pattern, list);
        return list.toArray(new File[list.size()]);
    }
    
    public static void listFiles(File directory, Pattern pattern, List<File> list) {
        File files[] = directory.listFiles();
        List<File> directoryList = new ArrayList<File>();
        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory())
                directoryList.add(files[i]);
            else if (files[i].isFile()) {
                if (pattern == null || pattern.matcher(files[i].getName()).matches())
                    list.add(files[i]);
            }
        }
        
        Iterator<File> it = directoryList.listIterator();
        while (it.hasNext())
            listFiles(it.next(), pattern, list);
    }
    
    public static String toSlashName(String className) {
        return className.replace('.', '/');
    }
    
    public static String[] descToStrings(String desc) {
        Type type = Type.getMethodType(desc);
        
        Type argTypes[] = type.getArgumentTypes();
        String types[] = 
            new String[argTypes.length];
        for (int i=0; i<types.length; i++)
            types[i] = argTypes[i].getClassName();
        
        return types;
    }
    
    public static InputStream getInputStream(ZipInputStream zis, String name) throws IOException {
        ZipEntry entry;
        while ( (entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().equals(name))
                return zis;
        }
        
        return null;
    }
    
    public static void write(InputStream is, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file); ) {
            byte buffer[] = new byte[1024 * 10];
            int length;
            while ( (length = is.read(buffer)) > 0)
                fos.write(buffer, 0, length);
        }
    }
    
    public static void write(byte bytes[], File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file); ) {
            fos.write(bytes);
        }
    }
    
    public static byte[] read(InputStream is) throws IOException {
        int length;
        byte bytes[] = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        while ( (length = is.read(bytes)) > 0)
            bos.write(bytes, 0, length);
        
        return bos.toByteArray();
    }
    
    public static byte[] read(File file) throws IOException {
        FileInputStream fos = new FileInputStream(file);
        try {
            return Util.read(fos);
        } 
        catch (IOException e) {
            throw e;        
        }
        finally {
            try { fos.close(); }
            catch (Exception e) { 
                /* nothing to do */
                e.printStackTrace();
            }
        }
    }
    
    public static byte[] read(ZipEntry entry, JarFile jar) throws IOException {
        InputStream is = jar.getInputStream(entry);
        try { return Util.read(is); } 
        catch (IOException e) {
            throw e;
        }
        finally {
            try { is.close(); } 
            catch (Exception e2) {
                // nothing to do
                e2.printStackTrace();
            }
        }
    }
    
    public static boolean isInterface(int access) {
        return isEnabled(access, Opcodes.ACC_INTERFACE);
    }
    
    public static boolean isEnabled(int access, int flag) {
        return (access & flag) != 0;
    }
    
    public static class FileSystemFileFilter implements FileFilter {

        public static final int DIRECTORIES_ONLY = 0;
        public static final int FILES_ONLY = 1;
        public static final int CLASS_FILES_ONLY = 2;
        
        final int type;
        
        public FileSystemFileFilter(int type) {
            this.type = type;
        }
        
        public boolean accept(File pathname) {
            switch (type) {
                case DIRECTORIES_ONLY: 
                    return pathname.isDirectory();
                case FILES_ONLY: 
                    return pathname.isFile();
                case CLASS_FILES_ONLY: 
                    return pathname.isFile() && pathname.getName().endsWith(".class");
                default: return false;
                
            }
        }
        
    }
    
    public static final FileSystemFileFilter DIRECTORY_FILTER = new FileSystemFileFilter(FileSystemFileFilter.DIRECTORIES_ONLY);
    public static final FileSystemFileFilter FILE_FILTER = new FileSystemFileFilter(FileSystemFileFilter.FILES_ONLY);
    public static final FileSystemFileFilter CLASS_FILE_FILTER = new FileSystemFileFilter(FileSystemFileFilter.CLASS_FILES_ONLY);

    public static String escapeHtml(String text) {
        return text.
                replace("&", "&amp;").
                replace("<", "&lt;").
                replace(">", "&gt;").
                replace("\"", "&quot;");
    }
}
