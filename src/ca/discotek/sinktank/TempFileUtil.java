package ca.discotek.sinktank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class TempFileUtil {
    static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    static final File DIRECTORY = new File(TMP_DIR, "DeepDive");
    
    static {
        DIRECTORY.mkdirs();
        File files[] = DIRECTORY.listFiles();
        if (files != null) {
            for (int i=0; i<files.length; i++)
                files[i].delete();
        }
    }
    
    public static File createTemporaryFile(byte bytes[], String name) throws IOException {
        return createTemporaryFile(new ByteArrayInputStream(bytes), name);
    }
    
    public static File createTemporaryFile(InputStream is, String name) throws IOException {
        File file = new File(DIRECTORY, name);
        file.deleteOnExit();
        
        byte bytes[] = new byte[1024];
        int length;
        
        
        FileOutputStream fos = new FileOutputStream(file);
        
        while ( (length = is.read(bytes)) > 0)
            fos.write(bytes, 0, length);
        
        fos.close();
        
        return file;
    }
}
