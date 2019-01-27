package ca.discotek.sinktank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ExplodedEar {

    List<File> earLibFileList = new ArrayList<File>();
    List<File> ejbJarFileList = new ArrayList<File>();
    List<ExplodedWar> explodedWarList = new ArrayList<ExplodedWar>();
    
    public ExplodedEar() {
        
    }
    
    public void addEarLibFile(File ejbJarFile) {
        earLibFileList.add(ejbJarFile);
    }
    
    public File[] getEarLibFiles() {
        return earLibFileList.toArray(new File[earLibFileList.size()]);
    }
    
    public void addEjbJarFile(File ejbJarFile) {
        ejbJarFileList.add(ejbJarFile);
    }
    
    public File[] getEjbJarFiles() {
        return ejbJarFileList.toArray(new File[ejbJarFileList.size()]);
    }
    
    public void addExplodedWar(ExplodedWar war) {
        explodedWarList.add(war);
    }
    
    public ExplodedWar[] getExplodedWars() {
        return explodedWarList.toArray(new ExplodedWar[explodedWarList.size()]);
    }
    
    
    public static ExplodedEar explodeEar(File ear, File explodeDir) throws IOException, ParserConfigurationException, SAXException {
        ZipFile zip = new ZipFile(ear);
        
        String ejbJarNames[] = new String[0];
        String warNames[] = new String[0];
        ZipEntry entry = zip.getEntry("META-INF/application.xml");
        if (entry != null) {
            ApplicationXml xml = new ApplicationXml();
            xml.parse(zip.getInputStream(entry));

            ejbJarNames = xml.getEjbJars();
            warNames = xml.getWars();
        }
        
        ExplodedEar explodedEar = new ExplodedEar();
        
        for (int i=0; i<ejbJarNames.length; i++){
            entry = zip.getEntry(ejbJarNames[i]);
            if (entry == null)
                System.out.println("EJB Module " + entry.getName() + " is defined in META-INF/application.xml, but does not exist in ear.");
            else {
                File file = new File(explodeDir, entry.getName());
                file.deleteOnExit();
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                InputStream is = zip.getInputStream(entry);
                fos.write(Util.read(is));
                fos.close();
                explodedEar.addEjbJarFile(file);
            }
        }
        
        for (int i=0; i<warNames.length; i++){
            entry = zip.getEntry(warNames[i]);
            if (entry == null)
                System.out.println("War Module " + entry.getName() + " is defined in META-INF/application.xml, but does not exist in ear.");
            else {
                File file = new File(explodeDir, entry.getName());
                file.deleteOnExit();
                file.mkdirs();
                InputStream is = zip.getInputStream(entry);
                byte bytes[] = Util.read(is);
                
                ExplodedWar explodedWar = ExplodedWar.explodeWar(bytes, file);
                explodedEar.addExplodedWar(explodedWar);
            }
        }
        
        FileInputStream fis = new FileInputStream(ear);
        ZipInputStream zis = new ZipInputStream(fis);
        
        String name;
        while ( (entry = zis.getNextEntry()) != null) {
            name = entry.getName();
            if (name.startsWith("lib") && name.endsWith(".jar")) {
                File file = new File(explodeDir, name);
                file.deleteOnExit();
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                byte bytes[] = Util.read(zis);
                fos.write(bytes);
                fos.close();
                explodedEar.addEarLibFile(file);
            }
        }
        
        return explodedEar;
    }
}
