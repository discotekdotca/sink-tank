package ca.discotek.sinktank.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class IOUtil {
    
    public static class DirectoryTypeFilter implements FileFilter {
        
        static DirectoryTypeFilter instance = new DirectoryTypeFilter();
        
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    public static class FileTypeFilter implements FileFilter {
        
        static FileTypeFilter instance = new FileTypeFilter();
        
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }
    
    public static byte[] copy(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte bytes[] = copy(is);
        is.close();
        return bytes;
    }
    
    public static void deleteDirectory(File directory) {
        deleteDirectory(directory, true);
    }
    
    public static void deleteDirectory(File directory, boolean recursive) {
        if (recursive) {
            File files[] = directory.listFiles();
            if (files != null) {
                for (int i=0; i<files.length; i++) {
                    if (files[i].isDirectory()) deleteDirectory(files[i], true);
                }
                
                for (int i=0; i<files.length; i++) {
                    if (!files[i].isDirectory()) {
                        files[i].deleteOnExit();
                        files[i].delete();
                    }
                }
            }
        }
        
        directory.deleteOnExit();
        directory.delete();
    }
    
    public static File[] listFilesRecursively(String directory, boolean includeDirectories) {
        return listFilesRecursively(new File(directory), includeDirectories);
    }
    
    public static File[] listFilesRecursively(File directory, boolean includeDirectories) {
        List<File> list = new ArrayList<File>();
        listFilesRecursively(directory, includeDirectories, list);
        return list.toArray(new File[list.size()]);
    }
    
    private static void listFilesRecursively(File directory, boolean includeDirectories, List<File> list) {
        File files[] = directory.listFiles();
        if (files != null) {
            for (int i=0; i<files.length; i++) {
                if (files[i].isFile()) list.add(files[i]);
             }
             
             for (int i=0; i<files.length; i++) {
                 if (includeDirectories) list.add(files[i]);
                 if (files[i].isDirectory()) listFilesRecursively(files[i], includeDirectories, list);
             }
        }
    }
    
    public static void copy(File input, File output) throws IOException {
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);
        copy(fis, fos);
        fos.close();
        fis.close();
    }
    
    public static void copy(File input, OutputStream os) throws IOException {
        FileInputStream fis = new FileInputStream(input);
        copy(fis, os);
        fis.close();
    }
    
    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte bytes[] = new byte[1024];
        int length;
        
        while ( (length = is.read(bytes)) > 0) os.write(bytes, 0, length);
    }
    
    public static byte[] copy(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte bytes[] = new byte[1024];
        int length;
        
        while ( (length = is.read(bytes)) > 0) bos.write(bytes, 0, length);
        
        return bos.toByteArray();
    }
    
    static void copyAll(File fromDirectory, File toDirectory, Pattern fileFilter) throws IOException {
        File files[] = fromDirectory.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                File newDirectory = new File(toDirectory, files[i].getName()); 
                newDirectory.mkdirs();
                copyAll(files[i], newDirectory, fileFilter);
            }
            else {
                String name = files[i].getAbsolutePath();
                if (fileFilter.matcher(name).matches()) copy(files[i], new File(toDirectory, files[i].getName()));
            }
        }
    }
    
    public static void copyToFile(byte bytes[], String filename) throws IOException {
        copyToFile(bytes, new File(filename));
    }
    
    public static void copyToFile(byte bytes[], File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
    }
    
    public static byte[] readFile(String filename) throws IOException {
        return readFile(new File(filename));
    }
    
    public static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte bytes[] = IOUtil.copy(fis);
        fis.close();
        return bytes;
    }

    public static void copy(File from, File to, boolean overwrite) throws IOException {
        if (from == null) throw new IOException("from parameter cannot be null.");
        else if (!from.exists()) throw new IOException("from parameter must exist.");
        else if (from.isDirectory()) throw new IOException("from parameter cannot represent a directory.");
        
        if (to == null) throw new IOException("to parameter cannot be null.");
        else if (to.isDirectory()) throw new IOException("to parameter cannot represent a directory.");
        else if (to.exists()) {
            boolean deleted = to.delete();
            if (!deleted) {
                throw new IOException("Could not overwrite existing file: " + to.getAbsolutePath());               
            }
        }
        else {
            FileInputStream fis = new FileInputStream(from);
            FileOutputStream fos = new FileOutputStream(to);
            transfer(fis, fos);

            fos.flush();
            fis.close();
            fos.close();
            
        }
    }
    
    public synchronized static void transfer(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024 * 1000];
        int length;
        
        while ( (length = is.read(buffer)) > -1) {
            os.write(buffer, 0, length);
        }
    }
    
    public static void delete(File directory, boolean recursive) throws IOException {
        if (recursive) {
            File files[] = directory.listFiles();
            for (int i=0; i<files.length; i++) {
                if (files[i].isDirectory()) delete(files[i], recursive);
                else {
                    boolean success = files[i].delete();
                    if (!success) 
                        throw new IOException("Could not delete file " + files[i].getAbsolutePath());
                }
            }
        }

        boolean success = directory.delete();
        if (!success) 
            throw new IOException("Could not delete directory " + directory.getAbsolutePath());
    }
    
    public static boolean delete(File directory, boolean recursive, boolean exceptionOnfail) throws IOException {
        boolean success = true;
        boolean singleSuccess;
        if (recursive) {
            File files[] = directory.listFiles();
            if (files != null) {
                for (int i=0; i<files.length; i++) {
                    if (files[i].isDirectory()) delete(files[i], recursive);
                    else {
                        singleSuccess = files[i].delete();
                        if (!singleSuccess) {
                            if (exceptionOnfail)
                                throw new IOException("Could not delete file " + files[i].getAbsolutePath());
                            else
                                success = false;
                        }
                    }
                }
            }
        }

        singleSuccess = directory.delete();
        if (!success) { 
            if (exceptionOnfail)
                throw new IOException("Could not delete directory " + directory.getAbsolutePath());
            else
                success = false;
        }
        
        return success;
    }
    
    public static File getJarInSameDirectory(String jarName, Class clazz) {
        ProtectionDomain pd = clazz.getProtectionDomain();
        CodeSource source = pd.getCodeSource();
        File file = new File(source.getLocation().getFile());
        return new File(file.getParentFile(), jarName);
    }
    
    public static File getJarForClass(Class clazz) throws UnsupportedEncodingException {
        ProtectionDomain pd = clazz.getProtectionDomain();
        CodeSource source = pd.getCodeSource();
        return new File( URLDecoder.decode( source.getLocation().getFile(), "UTF-8") );
    }
}
