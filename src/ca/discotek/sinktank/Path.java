package ca.discotek.sinktank;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class Path {

    public final Method methods[];
    
    Map<Method, Integer> indexOfCacheMap = new WeakHashMap<Method, Integer>();
    
    public Path(Method methods[]) {
        this.methods = methods;
    }

    public boolean equals(Object o) {
        if (o instanceof Path) {
            Path path = (Path) o;
            if (methods.length == path.methods.length) {
                for (int i=0; i<methods.length; i++) {
                    if (!methods[i].equals(path.methods[i]))
                        return false;
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    public int indexOf(Method m) {
        Integer result = indexOfCacheMap.get(m);
        if (result == null) {
            for (int i=0; i<methods.length; i++) {
                if (m.equals(methods[i])) {
                    result = i;
                    break;
                }
            }
            
            if (result == null)
                result = -1;
            
            indexOfCacheMap.put(m, result);
        }
        
        return result;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        for (int i=0; i<methods.length; i++) {
            buffer.append(methods[i].toString());
            if (i<methods.length-1)
                buffer.append(", ");
        }
        
        return buffer.toString();
    }
    
    public String toStringReversed() {
        StringBuilder buffer = new StringBuilder();
        
        for (int i=methods.length-1; i>=0; i--) {
            buffer.append(methods[i].toString());
            if (i>0)
                buffer.append(", ");
        }
        
        return buffer.toString();
    }
}
