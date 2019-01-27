package ca.discotek.sinktank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ExplodedWar {

    File webInfClassesDir;
    File rootDir;
    List<File> webInfLibJarList = new ArrayList<File>();
    List<File> otherJarList = new ArrayList<File>();
    
    public ExplodedWar() {
        
    }
    
    public File getWebInfClassesDir() {
        return webInfClassesDir;
    }
    
    public File getRootDir() {
        return rootDir;
    }
    
    public void addWebInfLibJar(File webInfLibJar) {
        webInfLibJarList.add(webInfLibJar);
    }
    
    public File[] getWebInfLibJars() {
        return webInfLibJarList.toArray(new File[webInfLibJarList.size()]);
    }
    
    public File[] getOtherLibJars() {
        return otherJarList.toArray(new File[otherJarList.size()]);
    }
    
    public static ExplodedWar explodeWar(byte warBytes[], File explodeDir) throws IOException {
        return explodeWar(warBytes, explodeDir, true);
    }
    
    public static ExplodedWar explodeWar(byte warBytes[], File explodeDir, boolean processOnlyJeeStructure) throws IOException {
        ExplodedWar explodedWar = new ExplodedWar();
        explodedWar.rootDir = explodeDir;
        explodedWar.webInfClassesDir = new File(explodeDir, "WEB-INF/classes");
        
        ByteArrayInputStream bis = new ByteArrayInputStream(warBytes);
        ZipInputStream zis = new ZipInputStream(bis);
        
        ZipEntry entry;
        String name;
        while ( (entry = zis.getNextEntry()) != null) {
            name = entry.getName();
            if (name.endsWith(".class")) {
                if (processOnlyJeeStructure && !name.startsWith("WEB-INF/classes"))
                    continue;
                else {
                    
                    File file = new File(explodeDir, name);
                    file.deleteOnExit();
                    file.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(Util.read(zis));
                    fos.close();
                }
            }
            else if (name.endsWith(".jar")) {
                if (processOnlyJeeStructure && !name.startsWith("WEB-INF/lib"))
                    continue;
                else {
                    File file = new File(explodeDir, name);
                    file.deleteOnExit();
                    file.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(Util.read(zis));
                    fos.close();
                    explodedWar.addWebInfLibJar(file);
                }
            }
        }
        
        return explodedWar;
    }
    
    public static ExplodedWar explodeWar(File war, File explodeDir) throws IOException {
        return explodeWar(war, explodeDir, true);
    }
    
    public static ExplodedWar explodeWar(File war, File explodeDir, boolean processOnlyJeeStructure) throws IOException {
        FileInputStream fis = new FileInputStream(war);
        byte bytes[] = Util.read(fis);
        return explodeWar(bytes, explodeDir, processOnlyJeeStructure);
    }
}
