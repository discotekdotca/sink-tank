package ca.discotek.sinktank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
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

public class ResourceHelper extends ClassLoader {

    List<String> list = new ArrayList<String>();
    
    public void addUrl(String url) {
        list.add(url);
    }

    InputStream getResourceAsStream(InputStream is, int index, String chunks[]) throws IOException {
        if (chunks.length == 0)
            return is;
        else {
            ZipEntry entry;
            ZipInputStream zis = new ZipInputStream(is);
            for (int i=index; i<chunks.length; i++) {
                if (i<chunks.length-1) {
                    while ( (entry = zis.getNextEntry()) != null) {
                        if (entry.getName().equals(chunks[i])) {
                            zis = new ZipInputStream(zis);
                            break;
                        }
                    }
                }
                else {
                    while ( (entry = zis.getNextEntry()) != null) {
                        if (entry.getName().equals(chunks[i])) {
                            return zis;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public InputStream getResourceAsStream(String name) {
        try {
            Iterator<String> it = list.listIterator();
            String parentUrl;
            InputStream is;
            String chunks[];
            int index;
            File parentFile;
            URL url;
            while (it.hasNext()) {
                parentUrl = it.next();
                url = new URL(parentUrl);
                parentFile = new File(url.getFile());
                if (parentFile.isDirectory()) {
                    File file = new File(parentFile, name);
                    if (file.exists())
                        return new FileInputStream(file);
                }
                else /* if (parentFile.isFile()) */ {
                    chunks = (parentUrl + "!/" + name).split("!/");
                    if (chunks.length == 2) {
                        try { is = new URL("jar:" + parentUrl + "/!/" + name).openStream(); } 
                        catch (FileNotFoundException e) { continue; }
                        if (is != null)
                            return is;
                        else
                            continue;
                    }
                    else {
                        index = chunks.length - 1;
                        is = new URL("jar:" + chunks[0] + "!/" + chunks[1]).openStream();
                    }

                    is = getResourceAsStream( is, index , chunks );
                    if (is != null)
                        return is;
                }
            }
            
            return null;
        } 
        catch (Exception e) {
            return null;
        }
    }
}
